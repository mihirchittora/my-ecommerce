import net from "node:net";
import { spawnSync } from "node:child_process";
import http from "node:http";

const ports = {
  "catalog-service": 8081,
  "inventory-service": 8082,
  "order-service": 8083,
  "cart-service": 8084,
  "auth-service": 8085,
  "customer-service": 8086,
  "payment-service": 8087,
  "shipping-service": 8088,
  "ecommerce-admin-ui": 3000,
  "ecommerce-storefront": 3001,
};

function commandCheck(name, command, args = []) {
  const result = spawnSync(command, args, { encoding: "utf8", timeout: 15_000 });
  if (result.error) return { name, ok: false, detail: result.error.message };
  const output = `${result.stdout ?? ""}${result.stderr ?? ""}`.trim().split(/\r?\n/)[0];
  return { name, ok: result.status === 0, detail: output || `exit code ${result.status}` };
}

function composeCheck() {
  const plugin = commandCheck("Docker Compose", "docker", ["compose", "version"]);
  if (plugin.ok) return plugin;
  const standalone = commandCheck("Docker Compose", "docker-compose", ["version"]);
  if (standalone.ok) return { ...standalone, detail: `${standalone.detail} (standalone command)` };
  return plugin;
}

function pythonCheck() {
  const candidates = process.platform === "win32" ? ["python", "py", "python3"] : ["python3", "python"];
  for (const command of candidates) {
    const result = commandCheck("Python", command, ["--version"]);
    if (result.ok) return result;
  }
  return { name: "Python", ok: false, detail: "Python 3 was not found" };
}

function portCheck(name, port) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host: "127.0.0.1", port });
    socket.setTimeout(350);
    socket.once("connect", () => {
      socket.destroy();
      resolve({ name, ok: false, detail: `port ${port} is already accepting connections` });
    });
    socket.once("timeout", () => {
      socket.destroy();
      resolve({ name, ok: true, detail: `port ${port} is available` });
    });
    socket.once("error", () => {
      socket.destroy();
      resolve({ name, ok: true, detail: `port ${port} is available` });
    });
  });
}

function healthCheck(name, port) {
  return new Promise((resolve) => {
    const request = http.get(`http://127.0.0.1:${port}/actuator/health`, (response) => {
      response.resume();
      resolve({ name, ok: response.statusCode === 200, detail: `HTTP ${response.statusCode}` });
    });
    request.setTimeout(1_500, () => request.destroy());
    request.on("error", () => resolve({ name, ok: false, detail: "not reachable" }));
  });
}

const checks = [
  commandCheck("Java 21+", "java", ["-version"]),
  commandCheck("Maven", "mvn", ["--version"]),
  commandCheck("Node.js", process.execPath, ["--version"]),
  commandCheck("npm", "npm", ["--version"]),
  pythonCheck(),
  commandCheck("Docker daemon", "docker", ["info"]),
  composeCheck(),
];

console.log("Toolchain");
for (const check of checks) console.log(`${check.ok ? "OK" : "MISSING"} ${check.name.padEnd(18)} ${check.detail}`);

console.log("\nPort availability (available is expected before startup)");
const portResults = await Promise.all(Object.entries(ports).map(([name, port]) => portCheck(name, port)));
for (const check of portResults) console.log(`${check.ok ? "OK" : "BUSY"} ${check.name.padEnd(24)} ${check.detail}`);

if (process.argv.includes("--health")) {
  console.log("\nBackend health");
  const healthResults = await Promise.all(Object.entries(ports).slice(0, 8).map(([name, port]) => healthCheck(name, port)));
  for (const check of healthResults) console.log(`${check.ok ? "OK" : "DOWN"} ${check.name.padEnd(24)} ${check.detail}`);
}

const required = checks.slice(0, 5);
process.exitCode = required.every((check) => check.ok) ? 0 : 1;
