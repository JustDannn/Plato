-- User Management: angkatan, pengguna, dan data khusus tiap peran.

CREATE TABLE cohorts (
  id         SERIAL PRIMARY KEY,
  name       TEXT NOT NULL UNIQUE,
  year       INTEGER NOT NULL CHECK (year BETWEEN 2000 AND 2100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
  id            SERIAL PRIMARY KEY,
  name          TEXT NOT NULL,
  email         TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role          TEXT NOT NULL CHECK (role IN ('STUDENT', 'ADMIN', 'SUPERADMIN')),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  -- Dinaikkan saat logout/ganti password sehingga token lama tidak berlaku.
  token_version INTEGER NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE students (
  user_id       INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  student_id    TEXT NOT NULL UNIQUE,
  cohort_id     INTEGER NOT NULL REFERENCES cohorts(id),
  study_program TEXT NOT NULL,
  semester      INTEGER NOT NULL CHECK (semester BETWEEN 1 AND 14)
);

CREATE TABLE admins (
  user_id           INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  admin_code        TEXT NOT NULL UNIQUE,
  assigned_class_id INTEGER UNIQUE,
  appointment_date  DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE superadmins (
  user_id     INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  admin_level TEXT NOT NULL CHECK (admin_level IN ('COHORT', 'GLOBAL')),
  cohort_id   INTEGER REFERENCES cohorts(id),
  CHECK (admin_level = 'GLOBAL' OR cohort_id IS NOT NULL)
);
