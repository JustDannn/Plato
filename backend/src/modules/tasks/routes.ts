import { z } from "zod";
import { requireRole, type AuthUser } from "../../core/auth";
import { sql } from "../../core/db";
import { badRequest, conflict, dateString, forbidden, notFound, type Router } from "../../core/http";
import { isActiveMember, requireClassManager } from "../classes/routes";
import { notifyClassMembers, notifyClassAdmin, notifyUser } from "../monitoring/routes";

const milestoneSchema = z.object({
  title: z.string().trim().min(1, "Judul milestone wajib diisi."),
  description: z.string().trim().default(""),
  startDate: dateString,
  deadline: dateString,
});

const taskFields = {
  title: z.string().trim().min(1, "Judul tugas wajib diisi."),
  description: z.string().trim().default(""),
  taskType: z.enum(["INDIVIDUAL", "GROUP", "PROJECT"]),
  startDate: dateString,
  deadline: dateString,
};

const createSchema = z.object({
  ...taskFields,
  classId: z.number().int(),
  milestones: z.array(milestoneSchema).default([]),
});
const updateSchema = z.object(taskFields);
const progressSchema = z.object({
  progress: z.number().int().min(0, "Progres harus 0 sampai 100.").max(100, "Progres harus 0 sampai 100."),
});

function assertDates(item: { startDate: string; deadline: string }) {
  if (item.deadline < item.startDate) throw badRequest("Tenggat tidak boleh lebih awal dari tanggal mulai.");
}

function assertMilestoneInRange(
  m: { title: string; startDate: string; deadline: string },
  task: { startDate: string; deadline: string },
) {
  assertDates(m);
  if (m.startDate < task.startDate || m.deadline > task.deadline) {
    throw badRequest(`Milestone "${m.title}" harus berada dalam rentang tanggal tugas.`);
  }
}

/**
 * Progres untuk mahasiswa adalah progres miliknya; untuk Admin/SuperAdmin adalah rata-rata
 * seluruh anggota aktif. Tugas bermilestone selalu dihitung dari milestone-nya.
 */
async function loadTasks(user: AuthUser, where: { taskId?: number; classId?: number | null; status?: string | null; from?: string | null; to?: string | null; q?: string | null }) {
  const viewer = user.role === "STUDENT" ? user.id : null;
  const scope = user.role === "SUPERADMIN" && user.adminLevel !== "GLOBAL" ? user.cohortId : null;
  const adminClass = user.role === "ADMIN" ? user.assignedClassId ?? -1 : null;
  const q = where.q ? `%${where.q}%` : null;

  const tasks = await sql`
    SELECT t.id AS "taskId", t.class_id AS "classId", c.class_name AS "className", c.course_name AS "courseName",
           t.created_by AS "createdBy", cu.name AS "createdByName", t.title, t.description, t.task_type AS "taskType",
           to_char(t.start_date, 'YYYY-MM-DD') AS "startDate", to_char(t.deadline, 'YYYY-MM-DD') AS "deadline",
           t.status, t.rejection_reason AS "rejectionReason", t.created_at AS "createdAt"
    FROM tasks t
    JOIN classrooms c ON c.id = t.class_id
    JOIN users cu ON cu.id = t.created_by
    WHERE (${where.taskId ?? null}::int IS NULL OR t.id = ${where.taskId ?? null})
      AND (${where.classId ?? null}::int IS NULL OR t.class_id = ${where.classId ?? null})
      AND (${where.status ?? null}::text IS NULL OR t.status = ${where.status ?? null})
      AND (${where.from ?? null}::date IS NULL OR t.deadline >= ${where.from ?? null}::date)
      AND (${where.to ?? null}::date IS NULL OR t.start_date <= ${where.to ?? null}::date)
      AND (${q}::text IS NULL OR t.title ILIKE ${q} OR c.course_name ILIKE ${q})
      AND (${adminClass}::int IS NULL OR t.class_id = ${adminClass})
      AND (${scope}::int IS NULL OR c.cohort_id = ${scope})
      AND (${viewer}::int IS NULL
           OR t.created_by = ${viewer}
           OR (t.status = 'APPROVED' AND EXISTS (
                 SELECT 1 FROM memberships m
                 WHERE m.class_id = t.class_id AND m.student_id = ${viewer} AND m.status = 'ACTIVE')))
    ORDER BY t.deadline, t.id`;
  if (tasks.length === 0) return [];

  const taskIds: number[] = tasks.map((t: { taskId: number }) => t.taskId);
  const milestones = await sql`
    SELECT ms.id AS "milestoneId", ms.task_id AS "taskId", ms.title, ms.description,
           to_char(ms.start_date, 'YYYY-MM-DD') AS "startDate", to_char(ms.deadline, 'YYYY-MM-DD') AS "deadline",
           COALESCE(
             CASE WHEN ${viewer}::int IS NOT NULL
               THEN (SELECT mp.progress FROM milestone_progress mp WHERE mp.milestone_id = ms.id AND mp.student_id = ${viewer})
               ELSE (SELECT round(COALESCE(sum(mp.progress), 0)::numeric / NULLIF(count(m.id), 0))::int
                     FROM tasks t JOIN memberships m ON m.class_id = t.class_id AND m.status = 'ACTIVE'
                     LEFT JOIN milestone_progress mp ON mp.milestone_id = ms.id AND mp.student_id = m.student_id
                     WHERE t.id = ms.task_id)
             END, 0) AS progress
    FROM milestones ms
    WHERE ms.task_id IN ${sql(taskIds)}
    ORDER BY ms.start_date, ms.deadline, ms.id`;

  const manual = await sql`
    SELECT t.id AS "taskId", COALESCE(
      CASE WHEN ${viewer}::int IS NOT NULL
        THEN (SELECT tp.progress FROM task_progress tp WHERE tp.task_id = t.id AND tp.student_id = ${viewer})
        ELSE (SELECT round(COALESCE(sum(tp.progress), 0)::numeric / NULLIF(count(m.id), 0))::int
              FROM memberships m
              LEFT JOIN task_progress tp ON tp.task_id = t.id AND tp.student_id = m.student_id
              WHERE m.class_id = t.class_id AND m.status = 'ACTIVE')
      END, 0) AS progress
    FROM tasks t WHERE t.id IN ${sql(taskIds)}`;

  const progressById = new Map<number, number>(manual.map((p: { taskId: number; progress: number }) => [p.taskId, p.progress]));
  const milestonesByTask = Map.groupBy(milestones as { taskId: number; progress: number }[], (m) => m.taskId);

  return tasks.map((t: { taskId: number }) => {
    const items = milestonesByTask.get(t.taskId) ?? [];
    const progress = items.length
      ? Math.round(items.reduce((sum, m) => sum + m.progress, 0) / items.length)
      : progressById.get(t.taskId) ?? 0;
    return { ...t, progress, milestones: items };
  });
}

async function findTask(user: AuthUser, id: number) {
  const [task] = await loadTasks(user, { taskId: id });
  if (!task) throw notFound("Tugas tidak ditemukan.");
  return task;
}

async function rawTask(id: number) {
  const [row] = await sql`
    SELECT id, class_id, created_by, title, status, to_char(start_date, 'YYYY-MM-DD') AS start_date,
           to_char(deadline, 'YYYY-MM-DD') AS deadline, task_type
    FROM tasks WHERE id = ${id}`;
  if (!row) throw notFound("Tugas tidak ditemukan.");
  return row;
}

async function milestoneTask(milestoneId: number) {
  const [row] = await sql`SELECT task_id FROM milestones WHERE id = ${milestoneId}`;
  if (!row) throw notFound("Milestone tidak ditemukan.");
  return rawTask(row.task_id);
}

async function requireStudentOnApproved(user: AuthUser, task: { class_id: number; status: string }) {
  requireRole(user, "STUDENT");
  if (task.status !== "APPROVED") throw conflict("Progres hanya dapat diperbarui pada tugas yang sudah disetujui.");
  if (!(await isActiveMember(user.id, task.class_id))) throw forbidden("Anda bukan anggota kelas ini.");
}

async function validateTask(user: AuthUser, id: number, decide: (task: Awaited<ReturnType<typeof rawTask>>) => Promise<void>) {
  requireRole(user, "ADMIN");
  const task = await rawTask(id);
  await requireClassManager(user, task.class_id);
  if (task.status !== "PENDING") throw conflict("Pengajuan ini sudah divalidasi.");
  await decide(task);
  return findTask(user, id);
}

export function registerTaskRoutes(r: Router) {
  r.get("/tasks", async (c) => {
    const classId = c.query.get("classId");
    return loadTasks(c.user, {
      classId: classId ? Number(classId) : null,
      status: c.query.get("status"),
      from: c.query.get("from"),
      to: c.query.get("to"),
      q: c.query.get("q"),
    });
  });

  r.get("/tasks/:id", (c) => findTask(c.user, c.intParam("id")));

  r.post("/tasks", async (c) => {
    const body = await c.json(createSchema);
    assertDates(body);
    if (body.milestones.length && body.taskType !== "PROJECT") throw badRequest("Milestone hanya untuk tugas proyek.");
    body.milestones.forEach((m) => assertMilestoneInRange(m, body));

    if (c.user.role === "STUDENT") {
      if (!(await isActiveMember(c.user.id, body.classId))) throw forbidden("Anda bukan anggota kelas ini.");
    } else {
      requireRole(c.user, "ADMIN");
      await requireClassManager(c.user, body.classId);
    }
    const byAdmin = c.user.role === "ADMIN";
    const status = byAdmin ? "APPROVED" : "PENDING";

    const id = await sql.begin(async (tx) => {
      const [t] = await tx`
        INSERT INTO tasks (class_id, created_by, title, description, task_type, start_date, deadline, status, validated_by, validated_at)
        VALUES (${body.classId}, ${c.user.id}, ${body.title}, ${body.description}, ${body.taskType},
                ${body.startDate}::date, ${body.deadline}::date, ${status},
                ${byAdmin ? c.user.id : null}, ${byAdmin ? new Date() : null})
        RETURNING id`;
      for (const m of body.milestones) {
        await tx`INSERT INTO milestones (task_id, title, description, start_date, deadline)
                 VALUES (${t.id}, ${m.title}, ${m.description}, ${m.startDate}::date, ${m.deadline}::date)`;
      }
      if (byAdmin) await notifyClassMembers(tx, body.classId, t.id, "TASK_PUBLISHED", `Tugas baru dipublikasikan: ${body.title}`);
      else await notifyClassAdmin(tx, body.classId, t.id, `Pengajuan tugas baru menunggu validasi: ${body.title}`);
      return t.id as number;
    });
    return findTask(c.user, id);
  });

  r.put("/tasks/:id", async (c) => {
    const id = c.intParam("id");
    const task = await rawTask(id);
    const body = await c.json(updateSchema);
    assertDates(body);

    let nextStatus = task.status;
    if (c.user.role === "ADMIN") {
      await requireClassManager(c.user, task.class_id);
    } else if (c.user.role === "STUDENT" && task.created_by === c.user.id && task.status !== "APPROVED") {
      nextStatus = "PENDING"; // Pengajuan yang diperbaiki kembali ditinjau Admin.
    } else {
      throw forbidden();
    }
    const [outside] = await sql`
      SELECT 1 FROM milestones WHERE task_id = ${id}
        AND (start_date < ${body.startDate}::date OR deadline > ${body.deadline}::date) LIMIT 1`;
    if (outside) throw badRequest("Masih ada milestone di luar rentang tanggal yang baru.");
    const [hasMilestones] = await sql`SELECT 1 FROM milestones WHERE task_id = ${id} LIMIT 1`;
    if (hasMilestones && body.taskType !== "PROJECT") throw badRequest("Tugas yang memiliki milestone harus berjenis proyek.");

    await sql`
      UPDATE tasks SET title = ${body.title}, description = ${body.description}, task_type = ${body.taskType},
        start_date = ${body.startDate}::date, deadline = ${body.deadline}::date, status = ${nextStatus},
        rejection_reason = CASE WHEN ${nextStatus} = 'PENDING' THEN NULL ELSE rejection_reason END
      WHERE id = ${id}`;
    return findTask(c.user, id);
  });

  r.delete("/tasks/:id", async (c) => {
    const id = c.intParam("id");
    const task = await rawTask(id);
    if (c.user.role === "ADMIN") await requireClassManager(c.user, task.class_id);
    else if (!(c.user.role === "STUDENT" && task.created_by === c.user.id && task.status !== "APPROVED")) throw forbidden();
    await sql`DELETE FROM tasks WHERE id = ${id}`;
  });

  r.post("/tasks/:id/approve", (c) =>
    validateTask(c.user, c.intParam("id"), async (task) => {
      await sql.begin(async (tx) => {
        const [updated] = await tx`
          UPDATE tasks SET status = 'APPROVED', rejection_reason = NULL, validated_by = ${c.user.id}, validated_at = now()
          WHERE id = ${task.id} AND status = 'PENDING' RETURNING id`;
        if (!updated) throw conflict("Pengajuan ini sudah divalidasi.");
        await notifyClassMembers(tx, task.class_id, task.id, "TASK_PUBLISHED", `Tugas baru dipublikasikan: ${task.title}`);
        await notifyUser(tx, task.created_by, task.id, "SUBMISSION_APPROVED", `Pengajuan "${task.title}" disetujui.`);
      });
    }),
  );

  r.post("/tasks/:id/reject", async (c) => {
    const body = await c.json(z.object({ reason: z.string().trim().min(1, "Alasan penolakan wajib diisi.") }));
    return validateTask(c.user, c.intParam("id"), async (task) => {
      await sql.begin(async (tx) => {
        const [updated] = await tx`
          UPDATE tasks SET status = 'REJECTED', rejection_reason = ${body.reason}, validated_by = ${c.user.id}, validated_at = now()
          WHERE id = ${task.id} AND status = 'PENDING' RETURNING id`;
        if (!updated) throw conflict("Pengajuan ini sudah divalidasi.");
        await notifyUser(tx, task.created_by, task.id, "SUBMISSION_REJECTED", `Pengajuan "${task.title}" ditolak: ${body.reason}`);
      });
    });
  });

  r.put("/tasks/:id/progress", async (c) => {
    const id = c.intParam("id");
    const task = await rawTask(id);
    await requireStudentOnApproved(c.user, task);
    const [hasMilestones] = await sql`SELECT 1 FROM milestones WHERE task_id = ${id} LIMIT 1`;
    if (hasMilestones) throw conflict("Progres tugas ini dihitung otomatis dari milestone.");
    const { progress } = await c.json(progressSchema);
    await sql`
      INSERT INTO task_progress (task_id, student_id, progress) VALUES (${id}, ${c.user.id}, ${progress})
      ON CONFLICT (task_id, student_id) DO UPDATE SET progress = EXCLUDED.progress, updated_at = now()`;
    return findTask(c.user, id);
  });

  r.post("/tasks/:id/milestones", async (c) => {
    const id = c.intParam("id");
    const task = await rawTask(id);
    requireRole(c.user, "ADMIN");
    await requireClassManager(c.user, task.class_id);
    if (task.task_type !== "PROJECT") throw badRequest("Milestone hanya untuk tugas proyek.");
    const body = await c.json(milestoneSchema);
    assertMilestoneInRange(body, { startDate: task.start_date, deadline: task.deadline });
    await sql`INSERT INTO milestones (task_id, title, description, start_date, deadline)
              VALUES (${id}, ${body.title}, ${body.description}, ${body.startDate}::date, ${body.deadline}::date)`;
    return findTask(c.user, id);
  });

  r.put("/milestones/:id", async (c) => {
    const id = c.intParam("id");
    const task = await milestoneTask(id);
    requireRole(c.user, "ADMIN");
    await requireClassManager(c.user, task.class_id);
    const body = await c.json(milestoneSchema);
    assertMilestoneInRange(body, { startDate: task.start_date, deadline: task.deadline });
    await sql`UPDATE milestones SET title = ${body.title}, description = ${body.description},
              start_date = ${body.startDate}::date, deadline = ${body.deadline}::date WHERE id = ${id}`;
    return findTask(c.user, task.id);
  });

  r.delete("/milestones/:id", async (c) => {
    const id = c.intParam("id");
    const task = await milestoneTask(id);
    requireRole(c.user, "ADMIN");
    await requireClassManager(c.user, task.class_id);
    await sql`DELETE FROM milestones WHERE id = ${id}`;
    return findTask(c.user, task.id);
  });

  r.put("/milestones/:id/progress", async (c) => {
    const id = c.intParam("id");
    const task = await milestoneTask(id);
    await requireStudentOnApproved(c.user, task);
    const { progress } = await c.json(progressSchema);
    await sql`
      INSERT INTO milestone_progress (milestone_id, student_id, progress) VALUES (${id}, ${c.user.id}, ${progress})
      ON CONFLICT (milestone_id, student_id) DO UPDATE SET progress = EXCLUDED.progress, updated_at = now()`;
    return findTask(c.user, task.id);
  });
}
