import { z } from "zod";
import { authenticate, type AuthUser } from "./auth";

export class HttpError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

export const badRequest = (m: string) => new HttpError(400, m);
export const forbidden = (m = "Anda tidak memiliki akses untuk tindakan ini.") => new HttpError(403, m);
export const notFound = (m = "Data tidak ditemukan.") => new HttpError(404, m);
export const conflict = (m: string) => new HttpError(409, m);

export interface Ctx {
  req: Request;
  params: Record<string, string>;
  query: URLSearchParams;
  user: AuthUser;
  json<T extends z.ZodTypeAny>(schema: T): Promise<z.infer<T>>;
  intParam(name: string): number;
}

type Handler = (ctx: Ctx) => Promise<unknown>;

interface Route {
  method: string;
  pattern: RegExp;
  keys: string[];
  handler: Handler;
  isPublic: boolean;
}

/** Router kecil; setiap modul fitur mendaftarkan endpoint-nya sendiri. */
export class Router {
  private routes: Route[] = [];

  add(method: string, path: string, handler: Handler, isPublic = false) {
    const keys: string[] = [];
    const pattern = new RegExp(
      "^" + path.replace(/:(\w+)/g, (_, key) => (keys.push(key), "([^/]+)")) + "$",
    );
    this.routes.push({ method, pattern, keys, handler, isPublic });
  }

  get(path: string, handler: Handler) { this.add("GET", path, handler); }
  post(path: string, handler: Handler) { this.add("POST", path, handler); }
  put(path: string, handler: Handler) { this.add("PUT", path, handler); }
  patch(path: string, handler: Handler) { this.add("PATCH", path, handler); }
  delete(path: string, handler: Handler) { this.add("DELETE", path, handler); }
  public(method: string, path: string, handler: Handler) { this.add(method, path, handler, true); }

  async handle(req: Request): Promise<Response> {
    try {
      const url = new URL(req.url);
      const path = url.pathname.replace(/^\/api\/v1/, "").replace(/\/$/, "") || "/";
      const pathMatches = this.routes.filter((r) => r.pattern.test(path));
      if (pathMatches.length === 0) throw notFound("Endpoint tidak ditemukan.");
      const route = pathMatches.find((r) => r.method === req.method);
      if (!route) throw new HttpError(405, "Metode tidak didukung.");

      const values = route.pattern.exec(path)!.slice(1);
      const params = Object.fromEntries(route.keys.map((k, i) => [k, decodeURIComponent(values[i]!)]));
      const user = route.isPublic ? (null as unknown as AuthUser) : await authenticate(req);

      const result = await route.handler({
        req,
        params,
        query: url.searchParams,
        user,
        json: (schema) => parseBody(req, schema),
        intParam: (name) => parseId(params[name]),
      });
      return result === undefined ? new Response(null, { status: 204 }) : Response.json(result);
    } catch (err) {
      return toErrorResponse(err);
    }
  }
}

async function parseBody<T extends z.ZodTypeAny>(req: Request, schema: T): Promise<z.infer<T>> {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    throw badRequest("Body harus berupa JSON yang valid.");
  }
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    throw badRequest(parsed.error.issues.map((i) => i.message).join(" "));
  }
  return parsed.data;
}

function parseId(value: string | undefined): number {
  const id = Number(value);
  if (!Number.isInteger(id) || id <= 0 || id > 2147483647) throw notFound();
  return id;
}

function toErrorResponse(err: unknown): Response {
  if (err instanceof HttpError) {
    return Response.json({ error: err.message }, { status: err.status });
  }
  const code = (err as { errno?: string; code?: string }).errno ?? (err as { code?: string }).code;
  if (code === "23505") {
    return Response.json({ error: "Data dengan nilai tersebut sudah ada." }, { status: 409 });
  }
  if (code === "23503") {
    return Response.json({ error: "Data masih terhubung dengan data lain." }, { status: 409 });
  }
  console.error(err);
  return Response.json({ error: "Terjadi kesalahan pada server." }, { status: 500 });
}

export const dateString = z
  .string({ required_error: "Tanggal wajib diisi." })
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Format tanggal harus YYYY-MM-DD.")
  .refine((v) => !Number.isNaN(Date.parse(v)), "Tanggal tidak valid.");
