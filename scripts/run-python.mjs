import { spawnSync } from "node:child_process";

const candidates = process.platform === "win32" ? ["python", "py", "python3"] : ["python3", "python"];
for (const command of candidates) {
  const result = spawnSync(command, process.argv.slice(2), { stdio: "inherit" });
  if (!result.error) {
    process.exitCode = result.status ?? 1;
    process.exit();
  }
}

console.error("Python 3 was not found. Install Python 3.11+ and retry.");
process.exitCode = 1;
