import { sql } from "./core/db";

const DEMO_PASSWORD = "plato12345";

const dayOffset = (days: number) => {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
};

/** Mengisi data contoh hanya ketika belum ada pengguna sama sekali. */
export async function seedDemoData() {
  const [{ count }] = await sql`SELECT count(*)::int AS count FROM users`;
  if (count > 0) return;

  const hash = await Bun.password.hash(DEMO_PASSWORD);

  await sql.begin(async (tx) => {
    const [c24] = await tx`INSERT INTO cohorts (name, year) VALUES ('Sains Data 2024', 2024) RETURNING id`;
    const [c25] = await tx`INSERT INTO cohorts (name, year) VALUES ('Sains Data 2025', 2025) RETURNING id`;

    const addUser = async (name: string, email: string, role: string) => {
      const [u] = await tx`INSERT INTO users (name, email, password_hash, role)
                           VALUES (${name}, ${email}, ${hash}, ${role}) RETURNING id`;
      return u.id as number;
    };
    const addClass = async (cohort: number, className: string, courseName: string, description: string) => {
      const [row] = await tx`INSERT INTO classrooms (cohort_id, class_name, course_name, description)
                             VALUES (${cohort}, ${className}, ${courseName}, ${description}) RETURNING id`;
      return row.id as number;
    };

    const global = await addUser("Super Admin", "superadmin@plato.test", "SUPERADMIN");
    await tx`INSERT INTO superadmins (user_id, admin_level, cohort_id) VALUES (${global}, 'GLOBAL', NULL)`;
    const cohortSuper = await addUser("Koordinator 2024", "koordinator2024@plato.test", "SUPERADMIN");
    await tx`INSERT INTO superadmins (user_id, admin_level, cohort_id) VALUES (${cohortSuper}, 'COHORT', ${c24.id})`;

    const oop = await addClass(c24.id, "SD-48-01", "Pemrograman Berorientasi Objek", "Konsep OOP dan aplikasi desktop dengan Java.");
    const stat = await addClass(c24.id, "SD-48-01", "Statistika Inferensial", "Estimasi, uji hipotesis, dan regresi.");
    const db = await addClass(c24.id, "SD-48-01", "Basis Data", "Perancangan dan pengelolaan basis data relasional.");
    const ml = await addClass(c24.id, "SD-48-02", "Pembelajaran Mesin", "Dasar supervised dan unsupervised learning.");
    await addClass(c25.id, "SD-49-01", "Kalkulus Lanjut", "Kalkulus multivariabel.");

    const admin = async (name: string, email: string, code: string, classId: number) => {
      const id = await addUser(name, email, "ADMIN");
      await tx`INSERT INTO admins (user_id, admin_code, assigned_class_id) VALUES (${id}, ${code}, ${classId})`;
      return id;
    };
    const adminOop = await admin("Admin PBO", "admin.pbo@plato.test", "ADM-PBO", oop);
    const adminStat = await admin("Admin Statistika", "admin.stat@plato.test", "ADM-STAT", stat);
    const adminDb = await admin("Admin Basis Data", "admin.db@plato.test", "ADM-DB", db);

    const student = async (name: string, email: string, nim: string) => {
      const id = await addUser(name, email, "STUDENT");
      await tx`INSERT INTO students (user_id, student_id, cohort_id, study_program, semester)
               VALUES (${id}, ${nim}, ${c24.id}, 'Sains Data', 5)`;
      return id;
    };
    const fattah = await student("Nur Fattah Hamdani", "fattah@plato.test", "103102400001");
    const annisa = await student("Annisa Sofia Albana", "annisa@plato.test", "103102400030");
    const talitha = await student("Talitha Ineztasia", "talitha@plato.test", "103102400067");
    const nashwa = await student("Nashwa Aqeela Irtisa Rudin", "nashwa@plato.test", "103102400006");
    const everyone = [fattah, annisa, talitha, nashwa];

    for (const classId of [oop, stat, db]) {
      for (const s of everyone) await tx`INSERT INTO memberships (student_id, class_id) VALUES (${s}, ${classId})`;
    }

    const addTask = async (classId: number, by: number, validator: number | null, title: string, description: string, type: string, start: number, end: number, status = "APPROVED") => {
      const [t] = await tx`
        INSERT INTO tasks (class_id, created_by, title, description, task_type, start_date, deadline, status, validated_by, validated_at)
        VALUES (${classId}, ${by}, ${title}, ${description}, ${type}, ${dayOffset(start)}::date, ${dayOffset(end)}::date, ${status},
                ${validator}, ${validator ? new Date() : null})
        RETURNING id`;
      return t.id as number;
    };
    const addMilestone = async (taskId: number, title: string, start: number, end: number) => {
      const [m] = await tx`INSERT INTO milestones (task_id, title, start_date, deadline)
                           VALUES (${taskId}, ${title}, ${dayOffset(start)}::date, ${dayOffset(end)}::date) RETURNING id`;
      return m.id as number;
    };
    const setTaskProgress = (taskId: number, s: number, progress: number, daysAgo: number) =>
      tx`INSERT INTO task_progress (task_id, student_id, progress, updated_at)
         VALUES (${taskId}, ${s}, ${progress}, now() - make_interval(days => ${daysAgo}))`;
    const setMilestoneProgress = (id: number, s: number, progress: number, daysAgo: number) =>
      tx`INSERT INTO milestone_progress (milestone_id, student_id, progress, updated_at)
         VALUES (${id}, ${s}, ${progress}, now() - make_interval(days => ${daysAgo}))`;

    const project = await addTask(oop, adminOop, adminOop, "Tugas Besar PLATO", "Aplikasi desktop JavaFX dengan backend REST API.", "PROJECT", -14, 40);
    const m1 = await addMilestone(project, "Analisis dan desain", -14, -5);
    const m2 = await addMilestone(project, "Implementasi fitur inti", -4, 18);
    const m3 = await addMilestone(project, "Integrasi dan pengujian", 19, 33);
    const m4 = await addMilestone(project, "Dokumentasi dan demo", 34, 40);
    for (const s of everyone) await setMilestoneProgress(m1, s, 100, 8);
    await setMilestoneProgress(m2, fattah, 45, 1);
    await setMilestoneProgress(m2, annisa, 30, 2);
    await setMilestoneProgress(m3, fattah, 0, 1);
    await setMilestoneProgress(m4, fattah, 0, 1);

    const quiz = await addTask(oop, adminOop, adminOop, "Latihan Inheritance", "Kerjakan latihan pewarisan class.", "INDIVIDUAL", -21, -7);
    const report = await addTask(stat, adminStat, adminStat, "Laporan Uji Hipotesis", "Analisis dataset dengan uji t.", "INDIVIDUAL", -6, 3);
    const regress = await addTask(stat, annisa, adminStat, "Proyek Regresi Linier", "Kerja kelompok analisis regresi.", "GROUP", -2, 12);
    const erd = await addTask(db, adminDb, adminDb, "Perancangan ERD", "ERD sistem akademik.", "GROUP", -28, -12);
    const queries = await addTask(db, adminDb, adminDb, "Latihan Query SQL", "JOIN dan agregasi.", "INDIVIDUAL", 0, 7);
    await addTask(db, adminDb, adminDb, "Normalisasi Skema", "Sampai bentuk 3NF.", "INDIVIDUAL", 4, 16);

    for (const s of everyone) {
      await setTaskProgress(quiz, s, 100, 20);
      await setTaskProgress(erd, s, 100, 32);
    }
    await setTaskProgress(report, fattah, 60, 1);
    await setTaskProgress(regress, fattah, 25, 1);
    await setTaskProgress(queries, fattah, 10, 0);

    await addTask(oop, talitha, null, "Kuis Polymorphism", "Dosen menginfokan kuis lewat grup kelas.", "INDIVIDUAL", 2, 6, "PENDING");
    await addTask(db, fattah, null, "Resume Bab Transaksi", "Menunggu konfirmasi Admin.", "INDIVIDUAL", 1, 9, "PENDING");

    const encapsulation = await addTask(oop, adminOop, adminOop, "Latihan Encapsulation", "Terapkan getter dan validasi.", "INDIVIDUAL", -9, -1);
    await setTaskProgress(encapsulation, fattah, 100, 1);

    for (const s of everyone) {
      await tx`INSERT INTO notifications (user_id, task_id, message, type)
               VALUES (${s}, ${queries}, 'Tugas baru dipublikasikan: Latihan Query SQL', 'TASK_PUBLISHED')`;
    }
    await tx`INSERT INTO notifications (user_id, task_id, message, type)
             VALUES (${annisa}, ${regress}, 'Pengajuan "Proyek Regresi Linier" disetujui.', 'SUBMISSION_APPROVED')`;
  });

  console.log(`[seed] Data contoh dibuat. Password semua akun: ${DEMO_PASSWORD}`);
}
