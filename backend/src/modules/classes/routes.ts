import { z } from "zod";
import { requireCohortScope, requireRole, type AuthUser } from "../../core/auth";
import { sql } from "../../core/db";
import { badRequest, conflict, forbidden, notFound, type Router } from "../../core/http";

const classSchema = z.object({
  cohortId: z.number().int(),
  className: z.string().trim().min(1, "Nama kelas wajib diisi."),
  courseName: z.string().trim().min(1, "Nama mata kuliah wajib diisi."),
  description: z.string().trim().default(""),
});

const CLASS_SELECT = sql`
  SELECT c.id AS "classId", c.cohort_id AS "cohortId", co.name AS "cohortName",
         c.class_name AS "className", c.course_name AS "courseName", c.description,
         a.user_id AS "adminId", au.name AS "adminName",
         (SELECT count(*)::int FROM memberships m WHERE m.class_id = c.id AND m.status = 'ACTIVE') AS "memberCount",
         (SELECT count(*)::int FROM tasks t WHERE t.class_id = c.id AND t.status = 'APPROVED') AS "taskCount"
  FROM classrooms c
  JOIN cohorts co ON co.id = c.cohort_id
  LEFT JOIN admins a ON a.assigned_class_id = c.id
  LEFT JOIN users au ON au.id = a.user_id`;

async function findClass(id: number) {
  const [row] = await sql`${CLASS_SELECT} WHERE c.id = ${id}`;
  if (!row) throw notFound("Kelas tidak ditemukan.");
  return row;
}

export async function isActiveMember(studentId: number, classId: number) {
  const [row] = await sql`SELECT 1 FROM memberships
    WHERE student_id = ${studentId} AND class_id = ${classId} AND status = 'ACTIVE'`;
  return !!row;
}

/** Admin kelas tersebut atau SuperAdmin yang berwenang atas angkatannya. */
export async function requireClassManager(user: AuthUser, classId: number) {
  if (user.role === "ADMIN") {
    if (user.assignedClassId !== classId) throw forbidden("Anda bukan Admin kelas ini.");
    return;
  }
  const [row] = await sql`SELECT cohort_id FROM classrooms WHERE id = ${classId}`;
  if (!row) throw notFound("Kelas tidak ditemukan.");
  requireCohortScope(user, row.cohort_id);
}

async function requireClassReader(user: AuthUser, classId: number) {
  if (user.role === "STUDENT") {
    if (!(await isActiveMember(user.id, classId))) throw forbidden("Anda bukan anggota kelas ini.");
    return;
  }
  await requireClassManager(user, classId);
}

export function registerClassRoutes(r: Router) {
  r.get("/classes", async (c) => {
    const u = c.user;
    if (u.role === "STUDENT") {
      return sql`${CLASS_SELECT}
        JOIN memberships me ON me.class_id = c.id AND me.student_id = ${u.id} AND me.status = 'ACTIVE'
        ORDER BY c.course_name`;
    }
    if (u.role === "ADMIN") {
      return sql`${CLASS_SELECT} WHERE c.id = ${u.assignedClassId} ORDER BY c.course_name`;
    }
    const scope = u.adminLevel === "GLOBAL" ? null : u.cohortId;
    return sql`${CLASS_SELECT} WHERE ${scope}::int IS NULL OR c.cohort_id = ${scope}
               ORDER BY co.year DESC, c.course_name, c.class_name`;
  });

  r.get("/classes/available", async (c) => {
    requireRole(c.user, "STUDENT");
    return sql`${CLASS_SELECT}
      WHERE c.cohort_id = ${c.user.cohortId}
        AND NOT EXISTS (SELECT 1 FROM memberships m WHERE m.class_id = c.id AND m.student_id = ${c.user.id})
      ORDER BY c.course_name`;
  });

  r.get("/classes/:id", async (c) => {
    const id = c.intParam("id");
    await requireClassReader(c.user, id);
    return findClass(id);
  });

  r.post("/classes", async (c) => {
    const body = await c.json(classSchema);
    requireCohortScope(c.user, body.cohortId);
    const [row] = await sql`
      INSERT INTO classrooms (cohort_id, class_name, course_name, description)
      VALUES (${body.cohortId}, ${body.className}, ${body.courseName}, ${body.description}) RETURNING id`;
    return findClass(row.id);
  });

  r.put("/classes/:id", async (c) => {
    const id = c.intParam("id");
    await requireClassManager(c.user, id);
    const body = await c.json(classSchema.partial({ cohortId: true }));
    if (body.cohortId !== undefined) {
      if (c.user.role !== "SUPERADMIN") {
        const current = await findClass(id);
        if (current.cohortId !== body.cohortId) throw forbidden("Hanya SuperAdmin yang dapat memindahkan angkatan kelas.");
      } else {
        requireCohortScope(c.user, body.cohortId);
      }
    }
    await sql`
      UPDATE classrooms SET class_name = ${body.className}, course_name = ${body.courseName},
        description = ${body.description}, cohort_id = COALESCE(${body.cohortId ?? null}::int, cohort_id)
      WHERE id = ${id}`;
    return findClass(id);
  });

  r.delete("/classes/:id", async (c) => {
    const id = c.intParam("id");
    requireRole(c.user, "SUPERADMIN");
    await requireClassManager(c.user, id);
    await sql`DELETE FROM classrooms WHERE id = ${id}`;
  });

  r.post("/classes/:id/join", async (c) => {
    requireRole(c.user, "STUDENT");
    const id = c.intParam("id");
    const cls = await findClass(id);
    if (cls.cohortId !== c.user.cohortId) throw forbidden("Kelas ini berada di angkatan lain.");
    const [existing] = await sql`SELECT status FROM memberships WHERE student_id = ${c.user.id} AND class_id = ${id}`;
    if (existing?.status === "ACTIVE") throw conflict("Anda sudah menjadi anggota kelas ini.");
    if (existing) throw forbidden("Keanggotaan Anda dinonaktifkan. Hubungi Admin kelas.");
    await sql`INSERT INTO memberships (student_id, class_id) VALUES (${c.user.id}, ${id})`;
    return findClass(id);
  });

  r.get("/classes/:id/members", async (c) => {
    const id = c.intParam("id");
    await requireClassReader(c.user, id);
    return sql`
      SELECT m.id AS "membershipId", m.student_id AS "userId", m.class_id AS "classId", u.name, u.email,
             s.student_id AS "studentId", m.joined_at AS "joinedAt", m.status
      FROM memberships m
      JOIN users u ON u.id = m.student_id
      JOIN students s ON s.user_id = m.student_id
      WHERE m.class_id = ${id} AND (m.status = 'ACTIVE' OR ${c.user.role} <> 'STUDENT')
      ORDER BY u.name`;
  });

  r.post("/classes/:id/members", async (c) => {
    const id = c.intParam("id");
    await requireClassManager(c.user, id);
    const body = await c.json(z.object({ email: z.string().trim().toLowerCase().email("Format email tidak valid.") }));
    const cls = await findClass(id);
    const [student] = await sql`
      SELECT u.id, s.cohort_id FROM users u JOIN students s ON s.user_id = u.id WHERE u.email = ${body.email}`;
    if (!student) throw badRequest("Mahasiswa dengan email tersebut tidak ditemukan.");
    if (student.cohort_id !== cls.cohortId) throw badRequest("Mahasiswa berada di angkatan yang berbeda dengan kelas ini.");
    await sql`
      INSERT INTO memberships (student_id, class_id) VALUES (${student.id}, ${id})
      ON CONFLICT (student_id, class_id) DO UPDATE SET status = 'ACTIVE'`;
    return findClass(id);
  });

  r.patch("/classes/:id/members/:membershipId", async (c) => {
    const id = c.intParam("id");
    await requireClassManager(c.user, id);
    const body = await c.json(z.object({ status: z.enum(["ACTIVE", "INACTIVE"]) }));
    const [row] = await sql`
      UPDATE memberships SET status = ${body.status}
      WHERE id = ${c.intParam("membershipId")} AND class_id = ${id} RETURNING id`;
    if (!row) throw notFound("Keanggotaan tidak ditemukan.");
  });
}
