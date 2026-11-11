import { beforeAll, describe, expect, test } from "bun:test";

/** Pengujian integrasi terhadap API yang sedang berjalan dengan data contoh. */
const BASE = process.env.API_URL ?? "http://localhost:8080/api/v1";
const PASSWORD = "plato12345";
const unique = Date.now();

async function call(method: string, path: string, token?: string, body?: unknown) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      "content-type": "application/json",
      ...(token ? { authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  return { status: res.status, body: text ? JSON.parse(text) : null };
}

const login = async (email: string) => {
  const res = await call("POST", "/auth/login", undefined, { email, password: PASSWORD });
  expect(res.status).toBe(200);
  return res.body.token as string;
};

const dateIn = (days: number) => new Date(Date.now() + days * 86400000).toISOString().slice(0, 10);

let student: string, otherStudent: string, admin: string, otherAdmin: string, global: string, cohortSuper: string;
let oopClassId: number;

beforeAll(async () => {
  [student, otherStudent, admin, otherAdmin, global, cohortSuper] = await Promise.all([
    login("fattah@plato.test"),
    login("annisa@plato.test"),
    login("admin.pbo@plato.test"),
    login("admin.stat@plato.test"),
    login("superadmin@plato.test"),
    login("koordinator2024@plato.test"),
  ]);
  const classes = await call("GET", "/classes", admin);
  oopClassId = classes.body[0].classId;
});

const newTask = (overrides = {}) => ({
  classId: oopClassId,
  title: `Tugas uji ${unique}-${Math.random()}`,
  description: "",
  taskType: "INDIVIDUAL",
  startDate: dateIn(1),
  deadline: dateIn(5),
  ...overrides,
});

describe("autentikasi dan hak akses", () => {
  test("password salah ditolak", async () => {
    const res = await call("POST", "/auth/login", undefined, { email: "fattah@plato.test", password: "salah" });
    expect(res.status).toBe(401);
  });

  test("permintaan tanpa token ditolak", async () => {
    expect((await call("GET", "/tasks")).status).toBe(401);
  });

  test("login mengembalikan role tanpa hash password", async () => {
    const res = await call("POST", "/auth/login", undefined, { email: "fattah@plato.test", password: PASSWORD });
    expect(res.body.user.role).toBe("STUDENT");
    expect(JSON.stringify(res.body)).not.toContain("password");
  });

  test("mahasiswa tidak dapat mengelola pengguna", async () => {
    expect((await call("GET", "/users", student)).status).toBe(403);
  });

  test("logout membatalkan token", async () => {
    const email = `logout${unique}@plato.test`;
    const cohorts = await call("GET", "/cohorts", global);
    const created = await call("POST", "/users", global, {
      role: "STUDENT", name: "Uji Logout", email, password: PASSWORD,
      studentId: `L${unique}`, cohortId: cohorts.body[0].id, studyProgram: "Sains Data", semester: 1,
    });
    expect(created.status).toBe(200);
    const token = await login(email);
    expect((await call("POST", "/auth/logout", token)).status).toBe(204);
    expect((await call("GET", "/users/me", token)).status).toBe(401);
  });

  test("SuperAdmin angkatan tidak dapat membuat kelas di angkatan lain", async () => {
    const cohorts = await call("GET", "/cohorts", global);
    const other = cohorts.body.find((c: { name: string }) => c.name === "Sains Data 2025");
    const res = await call("POST", "/classes", cohortSuper, { cohortId: other.id, className: "X", courseName: `X${unique}` });
    expect(res.status).toBe(403);
  });
});

describe("alur pengajuan tugas", () => {
  test("pengajuan mahasiswa tertahan sampai disetujui lalu memberi notifikasi", async () => {
    const created = await call("POST", "/tasks", student, newTask());
    expect(created.status).toBe(200);
    expect(created.body.status).toBe("PENDING");
    const id = created.body.taskId;

    expect((await call("GET", `/tasks/${id}`, otherStudent)).status).toBe(404);
    expect((await call("POST", `/tasks/${id}/approve`, otherAdmin)).status).toBe(403);

    const approved = await call("POST", `/tasks/${id}/approve`, admin);
    expect(approved.body.status).toBe("APPROVED");
    expect((await call("GET", `/tasks/${id}`, otherStudent)).status).toBe(200);

    const notes = await call("GET", "/notifications", otherStudent);
    expect(notes.body.some((n: { taskId: number; type: string }) => n.taskId === id && n.type === "TASK_PUBLISHED")).toBe(true);

    expect((await call("POST", `/tasks/${id}/approve`, admin)).status).toBe(409);
  });

  test("penolakan wajib beralasan dan diinformasikan ke pengaju", async () => {
    const { body } = await call("POST", "/tasks", student, newTask());
    expect((await call("POST", `/tasks/${body.taskId}/reject`, admin, { reason: " " })).status).toBe(400);
    const rejected = await call("POST", `/tasks/${body.taskId}/reject`, admin, { reason: "Tanggal keliru" });
    expect(rejected.body.status).toBe("REJECTED");
    const notes = await call("GET", "/notifications", student);
    expect(notes.body.some((n: { taskId: number; type: string }) => n.taskId === body.taskId && n.type === "SUBMISSION_REJECTED")).toBe(true);
  });

  test("tenggat sebelum tanggal mulai ditolak", async () => {
    const res = await call("POST", "/tasks", student, newTask({ startDate: dateIn(5), deadline: dateIn(1) }));
    expect(res.status).toBe(400);
  });

  test("milestone di luar rentang tugas ditolak", async () => {
    const res = await call("POST", "/tasks", admin, newTask({
      taskType: "PROJECT",
      milestones: [{ title: "Terlalu jauh", startDate: dateIn(2), deadline: dateIn(30) }],
    }));
    expect(res.status).toBe(400);
  });
});

describe("progres", () => {
  test("progres manual bersifat per mahasiswa dan dibatasi 0-100", async () => {
    const { body } = await call("POST", "/tasks", admin, newTask());
    const id = body.taskId;
    expect((await call("PUT", `/tasks/${id}/progress`, student, { progress: 101 })).status).toBe(400);
    const mine = await call("PUT", `/tasks/${id}/progress`, student, { progress: 80 });
    expect(mine.body.progress).toBe(80);
    expect((await call("GET", `/tasks/${id}`, otherStudent)).body.progress).toBe(0);
  });

  test("progres proyek dihitung dari milestone dan tidak dapat diisi manual", async () => {
    const { body } = await call("POST", "/tasks", admin, newTask({
      taskType: "PROJECT",
      milestones: [
        { title: "A", startDate: dateIn(1), deadline: dateIn(2) },
        { title: "B", startDate: dateIn(3), deadline: dateIn(5) },
      ],
    }));
    expect((await call("PUT", `/tasks/${body.taskId}/progress`, student, { progress: 50 })).status).toBe(409);
    const updated = await call("PUT", `/milestones/${body.milestones[0].milestoneId}/progress`, student, { progress: 100 });
    expect(updated.body.progress).toBe(50);
  });

  test("tugas yang belum disetujui tidak dapat diberi progres", async () => {
    const { body } = await call("POST", "/tasks", student, newTask());
    expect((await call("PUT", `/tasks/${body.taskId}/progress`, student, { progress: 10 })).status).toBe(409);
  });
});

describe("kelas dan dashboard", () => {
  test("Admin hanya melihat kelasnya", async () => {
    const res = await call("GET", "/classes", admin);
    expect(res.body).toHaveLength(1);
  });

  test("mahasiswa bukan anggota tidak dapat melihat anggota kelas", async () => {
    const available = await call("GET", "/classes/available", student);
    const target = available.body[0];
    expect((await call("GET", `/classes/${target.classId}/members`, student)).status).toBe(403);
  });

  test("ringkasan dashboard memuat tren 7 minggu untuk tiap peran", async () => {
    for (const token of [student, admin, global]) {
      const res = await call("GET", "/dashboard/summary", token);
      expect(res.status).toBe(200);
      expect(res.body.completionTrend).toHaveLength(7);
    }
  });
});
