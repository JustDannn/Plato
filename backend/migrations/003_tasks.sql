-- Task and Milestone Management.
-- Progres disimpan per mahasiswa karena satu tugas dipublikasikan ke seluruh kelas.

CREATE TABLE tasks (
  id               SERIAL PRIMARY KEY,
  class_id         INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
  created_by       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title            TEXT NOT NULL,
  description      TEXT NOT NULL DEFAULT '',
  task_type        TEXT NOT NULL CHECK (task_type IN ('INDIVIDUAL', 'GROUP', 'PROJECT')),
  start_date       DATE NOT NULL,
  deadline         DATE NOT NULL,
  status           TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
  rejection_reason TEXT,
  validated_by     INTEGER REFERENCES users(id) ON DELETE SET NULL,
  validated_at     TIMESTAMPTZ,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (deadline >= start_date)
);

CREATE INDEX tasks_class_status_idx ON tasks(class_id, status);

CREATE TABLE milestones (
  id          SERIAL PRIMARY KEY,
  task_id     INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  title       TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  start_date  DATE NOT NULL,
  deadline    DATE NOT NULL,
  CHECK (deadline >= start_date)
);

CREATE INDEX milestones_task_idx ON milestones(task_id);

CREATE TABLE task_progress (
  task_id    INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  student_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  progress   INTEGER NOT NULL CHECK (progress BETWEEN 0 AND 100),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (task_id, student_id)
);

CREATE TABLE milestone_progress (
  milestone_id INTEGER NOT NULL REFERENCES milestones(id) ON DELETE CASCADE,
  student_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  progress     INTEGER NOT NULL CHECK (progress BETWEEN 0 AND 100),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (milestone_id, student_id)
);
