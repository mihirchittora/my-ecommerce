# Milestone 9 verification matrix

Measured on 2026-09-26 in the repository's macOS Apple Silicon/Colima
environment. `PASS` means the check actually ran; `AUDIT` means source or
configuration review; `NOT RUN` is intentionally not presented as a passing
result.

| Area | Scenario | Actual | Result |
| --- | --- | --- | --- |
| Docker | Unified Compose config | `docker-compose ... config --quiet` passed; 10 services, one explicit private network, no service/database host ports | PASS |
| Docker | Backend image build | Auth, Catalog, Inventory, Order, Cart, Customer, Payment, and Shipping images built successfully | PASS |
| PostgreSQL | Init roles/databases | Eight service databases initialized in one Postgres server; service role connected to its own DB and Catalog was denied `CONNECT` to `inventory_db` | PASS |
| Health | Startup/readiness | Postgres, all eight Spring services, and Caddy reached healthy state through dependency-gated startup | PASS |
| Gateway | Public boundary | `/healthz` and JWKS returned 200; public actuator and `/internal/*` returned 404; only proxy ports were host-published | PASS |
| Backend | Service integration suites | 72 tests passed across eight Maven services; 0 failures and 0 skipped | PASS |
| Auth/AuthZ | JWT issuer/audience/expiry, JWKS, permissions, ownership | Existing service tests plus static security audit; full external penetration scan not run | AUDIT |
| Catalog | Public reads and protected writes | Existing Catalog tests passed; gateway routing verified; full authenticated browser flow not run | AUDIT |
| Inventory | Physical-unit reservation and conflict | Existing Inventory tests passed; dedicated online concurrency run not performed | AUDIT |
| Cart/Order | Checkout idempotency | Existing Cart/Order tests passed, including persistence behavior; full seeded gateway checkout not run | AUDIT |
| Payment/Shipping | Sandbox signatures and idempotency | Existing Payment/Shipping tests passed; live sandbox callback not invoked | AUDIT |
| COD | No gateway call, later collection | Seed scenario and service logic are present; end-to-end COD lifecycle not run | NOT RUN |
| Seed | Dataset validation and safety | `npm run seed:validate`, `npm run seed:dry-run`, and production refusal all passed; seed remains API-only | PASS |
| Frontend | Admin lint/test/typecheck/build | Passed; 8 tests passed | PASS |
| Frontend | Storefront lint/test/typecheck/build | Passed; 8 tests passed and 2 network E2E cases skipped because `STOREFRONT_E2E_BASE_URL` was unset | PASS |
| Portability | Host/runtime independence | Static audit plus current macOS Apple Silicon runtime; Windows, Linux, and macOS Intel were not run | AUDIT |
| Backups | Logical dump and restore | Backup commands are documented; restore drill was not executed | NOT RUN |
| Architecture | Domain and database ownership | Eight separate Spring services preserved; no confirmed cross-service JDBC access found | AUDIT |
| CI | GitHub workflow | Backend, frontend, seed, and Compose jobs are defined; hosted CI execution was not triggered from this workspace | AUDIT |

The detailed deployment topology and operational commands are in
`deploy/single-vm/README.md`. The overall release classification and remaining
conditions are in `docs/milestone-9-report.md`.
