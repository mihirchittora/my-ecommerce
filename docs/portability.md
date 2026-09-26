# Cross-platform portability

## Scope

This repository is audited for developer workflows on:

- macOS Intel and Apple Silicon
- Windows 10/11 with Docker Desktop and optional WSL2
- Linux with Docker Engine or Docker Desktop

The audit covers Java/Maven services, Next.js applications, Docker Compose,
Testcontainers, the Python seed tool, local image storage, environment files,
Git line endings, ignore rules, CI configuration, and developer documentation.

## Supported workflow

The supported workflow is:

1. Copy the root .env.example to .env.
2. Start Docker Desktop, Docker Engine, or optional Colima.
3. Start each backend with its service Compose file.
4. Run the Admin UI and Storefront with npm.
5. Run npm run seed after backend health checks pass.

The root README contains the exact commands and PowerShell file-copy equivalents.

## Runtime requirements

| Platform | Runtime | Docker |
| --- | --- | --- |
| macOS Intel/Apple Silicon | Java 21, Maven 3.9+, Node/npm, Python 3.11+ | Docker Desktop or optional Colima |
| Windows 10/11 | Java 21, Maven 3.9+, Node/npm, Python 3.11+ | Docker Desktop Linux containers; WSL2 optional |
| Linux | Java 21, Maven 3.9+, Node/npm, Python 3.11+ | Docker Engine or Docker Desktop with Compose v2 |

No essential workflow depends on a user-specific path, a shell-specific Maven
wrapper, Git Bash, a Unix socket mount, or a distro package manager.

## Docker and networking

The eight Compose files use relative build contexts and named database volumes.
The catalog image directory is mounted as ./uploads:/app/uploads; it contains no
host-specific absolute path.

The Compose projects are independent. A database is addressed by its Compose
service name within its project. Backend calls across projects use
host.docker.internal and the files add host-gateway for Linux Docker Engine.
From the host, use localhost and the published port. localhost inside a
container is the current container.

The `deploy/single-vm` profile uses a different, portable placement: all ten
containers share the explicit `ecommerce-private` network, backend calls use
service DNS names, and only the Caddy proxy publishes host ports. It contains
no `host.docker.internal` dependency and no host Docker socket mount. Its
PostgreSQL container creates eight logical databases and service-owned roles;
Flyway remains responsible for each schema.

No application Compose file requires /var/run/docker.sock, ~/.colima, or
~/.docker. Testcontainers fallback detection is isolated to test sources,
honors explicit DOCKER_HOST first, and only considers optional Unix socket
candidates on Unix-like hosts.

## Maven and Testcontainers

Every backend uses Java 21 and Maven commands that are platform-neutral. Docker
builds are multi-stage for all services, so a developer does not need to create
target JARs before docker compose up --build.

Integration tests use Testcontainers PostgreSQL. Testcontainers is not disabled.
Docker Desktop, Colima, and Linux Docker are supported through the Docker API;
Windows does not receive a fabricated Unix socket path.

In the current Colima environment, Maven integration tests required
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` so the Ryuk
container could address the daemon through the container-visible socket. The
override is documented for that runner and is not part of application runtime.

## Node and seed tooling

Frontend package scripts use npm, Next.js, and Node-based tooling. They do not
require rm, cp, mv, export, source, or a shell-specific environment assignment.

The canonical seed entry point is Python:
python dev-seed/scripts/seed.py
or npm run seed. The Unix seed.sh wrapper remains an optional convenience and is
not required on Windows. The seed loads an untracked root .env file without
requiring users to export a large set of variables.

## Paths and uploads

Java image storage uses Path APIs, normalizes and validates storage keys, creates
directories with Files, and stores portable slash-separated database keys.
The configured default is relative ./uploads. The host mount is relative to the
catalog-service Compose file.

The seed scripts use pathlib.Path. The Next.js applications do not embed
operating-system filesystem paths in their build scripts.

## Environment and secrets

Tracked .env.example files contain development placeholders only. Real .env
files, API keys, gateway credentials, webhook secrets, and production database
passwords are ignored by Git. Browser environment files contain public URLs only.

Root .env values are loaded by Compose and by the seed tool. Service-specific
.env.example files remain available for host-run Java development.

## Git portability

.gitattributes sets text=auto and explicit LF/CRLF rules for shell, Python,
PowerShell, CMD, and batch scripts. .gitignore covers node_modules, Maven
target, Next output, dotenv files, editor metadata, coverage, logs, Python
caches, local database files, macOS metadata, and Windows thumbnails without
ignoring required seed image assets.

## Verified versus static-audit only

Verified in the current macOS Apple Silicon execution environment:

- Static searches across tracked source, scripts, Compose, Dockerfiles, env
  examples, tests, and documentation.
- All eight Compose files with the available standalone Compose command.
- Java compilation and packaging for all eight Maven services.
- Frontend Admin UI and Storefront lint/build checks and unit tests.
- Frozen seed-data validation and Python syntax checks.
- The cross-platform diagnostic command and the storefront E2E wrapper's
  expected no-server skip behavior.
- Source review of Java path handling, image storage, Testcontainers setup, and
  environment precedence.
- All eight backend test suites with Testcontainers enabled: 72 tests passed,
  with no skipped tests.
- Both frontend lint/test/typecheck/build workflows.
- Single-VM Compose config, all eight backend image builds, clean database
  initialization, private DNS health calls, gateway routing, and public port
  isolation.
- Seed validation, dry-run behavior, and production-environment refusal.

The Docker daemon was available through Colima on macOS Apple Silicon. The
standalone `docker-compose` command was used because the `docker compose`
plugin was unavailable. The host's port 8080 was already occupied, so the
runtime gateway verification used `PROXY_HTTP_PORT=18080` and
`PROXY_HTTPS_PORT=18443`; the profile itself remains configurable for 80/443 or
any free ports.

Static-audit only unless a matching runner is available:

- Windows 10/11, PowerShell, and Docker Desktop.
- Linux Docker Engine.
- macOS Intel hardware.
- macOS Docker Desktop or Colima when not active in the current environment.
- TLS certificate issuance and restore-from-backup execution.
- Full browser E2E against a seeded, running gateway; the Storefront network
  E2E cases intentionally skip when `STOREFRONT_E2E_BASE_URL` is unset.

Do not interpret a static audit as an OS runtime test. Follow the manual matrix
below on each target platform.

## Manual verification matrix

macOS Docker Desktop:

~~~text
docker info
docker compose -f catalog-service/docker-compose.yml config
mvn -f catalog-service/pom.xml clean test
npm --prefix ecommerce-admin-ui run lint
npm --prefix ecommerce-admin-ui run build
npm --prefix ecommerce-storefront run lint
npm --prefix ecommerce-storefront run build
npm run seed:validate
~~~

macOS Colima:

~~~text
colima start
docker context use colima
docker info
mvn -f catalog-service/pom.xml clean test
~~~

Windows PowerShell:

~~~powershell
docker info
docker compose -f catalog-service/docker-compose.yml config
mvn -f catalog-service/pom.xml clean test
npm --prefix ecommerce-admin-ui run lint
npm --prefix ecommerce-admin-ui run build
npm --prefix ecommerce-storefront run lint
npm --prefix ecommerce-storefront run build
npm run seed:validate
~~~

Linux:

~~~text
docker info
docker compose -f catalog-service/docker-compose.yml config
mvn -f catalog-service/pom.xml clean test
npm --prefix ecommerce-admin-ui run lint
npm --prefix ecommerce-admin-ui run build
npm --prefix ecommerce-storefront run lint
npm --prefix ecommerce-storefront run build
npm run seed:validate
~~~

For full acceptance, repeat the eight backend Maven commands, both frontend test
commands, a Compose up/down cycle, and a development seed on the target OS.
