-- Class Management: kelas dan keanggotaan mahasiswa.

CREATE TABLE classrooms (
  id          SERIAL PRIMARY KEY,
  cohort_id   INTEGER NOT NULL REFERENCES cohorts(id),
  class_name  TEXT NOT NULL,
  course_name TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (cohort_id, class_name, course_name)
);

ALTER TABLE admins
  ADD CONSTRAINT admins_assigned_class_fk
  FOREIGN KEY (assigned_class_id) REFERENCES classrooms(id) ON DELETE SET NULL;

CREATE TABLE memberships (
  id         SERIAL PRIMARY KEY,
  student_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  class_id   INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
  joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  status     TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
  UNIQUE (student_id, class_id)
);

CREATE INDEX memberships_class_idx ON memberships(class_id);
