# Milestone 9 production-readiness report

## Outcome

The repository now has a cloud-agnostic single-VM deployment profile that keeps
the eight Spring services separate, places them on one private Docker network,
uses one PostgreSQL server with eight isolated logical databases and roles, and
publishes only a reverse proxy. The measured result is **single-VM integration
and staging ready**, with explicit production conditions still required for
TLS, stable signing keys, backup restore evidence, and full browser/business
journey execution.

## Implemented

- Added `deploy/single-vm/compose.yml` with eight independently built backend
  containers, Postgres, Caddy, healthchecks, dependency-gated startup, named
  volumes, and no `host.docker.internal` dependency.
- Added first-init Postgres role/database provisioning in
  `deploy/single-vm/postgres/init/01-init-databases.sh`. Flyway remains the
  owner of application tables.
- Added explicit gateway routing and public blocking rules in
  `deploy/single-vm/reverse-proxy/Caddyfile`.
- Added safe environment placeholders and deployment, backup, rollback, and
  cloud-mapping documentation.
- Added GitHub Actions jobs for all backend tests, both frontends, seed checks,
  and unified Compose configuration/image builds.
- Added `curl` to service runtime images for healthchecks.
- Removed silent Docker-unavailable skipping from the Order and Cart
  Testcontainers suites.

## Evidence

The current verification run used the standalone `docker-compose` 5.5.1 command
because the Docker Compose plugin was unavailable. Docker ran through Colima on
macOS Apple Silicon.

- Compose config passed.
- All eight backend images built.
- All eight Spring services, Postgres, and Caddy reached healthy state.
- Private DNS calls from Order to Inventory and Auth returned `{"status":"UP"}`.
- The gateway returned 200 for `/healthz` and JWKS, and 404 for public actuator
  and internal routes.
- Only the proxy had host-published ports. The verification host already used
  port 8080, so the proxy was tested on 18080/18443 via supported overrides.
- The initialized databases were `auth_db`, `catalog_db`, `inventory_db`,
  `order_db`, `cart_db`, `customer_db`, `payment_db`, and `shipping_db`.
- Catalog could connect to `catalog_db`; its attempt to connect to
  `inventory_db` was denied by PostgreSQL.
- The eight Maven suites passed 72 tests with no skipped tests.
- Admin checks passed with 8 tests. Storefront checks passed with 8 tests and
  2 intentionally skipped network E2E cases because no live E2E base URL was
  configured.
- Seed validation and dry-run passed. `APP_ENV=production SEED_ENV=production
  npm run seed` was refused as designed.

## Security and portability assessment

The source audit found no confirmed cross-service JDBC access. JWT validation,
audience/issuer/expiry checks, webhook signatures, idempotency entities, and
service-token configuration remain service-owned. The unified profile uses
private service DNS, exposes no backend or database host ports, and blocks
diagnostic routes at the proxy.

The profile is provider-neutral at the container/configuration boundary. The
current runtime evidence is macOS Apple Silicon only; Windows, Linux, macOS
Intel, TLS issuance, and a restore drill remain follow-up verification items.

## Required before calling it production-ready

1. Inject persistent `JWT_PRIVATE_KEY_PATH` and `JWT_PUBLIC_KEY_PATH` material;
   the example profile intentionally falls back to an ephemeral development
   signing key when no key files are supplied.
2. Replace every example credential and sandbox integration value, configure
   the real DNS name/CORS origins, and enable TLS at Caddy or an upstream load
   balancer.
3. Run and record a restore drill for every logical database, plus a scheduled
   backup retention/monitoring policy.
4. Execute authenticated browser and API journeys against a seeded gateway,
   including inventory conflict, checkout idempotency, payment/webhook replay,
   COD collection, and failure-path checks.
5. Run the portability matrix on at least one Windows Docker Desktop and one
   Linux Docker Engine runner, then allow the GitHub workflow to complete.

These are release conditions, not hidden assumptions. The deployment remains
usable for integration and staging with the documented sandbox and verification
settings.
