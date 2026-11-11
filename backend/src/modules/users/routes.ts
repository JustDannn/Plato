import { z } from "zod";
import { requireCohortScope, requireRole, signToken, type AuthUser } from "../../core/auth";
import { sql } from "../../core/db";
import { badRequest, conflict, dateString, forbidden, HttpError, notFound, type Router } from "../../core/http";

const email = z.string().trim().toLowerCase().email("Format email tidak valid.");
const name = z.string().trim().min(1, "Nama wajib diisi.").max(120);
const password = z.string().min(8, "Password minimal 8 karakter.").max(200);

const baseUser = { name, email };
const createUserSchema = z.discriminatedUnion("role", [
  z.object({
    ...baseUser, password, role: z.literal("STUDENT"),
    studentId: z.string().trim().min(1, "NIM wajib diisi."),
    cohortId: z.number().int(),
    studyProgram: z.string().trim().min(1, "Program studi wajib diisi."),
    semester: z.number().int().min(1, "Semester tidak valid.").max(14, "Semester tidak valid."),
  }),
  z.object({
    ...baseUser, password, role: z.literal("ADMIN"),
    adminCode: z.string().trim().min(1, "Kode admin wajib diisi."),
    assignedClassId: z.number().int().nullable(),
    appointmentDate: dateString.optional(),
  }),
  z.object({
    ...baseUser, password, role: z.literal("SUPERADMIN"),
    adminLevel: z.enum(["COHORT", "GLOBAL"]),
    cohortId: z.number().int().nullable(),
  }),
]);

const updateUserSchema = z.object({
  name, email,
  isActive: z.boolean().optional(),
  password: password.optional(),
  studentId: z.string().trim().min(1).optional(),
  cohortId: z.number().int().nullable().optional(),
  studyProgram: z.string().trim().min(1).optional(),
  semester: z.number().int().min(1).max(14).optional(),
  adminCode: z.string().trim().min(1).optional(),
  assignedClassId: z.number().int().nullable().optional(),
  adminLevel: z.enum(["COHORT", "GLOBAL"]).optional(),
});

const cohortSchema = z.object({
  name: z.string().trim().min(1, "Nama angkatan wajib diisi."),
  year: z.number().int().min(2000, "Tahun tidak valid.").max(2100, "Tahun tidak valid."),
});

const USER_SELECT = sql`
  SELECT u.id AS "userId", u.name, u.email, u.role, u.is_active AS "isActive", u.created_at AS "createdAt",
         s.student_id AS "studentId", COALESCE(s.cohort_id, sa.cohort_id) AS "cohortId",
         s.study_program AS "studyProgram", s.semester,
         a.admin_code AS "adminCode", a.assigned_class_id AS "assignedClassId",
         to_char(a.appointment_date, 'YYYY-MM-DD') AS "appointmentDate",
         sa.admin_level AS "adminLevel"
  FROM users u
  LEFT JOIN students s ON s.user_id = u.id
  LEFT JOIN admins a ON a.user_id = u.id
  LEFT JOIN superadmins sa ON sa.user_id = u.id`;

async function findUser(id: number) {
  const [row] = await sql`${USER_SELECT} WHERE u.id = ${id}`;
  if (!row) throw notFound("Pengguna tidak ditemukan.");
  return row;
}

async function classCohort(classId: number): Promise<number> {
  const [row] = await sql`SELECT cohort_id FROM classrooms WHERE id = ${classId}`;
  if (!row) throw badRequest("Kelas tidak ditemukan.");
  return row.cohort_id;
}

/** Admin tidak memiliki angkatan sendiri; cakupannya mengikuti kelas yang dipegang. */
function targetCohort(row: { role: string; cohortId: number | null; assignedClassId: number | null }) {
  if (row.role === "ADMIN") return row.assignedClassId ? classCohort(row.assignedClassId) : Promise.resolve(null);
  return Promise.resolve(row.cohortId);
}

async function requireManageable(actor: AuthUser, target: Awaited<ReturnType<typeof findUser>>) {
  requireRole(actor, "SUPERADMIN");
  if (actor.adminLevel === "GLOBAL") return;
  if (target.role === "SUPERADMIN") throw forbidden("Hanya SuperAdmin GLOBAL yang dapat mengelola SuperAdmin.");
  const cohort = await targetCohort(target);
  if (cohort !== null) requireCohortScope(actor, cohort);
}

export function registerUserRoutes(r: Router) {
  r.public("POST", "/auth/login", async (c) => {
    const body = await c.json(z.object({ email, password: z.string().min(1, "Password wajib diisi.") }));
    const [row] = await sql`SELECT id, password_hash, is_active, token_version FROM users WHERE email = ${body.email}`;
    const valid = row && (await Bun.password.verify(body.password, row.password_hash));
    if (!valid) throw new HttpError(401, "Email atau password salah.");
    if (!row.is_active) throw new HttpError(403, "Akun ini telah dinonaktifkan.");
    return { token: await signToken(row.id, row.token_version), user: await findUser(row.id) };
  });

  r.post("/auth/logout", async (c) => {
    await sql`UPDATE users SET token_version = token_version + 1 WHERE id = ${c.user.id}`;
  });

  r.post("/auth/verify", async (c) => {
    const body = await c.json(z.object({ email, password: z.string() }));
    const [row] = await sql`SELECT email, password_hash FROM users WHERE id = ${c.user.id}`;
    return { valid: row.email === body.email && (await Bun.password.verify(body.password, row.password_hash)) };
  });

  r.get("/users/me", (c) => findUser(c.user.id));

  r.put("/users/me", async (c) => {
    const body = await c.json(z.object({ name, email }));
    await sql`UPDATE users SET name = ${body.name}, email = ${body.email} WHERE id = ${c.user.id}`;
    return findUser(c.user.id);
  });

  r.put("/users/me/password", async (c) => {
    const body = await c.json(z.object({ currentPassword: z.string(), newPassword: password }));
    const [row] = await sql`SELECT password_hash FROM users WHERE id = ${c.user.id}`;
    if (!(await Bun.password.verify(body.currentPassword, row.password_hash))) {
      throw badRequest("Password saat ini salah.");
    }
    const hash = await Bun.password.hash(body.newPassword);
    const [updated] = await sql`
      UPDATE users SET password_hash = ${hash}, token_version = token_version + 1
      WHERE id = ${c.user.id} RETURNING token_version`;
    return { token: await signToken(c.user.id, updated.token_version) };
  });

  r.get("/users", async (c) => {
    requireRole(c.user, "SUPERADMIN");
    const role = c.query.get("role");
    const q = `%${c.query.get("q") ?? ""}%`;
    const scope = c.user.adminLevel === "GLOBAL" ? null : c.user.cohortId;
    return sql`${USER_SELECT}
      LEFT JOIN classrooms cr ON cr.id = a.assigned_class_id
      WHERE (${role}::text IS NULL OR u.role = ${role})
        AND (u.name ILIKE ${q} OR u.email ILIKE ${q})
        AND (${scope}::int IS NULL
             OR s.cohort_id = ${scope}
             OR cr.cohort_id = ${scope}
             OR (u.role = 'ADMIN' AND a.assigned_class_id IS NULL)
             OR u.id = ${c.user.id})
      ORDER BY u.role, u.name`;
  });

  r.post("/users", async (c) => {
    requireRole(c.user, "SUPERADMIN");
    const body = await c.json(createUserSchema);
    if (body.role === "SUPERADMIN" && c.user.adminLevel !== "GLOBAL") {
      throw forbidden("Hanya SuperAdmin GLOBAL yang dapat membuat SuperAdmin.");
    }
    if (body.role === "STUDENT") requireCohortScope(c.user, body.cohortId);
    if (body.role === "ADMIN" && body.assignedClassId !== null) {
      requireCohortScope(c.user, await classCohort(body.assignedClassId));
    }
    const hash = await Bun.password.hash(body.password);

    const id = await sql.begin(async (tx) => {
      const [u] = await tx`
        INSERT INTO users (name, email, password_hash, role)
        VALUES (${body.name}, ${body.email}, ${hash}, ${body.role}) RETURNING id`;
      if (body.role === "STUDENT") {
        await tx`INSERT INTO students (user_id, student_id, cohort_id, study_program, semester)
                 VALUES (${u.id}, ${body.studentId}, ${body.cohortId}, ${body.studyProgram}, ${body.semester})`;
      } else if (body.role === "ADMIN") {
        await tx`INSERT INTO admins (user_id, admin_code, assigned_class_id, appointment_date)
                 VALUES (${u.id}, ${body.adminCode}, ${body.assignedClassId},
                         COALESCE(${body.appointmentDate ?? null}::date, CURRENT_DATE))`;
      } else {
        if (body.adminLevel === "COHORT" && body.cohortId === null) {
          throw badRequest("SuperAdmin tingkat angkatan harus memiliki angkatan.");
        }
        await tx`INSERT INTO superadmins (user_id, admin_level, cohort_id)
                 VALUES (${u.id}, ${body.adminLevel}, ${body.cohortId})`;
      }
      return u.id as number;
    });
    return findUser(id);
  });

  r.put("/users/:id", async (c) => {
    const id = c.intParam("id");
    const target = await findUser(id);
    await requireManageable(c.user, target);
    const body = await c.json(updateUserSchema);
    if (id === c.user.id && body.isActive === false) throw conflict("Anda tidak dapat menonaktifkan akun sendiri.");
    if (target.role === "STUDENT" && body.cohortId != null) requireCohortScope(c.user, body.cohortId);
    if (target.role === "ADMIN" && body.assignedClassId != null) {
      requireCohortScope(c.user, await classCohort(body.assignedClassId));
    }
    const hash = body.password ? await Bun.password.hash(body.password) : null;

    await sql.begin(async (tx) => {
      await tx`
        UPDATE users SET name = ${body.name}, email = ${body.email},
          is_active = COALESCE(${body.isActive ?? null}::boolean, is_active),
          password_hash = COALESCE(${hash}::text, password_hash),
          token_version = token_version + CASE WHEN ${hash}::text IS NULL AND ${body.isActive ?? true}::boolean THEN 0 ELSE 1 END
        WHERE id = ${id}`;
      if (target.role === "STUDENT") {
        await tx`UPDATE students SET
          student_id = COALESCE(${body.studentId ?? null}::text, student_id),
          cohort_id = COALESCE(${body.cohortId ?? null}::int, cohort_id),
          study_program = COALESCE(${body.studyProgram ?? null}::text, study_program),
          semester = COALESCE(${body.semester ?? null}::int, semester)
          WHERE user_id = ${id}`;
      } else if (target.role === "ADMIN") {
        const nextClass = body.assignedClassId === undefined ? target.assignedClassId : body.assignedClassId;
        await tx`UPDATE admins SET
          admin_code = COALESCE(${body.adminCode ?? null}::text, admin_code),
          assigned_class_id = ${nextClass}
          WHERE user_id = ${id}`;
      } else {
        await tx`UPDATE superadmins SET
          admin_level = COALESCE(${body.adminLevel ?? null}::text, admin_level),
          cohort_id = COALESCE(${body.cohortId ?? null}::int, cohort_id)
          WHERE user_id = ${id}`;
      }
    });
    return findUser(id);
  });

  r.delete("/users/:id", async (c) => {
    const id = c.intParam("id");
    if (id === c.user.id) throw conflict("Anda tidak dapat menghapus akun sendiri.");
    await requireManageable(c.user, await findUser(id));
    await sql`DELETE FROM users WHERE id = ${id}`;
  });

  r.get("/cohorts", async (c) => {
    requireRole(c.user, "SUPERADMIN");
    const scope = c.user.adminLevel === "GLOBAL" ? null : c.user.cohortId;
    return sql`
      SELECT co.id, co.name, co.year,
             (SELECT count(*)::int FROM students s WHERE s.cohort_id = co.id) AS "studentCount",
             (SELECT count(*)::int FROM classrooms cr WHERE cr.cohort_id = co.id) AS "classCount"
      FROM cohorts co
      WHERE ${scope}::int IS NULL OR co.id = ${scope}
      ORDER BY co.year DESC, co.name`;
  });

  r.post("/cohorts", async (c) => {
    requireRole(c.user, "SUPERADMIN");
    if (c.user.adminLevel !== "GLOBAL") throw forbidden("Hanya SuperAdmin GLOBAL yang dapat membuat angkatan.");
    const body = await c.json(cohortSchema);
    const [row] = await sql`INSERT INTO cohorts (name, year) VALUES (${body.name}, ${body.year})
                            RETURNING id, name, year, 0 AS "studentCount", 0 AS "classCount"`;
    return row;
  });

  r.put("/cohorts/:id", async (c) => {
    const id = c.intParam("id");
    requireCohortScope(c.user, id);
    const body = await c.json(cohortSchema);
    const [row] = await sql`UPDATE cohorts SET name = ${body.name}, year = ${body.year}
                            WHERE id = ${id} RETURNING id, name, year`;
    if (!row) throw notFound("Angkatan tidak ditemukan.");
    return row;
  });

  r.delete("/cohorts/:id", async (c) => {
    requireRole(c.user, "SUPERADMIN");
    if (c.user.adminLevel !== "GLOBAL") throw forbidden("Hanya SuperAdmin GLOBAL yang dapat menghapus angkatan.");
    await sql`DELETE FROM cohorts WHERE id = ${c.intParam("id")}`;
  });
}
