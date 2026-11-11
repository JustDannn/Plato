import { migrate } from "./core/db";
import { Router } from "./core/http";
import { registerClassRoutes } from "./modules/classes/routes";
import { registerMonitoringRoutes } from "./modules/monitoring/routes";
import { registerTaskRoutes } from "./modules/tasks/routes";
import { registerUserRoutes } from "./modules/users/routes";
import { seedDemoData } from "./seed";

await migrate();
if (process.env.SEED_DEMO_DATA === "true") await seedDemoData();

const router = new Router();
router.public("GET", "/health", async () => ({ status: "ok" }));
registerUserRoutes(router);
registerClassRoutes(router);
registerTaskRoutes(router);
registerMonitoringRoutes(router);

const server = Bun.serve({
  port: Number(process.env.PORT ?? 8080),
  fetch: (req) => router.handle(req),
});

console.log(`[plato] API berjalan di port ${server.port}`);
