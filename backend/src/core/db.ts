import { SQL } from "bun";
import { readdir } from "node:fs/promises";
import { join } from "node:path";

export const sql = new SQL(process.env.DATABASE_URL!);

const MIGRATIONS_DIR = join(import.meta.dir, "../../migrations");

/** Menjalankan file .sql yang belum pernah dijalankan, berurutan menurut nama. */
export async function migrate() {
  await sql`CREATE TABLE IF NOT EXISTS schema_migrations (
    name TEXT PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
  )`;
  const applied = new Set(
    (await sql`SELECT name FROM schema_migrations`).map((r: { name: string }) => r.name),
  );
  const files = (await readdir(MIGRATIONS_DIR)).filter((f) => f.endsWith(".sql")).sort();

  for (const file of files) {
    if (applied.has(file)) continue;
    const content = await Bun.file(join(MIGRATIONS_DIR, file)).text();
    await sql.begin(async (tx) => {
      await tx.unsafe(content);
      await tx`INSERT INTO schema_migrations (name) VALUES (${file})`;
    });
    console.log(`[migrate] ${file}`);
  }
}
