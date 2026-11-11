-- Information and Monitoring: notifikasi pengguna.

CREATE TABLE notifications (
  id         SERIAL PRIMARY KEY,
  user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  task_id    INTEGER REFERENCES tasks(id) ON DELETE CASCADE,
  message    TEXT NOT NULL,
  type       TEXT NOT NULL CHECK (type IN ('TASK_PUBLISHED', 'SUBMISSION_APPROVED', 'SUBMISSION_REJECTED', 'NEW_SUBMISSION', 'REMINDER')),
  is_read    BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX notifications_user_idx ON notifications(user_id, is_read, created_at DESC);
