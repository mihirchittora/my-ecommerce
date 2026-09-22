# Troubleshooting

Use the root README for the standard workflow. This page lists checks that are
safe to run on a developer machine and do not print credentials.

## Docker unavailable

Run:

~~~text
docker info
docker compose version
~~~

Start Docker Desktop on macOS or Windows. On macOS with optional Colima, run
colima start and retry docker info. On Linux, make sure the Docker Engine or
Docker Desktop daemon is running and that the current user can access it.

Do not mount a Docker socket into the application containers. Testcontainers
uses the Docker API for tests.

## Port already in use

The diagnostic command checks the expected ports:

~~~text
npm run doctor
~~~

macOS/Linux:

~~~text
lsof -i :8082
~~~

Windows PowerShell:

~~~powershell
Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue
~~~

Stop the process that owns the port, or override the service port and its
published Compose port consistently. The backend defaults are listed in the
root README.

## Testcontainers cannot connect

Make sure Docker responds to docker info before running Maven tests. Docker
Desktop and Linux Docker normally need no extra settings. An explicit DOCKER_HOST
is authoritative when one is already configured.

On macOS, the test-only fallback detects an existing Colima or Docker Desktop
Unix socket only when the Java runtime is Unix-like and no explicit Docker
configuration exists. On Windows it does not construct Unix socket paths;
Testcontainers uses Docker Desktop's supported API discovery.

Do not disable integration tests or replace PostgreSQL with an in-memory database.
If a stale DOCKER_HOST is set, clear it using the shell's normal environment
settings and retry.

## PostgreSQL unavailable

Each Compose project has a health check and its own named volume. Inspect the
specific project:

~~~text
docker compose -f catalog-service/docker-compose.yml ps
docker compose -f catalog-service/docker-compose.yml logs postgres
~~~

The database hostname inside the application container is the Compose service
name, not localhost. A host-run Java process uses the published localhost port.

## Catalog unavailable

Check:

~~~text
curl http://localhost:8081/actuator/health
docker compose -f catalog-service/docker-compose.yml logs app
~~~

Inventory, Order, and Cart use host.docker.internal because the eight backend
Compose projects are independent. On Linux the Compose files add the
host-gateway mapping. If a service is run directly on the host, use
http://localhost:8081 instead.

## Auth unavailable

Check Auth before using protected APIs:

~~~text
curl http://localhost:8085/actuator/health
docker compose -f auth-service/docker-compose.yml logs auth-service
~~~

The initial admin values are read only when the Auth database is initialized.
Changing them later does not change an existing account. For a clean local
development reset, use npm run seed:reset only after confirming SEED_ENV is
development.

## JWT/JWKS errors

The JWT issuer must match the AUTH_ISSUER value in the token. The JWK URL must be
reachable from the process making the request:

- Host-run backend: http://localhost:8085/.well-known/jwks.json
- Backend container in the current Compose layout:
  http://host.docker.internal:8085/.well-known/jwks.json

A 401 usually means missing, expired, wrongly signed, wrong-issuer, or
wrong-audience credentials. A 403 usually means a valid token lacks the required
permission. Do not disable security to work around a configuration error.

## CORS

Set ALLOWED_ORIGINS to the exact browser origins used by the local applications,
normally http://localhost:3000,http://localhost:3001. Do not use a wildcard for a
credentialed production browser flow.

## Windows Docker Desktop

Use Linux containers and run docker info from PowerShell. WSL2 integration is
optional. The normal workflow uses docker compose, Maven, npm, Python, and
Copy-Item; it does not require Git Bash, sed, or Unix socket paths.

If a bind mount is denied, move the checkout to a Docker Desktop shared drive or
enable file sharing for that location. The catalog image upload mount is relative
to catalog-service, so it does not contain a user-specific absolute path.

## macOS Docker Desktop or Colima

Docker Desktop requires no repository changes. With Colima:

~~~text
colima start
docker context use colima
docker info
~~~

The context command is optional and is needed only when the Docker CLI is not
already using Colima. The application does not read ~/.colima or ~/.docker; only
test-only Java tooling performs guarded optional detection.

## Linux Docker

The Compose files add host.docker.internal:host-gateway for backend-to-host
calls. If the Docker installation is old enough not to support the host-gateway
mapping, upgrade Docker Engine/Compose rather than adding a host-specific socket
or absolute path. Container-to-container database DNS remains unchanged.

## Node/npm version mismatch

Run:

~~~text
node --version
npm --version
~~~

Use the current LTS Node.js release supported by Next.js and reinstall the
frontend dependencies from the lock file if needed:

~~~text
npm --prefix ecommerce-admin-ui install
npm --prefix ecommerce-storefront install
~~~

## Java/Maven version mismatch

Run:

~~~text
java -version
mvn --version
~~~

Use Java 21 and Maven 3.9 or newer. Run tests with the Maven file option from
the repository root, or change into a service directory and run mvn clean test.

## Frontend environment files

If a UI starts but calls the wrong backend, recreate its untracked environment
file from the corresponding .env.example. Browser variables belong in the UI
file; service tokens and database passwords must never be placed there.

## Seed failures

Validate frozen data without contacting services:

~~~text
npm run seed:validate
~~~

For a live seed, confirm all required backend health endpoints and the matching
development-only Auth admin password. The seed client prefers SEED_* URLs from
the root .env so Compose container URLs and host URLs do not conflict. A failure
does not print credentials.

