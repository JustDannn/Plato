import type { SQL } from "bun";
import { z } from "zod";
import { requireRole } from "../../core/auth";
import { sql } from "../../core/db";
import { forbidden, notFound, type Router } from "../../core/http";

type NotificationType = "TASK_PUBLISHED" | "SUBMISSION_APPROVED" | "SUBMISSION_REJECTED" | "NEW_SUBMISSION" | "REMINDER";

export async function notifyUser(tx: SQL, userId: number, taskId: number | null, type: NotificationType, message: string) {
  await tx`INSERT INTO notifications (user_id, task_id, message, type) VALUES (${userId}, ${taskId}, ${message}, ${type})`;
}

export async function notifyClassMembers(tx: SQL, classId: number, taskId: number | null, type: NotificationType, message: string) {
  await tx`
    INSERT INTO notifications (user_id, task_id, message, type)
    SELECT m.student_id, ${taskId}::int, ${message}, ${type}
    FROM memberships m WHERE m.class_id = ${classId} AND m.status = 'ACTIVE'`;
}

export async function notifyClassAdmin(tx: SQL, classId: number, taskId: number, message: string) {
  await tx`
    INSERT INTO notifications (user_id, task_id, message, type)
    SELECT a.user_id, ${taskId}::int, ${message}, 'NEW_SUBMISSION'
    FROM admins a WHERE a.assigned_class_id = ${classId}`;
}

export function registerMonitoringRoutes(r: Router) {
  r.get("/notifications", (c) => sql`
    SELECT n.id AS "notificationId", n.user_id AS "userId", n.task_id AS "taskId", n.message, n.type,
           n.is_read AS "read", n.created_at AS "createdAt"
    FROM notifications n
    WHERE n.user_id = ${c.user.id}
    ORDER BY n.created_at DESC, n.id DESC
    LIMIT 100`);

  r.get("/notifications/unread-count", async (c) => {
    const [row] = await sql`SELECT count(*)::int AS count FROM notifications WHERE user_id = ${c.user.id} AND NOT is_read`;
    return row;
  });

  r.post("/notifications/read-all", async (c) => {
    await sql`UPDATE notifications SET is_read = TRUE WHERE user_id = ${c.user.id} AND NOT is_read`;
  });

  r.patch("/notifications/:id/read", async (c) => {
    const [row] = await sql`
      UPDATE notifications SET is_read = TRUE
      WHERE id = ${c.intParam("id")} AND user_id = ${c.user.id} RETURNING id`;
    if (!row) throw notFound("Notifikasi tidak ditemukan.");
  });

  r.post("/classes/:id/notifications", async (c) => {
    requireRole(c.user, "ADMIN");
    const classId = c.intParam("id");
    if (c.user.assignedClassId !== classId) throw forbidden("Anda bukan Admin kelas ini.");
    const body = await c.json(z.object({
      message: z.string().trim().min(1, "Pesan wajib diisi.").max(500),
      taskId: z.number().int().nullable().default(null),
    }));
    if (body.taskId !== null) {
      const [task] = await sql`SELECT 1 FROM tasks WHERE id = ${body.taskId} AND class_id = ${classId}`;
      if (!task) throw notFound("Tugas tidak ditemukan di kelas ini.");
    }
    await notifyClassMembers(sql, classId, body.taskId, "REMINDER", body.message);
  });

  /** Ringkasan angka per peran. Pengolahan kalender dan Gantt dilakukan class Dashboard di desktop. */
  r.get("/dashboard/summary", async (c) => {
    const u = c.user;
    if (u.role === "STUDENT") {
      const [row] = await sql`
        SELECT (SELECT count(*)::int FROM memberships WHERE student_id = ${u.id} AND status = 'ACTIVE') AS "classCount",
               (SELECT count(*)::int FROM tasks WHERE created_by = ${u.id} AND status = 'PENDING') AS "pendingCount"`;
      return { ...row, completionTrend: await completionTrend(u.id, null, null) };
    }
    if (u.role === "ADMIN") {
      const classId = u.assignedClassId ?? -1;
      const [row] = await sql`
        SELECT (SELECT count(*)::int FROM memberships WHERE class_id = ${classId} AND status = 'ACTIVE') AS "memberCount",
               (SELECT count(*)::int FROM tasks WHERE class_id = ${classId} AND status = 'PENDING') AS "pendingCount"`;
      return { ...row, completionTrend: await completionTrend(null, classId, null) };
    }
    const scope = u.adminLevel === "GLOBAL" ? null : u.cohortId;
    const [row] = await sql`
      SELECT (SELECT count(*)::int FROM cohorts WHERE ${scope}::int IS NULL OR id = ${scope}) AS "cohortCount",
             (SELECT count(*)::int FROM classrooms WHERE ${scope}::int IS NULL OR cohort_id = ${scope}) AS "classCount",
             (SELECT count(*)::int FROM students WHERE ${scope}::int IS NULL OR cohort_id = ${scope}) AS "studentCount",
             (SELECT count(*)::int FROM admins a JOIN classrooms c ON c.id = a.assigned_class_id
              WHERE ${scope}::int IS NULL OR c.cohort_id = ${scope}) AS "adminCount",
             (SELECT count(*)::int FROM tasks t JOIN classrooms c ON c.id = t.class_id
              WHERE t.status = 'PENDING' AND (${scope}::int IS NULL OR c.cohort_id = ${scope})) AS "pendingCount"`;
    return { ...row, completionTrend: await completionTrend(null, null, scope) };
  });
}

/** Jumlah kumulatif pekerjaan selesai pada 7 minggu terakhir. */
async function completionTrend(studentId: number | null, classId: number | null, cohortId: number | null) {
  return sql`
    WITH weeks AS (
      SELECT generate_series(date_trunc('week', now()) - interval '6 weeks', date_trunc('week', now()), interval '1 week') AS week
    ),
    done AS (
      SELECT tp.updated_at, tp.student_id, t.class_id FROM task_progress tp JOIN tasks t ON t.id = tp.task_id
      WHERE tp.progress = 100
      UNION ALL
      SELECT mp.updated_at, mp.student_id, t.class_id FROM milestone_progress mp
      JOIN milestones ms ON ms.id = mp.milestone_id JOIN tasks t ON t.id = ms.task_id
      WHERE mp.progress = 100
    )
    SELECT to_char(w.week, 'YYYY-MM-DD') AS "weekStart",
           (SELECT count(*)::int FROM done d JOIN classrooms c ON c.id = d.class_id
            WHERE d.updated_at < w.week + interval '1 week'
              AND (${studentId}::int IS NULL OR d.student_id = ${studentId})
              AND (${classId}::int IS NULL OR d.class_id = ${classId})
              AND (${cohortId}::int IS NULL OR c.cohort_id = ${cohortId})) AS total
    FROM weeks w ORDER BY w.week`;
}
