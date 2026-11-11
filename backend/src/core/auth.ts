import { jwtVerify, SignJWT } from "jose";
import { sql } from "./db";
import { forbidden, HttpError } from "./http";

export type Role = "STUDENT" | "ADMIN" | "SUPERADMIN";

export interface AuthUser {
  id: number;
  role: Role;
  /** Angkatan mahasiswa/superadmin. Null untuk admin dan superadmin GLOBAL. */
  cohortId: number | null;
  /** Kelas yang dimoderasi Admin. */
  assignedClassId: number | null;
  adminLevel: "COHORT" | "GLOBAL" | null;
}

const secret = new TextEncoder().encode(process.env.JWT_SECRET!);

export function signToken(userId: number, tokenVersion: number) {
  return new SignJWT({ tv: tokenVersion })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(String(userId))
    .setIssuedAt()
    .setExpirationTime("12h")
    .sign(secret);
}

const unauthorized = () => new HttpError(401, "Sesi tidak valid. Silakan masuk kembali.");

export async function authenticate(req: Request): Promise<AuthUser> {
  const header = req.headers.get("authorization");
  if (!header?.startsWith("Bearer ")) throw unauthorized();

  let userId: number;
  let tokenVersion: unknown;
  try {
    const { payload } = await jwtVerify(header.slice(7), secret, { algorithms: ["HS256"] });
    userId = Number(payload.sub);
    tokenVersion = payload.tv;
  } catch {
    throw unauthorized();
  }

  const [row] = await sql`
    SELECT u.id, u.role, u.token_version, u.is_active,
           COALESCE(s.cohort_id, sa.cohort_id) AS cohort_id,
           a.assigned_class_id, sa.admin_level
    FROM users u
    LEFT JOIN students s ON s.user_id = u.id
    LEFT JOIN admins a ON a.user_id = u.id
    LEFT JOIN superadmins sa ON sa.user_id = u.id
    WHERE u.id = ${userId}`;
  if (!row || !row.is_active || row.token_version !== tokenVersion) throw unauthorized();

  return {
    id: row.id,
    role: row.role,
    cohortId: row.cohort_id,
    assignedClassId: row.assigned_class_id,
    adminLevel: row.admin_level,
  };
}

export function requireRole(user: AuthUser, ...roles: Role[]) {
  if (!roles.includes(user.role)) throw forbidden();
}

/** Memastikan SuperAdmin berwenang atas angkatan tersebut. */
export function requireCohortScope(user: AuthUser, cohortId: number | null) {
  requireRole(user, "SUPERADMIN");
  if (user.adminLevel !== "GLOBAL" && user.cohortId !== cohortId) {
    throw forbidden("Data ini berada di luar angkatan yang Anda kelola.");
  }
}
