import { spawnSync } from "node:child_process";

let baseUrl = process.env.STOREFRONT_E2E_BASE_URL;
if (!baseUrl) {
  try {
    const response = await fetch("http://localhost:3001", { signal: AbortSignal.timeout(750) });
    if (response.ok) baseUrl = "http://localhost:3001";
  } catch {
    // The test file intentionally skips when no local storefront is running.
  }
}

const result = spawnSync(
  process.execPath,
  ["--test", "test/e2e.test.mjs"],
  {
    env: {
      ...process.env,
      ...(baseUrl ? { STOREFRONT_E2E_BASE_URL: baseUrl } : {}),
    },
    stdio: "inherit",
  },
);

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);
