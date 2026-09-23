# my-ecommerce

This repository contains independently deployable Java services and two Next.js
applications for a local e-commerce platform. The default development workflow
uses Docker Compose for PostgreSQL and backend services, then runs the browser
applications on the host.

## Architecture

| Component | Directory | Host port |
| --- | --- | ---: |
| Catalog | catalog-service | 8081 |
| Inventory | inventory-service | 8082 |
| Order | order-service | 8083 |
| Cart | cart-service | 8084 |
| Auth | auth-service | 8085 |
| Customer | customer-service | 8086 |
| Payment | payment-service | 8087 |
| Shipping | shipping-service | 8088 |
| Admin UI | ecommerce-admin-ui | 3000 |
| Storefront | ecommerce-storefront | 3001 |

Each backend has a dedicated docker-compose.yml and PostgreSQL volume. The
Compose projects are intentionally independent; backend containers call each
other through the host gateway, while each application calls its own database
by the Compose service name.

Catalog is the sole owner of category hierarchy, metadata, and category images:

```text
Catalog Service → Category → CategoryImage → Storefront / Admin UI
```

The storefront derives all category navigation from Catalog responses and never
stores a duplicate category list. Admin image mutations use Catalog's existing
JWT permission boundary (`CATEGORY_UPDATE`), while public category/image reads
remain anonymous.

## Prerequisites

All platforms need:

- Java 21
- Maven 3.9+
- Node.js 20+ and npm
- Python 3.11+ for the development seed tool
- Docker with Compose v2

macOS:

- Docker Desktop, or Colima with the Docker CLI and Compose plugin
- Apple Silicon and Intel are supported by the official multi-architecture
  images used by the project

Windows:

- Docker Desktop with Linux containers enabled
- WSL2 is recommended by Docker Desktop but Git Bash is not required

Linux:

- Docker Engine or Docker Desktop with the Compose v2 plugin
- A distro-neutral Docker installation; no systemd, apt, or specific distro
  is required by this repository

Check the local toolchain with:

~~~text
npm run doctor
~~~

The diagnostic does not print environment values or secrets. Add -- --health
to probe already-running backend health endpoints.

## Supported Platforms

The intended local workflow is supported on macOS, Windows 10/11, and Linux.
Docker Desktop and Linux Docker Engine use the same Compose files. Colima is an
optional macOS Docker runtime, not a repository requirement.

## Quick Start

1. Clone the repository and change into its directory.
2. Copy the root .env.example to .env and change the development-only admin
   password. The seed tool reads this file automatically.
3. Start Docker Desktop, Docker Engine, or the optional Colima runtime.
4. Validate the Compose files with docker compose config.
5. Start the eight backend Compose projects.
6. Copy each frontend example environment file, install dependencies, and start
   the Admin UI and Storefront.
7. Run the repeatable development seed.

The commands below are the complete workflow. Run each npm run command in its own
terminal when the process is long-running.

## Local Development

### 1. Configure local values

macOS/Linux:

~~~text
cp .env.example .env
cp ecommerce-admin-ui/.env.example ecommerce-admin-ui/.env.local
cp ecommerce-storefront/.env.example ecommerce-storefront/.env.local
~~~

Windows PowerShell:

~~~powershell
Copy-Item .env.example .env
Copy-Item ecommerce-admin-ui/.env.example ecommerce-admin-ui/.env.local
Copy-Item ecommerce-storefront/.env.example ecommerce-storefront/.env.local
~~~

The root file contains local-only database defaults, Auth bootstrap values, and
host-published URLs for the seed tool. The frontend files contain browser URLs.
Do not put production credentials in any example file.

### 2. Start the backend services

Run from the repository root:

~~~text
docker compose -f auth-service/docker-compose.yml up -d --build
docker compose -f catalog-service/docker-compose.yml up -d --build
docker compose -f inventory-service/docker-compose.yml up -d --build
docker compose -f order-service/docker-compose.yml up -d --build
docker compose -f cart-service/docker-compose.yml up -d --build
docker compose -f customer-service/docker-compose.yml up -d --build
docker compose -f payment-service/docker-compose.yml up -d --build
docker compose -f shipping-service/docker-compose.yml up -d --build
~~~

The images build their JARs inside Docker, so a local target/ directory is not
required before docker compose up --build.

Validate before startup, or when diagnosing interpolation problems:

~~~text
docker compose -f auth-service/docker-compose.yml config
docker compose -f catalog-service/docker-compose.yml config
~~~

### 3. Start the browser applications

Install once and then start each application in a separate terminal:

~~~text
npm --prefix ecommerce-admin-ui install
npm --prefix ecommerce-admin-ui run dev
~~~

~~~text
npm --prefix ecommerce-storefront install
npm --prefix ecommerce-storefront run dev
~~~

The same commands work in macOS/Linux shells, PowerShell, and CMD. They use the
repository's lock files and do not require shell environment assignments.

## Command Matrix

| Task | macOS/Linux | Windows PowerShell |
| --- | --- | --- |
| Copy root env | `cp .env.example .env` | `Copy-Item .env.example .env` |
| Start Docker | `docker info` or `colima start` | Start Docker Desktop, then `docker info` |
| Validate Compose | `docker compose ... config` | `docker compose ... config` |
| Build backend | `mvn -f <service>/pom.xml clean package` | `mvn -f <service>/pom.xml clean package` |
| Build frontend | `npm --prefix ecommerce-admin-ui run build` | same command |
| Seed | `npm run seed` | `npm run seed` |
| Reset seed | `npm run seed:reset` | `npm run seed:reset` |

### 4. Seed development data

After Auth and all required backend services are healthy:

~~~text
npm run seed
~~~

Equivalent direct commands:

~~~text
python3 dev-seed/scripts/seed.py  # macOS/Linux
python dev-seed/scripts/seed.py   # Windows
~~~

On Windows installations that expose only the Python launcher, use
py dev-seed/scripts/seed.py. The npm entry point auto-detects python3, python,
or py. The seed is restricted to SEED_ENV=development or
SEED_ENV=test, is repeatable, and writes its manifest under dev-seed.

## macOS

Docker Desktop is the default macOS option. Start it, then use the Quick Start
commands unchanged. Both Intel and Apple Silicon are supported by the base
images used here.

## Windows

Docker Desktop with Linux containers is the recommended setup. WSL2 integration
is optional but useful for Docker Desktop's Linux backend. Use PowerShell or CMD
for the normal workflow; Git Bash is not required.

PowerShell equivalents for the few file-copy operations are shown above. Maven,
Node, npm, Python, and docker compose commands are otherwise identical.

## Linux

Use Docker Engine or Docker Desktop with Compose v2. The Compose files add the
portable host.docker.internal:host-gateway mapping needed by Docker Engine so
Linux does not depend on a platform-specific host alias. No distro-specific
package manager command is part of the project workflow.

## Docker

From the host, use http://localhost:<port> for published service ports. Inside
one of these independent Compose projects:

Published ports can be changed without editing Compose files by setting the
matching `<SERVICE>_HOST_PORT` and `<SERVICE>_DB_HOST_PORT` variables, such as
`CATALOG_HOST_PORT` or `CATALOG_DB_HOST_PORT`. Keep the frontend and seed URLs
in sync when changing backend host ports; container-internal ports stay fixed.

- The local database is reached by its Compose service name, such as
  jdbc:postgresql://catalog-db:5432/... or jdbc:postgresql://postgres:5432/....
- A backend in another Compose project is reached through
  http://host.docker.internal:<port>.
- localhost inside a container means that same container; it is never another
  backend service.

If all services are later placed in one Compose network, use service DNS names
such as http://catalog-service:8081 instead of the host gateway. The current
independent Compose layout intentionally uses the host gateway and does not
require host socket mounts.

To stop services while retaining database volumes:

~~~text
docker compose -f auth-service/docker-compose.yml down
docker compose -f catalog-service/docker-compose.yml down
docker compose -f inventory-service/docker-compose.yml down
docker compose -f order-service/docker-compose.yml down
docker compose -f cart-service/docker-compose.yml down
docker compose -f customer-service/docker-compose.yml down
docker compose -f payment-service/docker-compose.yml down
docker compose -f shipping-service/docker-compose.yml down
~~~

Add -v only when intentionally deleting local database data.

## Colima

Colima is optional on macOS. If it is installed, the minimum setup is:

~~~text
colima start
docker info
~~~

If more than one Docker context is installed, select Colima with
docker context use colima. Do this only for the local shell; no repository
file or application setting depends on a Colima socket. Testcontainers detects
the active Docker API and the repository's test-only fallback checks optional
Unix sockets only on Unix-like hosts.

## Service Ports

| Service | Host URL | Default database host port |
| --- | --- | ---: |
| Catalog | http://localhost:8081 | 5432 |
| Inventory | http://localhost:8082 | 5433 |
| Order | http://localhost:8083 | 5435 |
| Cart | http://localhost:8084 | 5436 |
| Auth | http://localhost:8085 | 5434 |
| Customer | http://localhost:8086 | 5437 |
| Payment | http://localhost:8087 | 5438 |
| Shipping | http://localhost:8088 | 5439 |
| Admin UI | http://localhost:3000 | — |
| Storefront | http://localhost:3001 | — |

Every application port is configurable through the service's environment
configuration. The Compose files publish the documented defaults.

## Environment Variables

Prefer .env files over manually exporting many variables. The tracked example
files are the safe starting points:

- Root .env.example: shared Compose and seed values.
- <service>/.env.example: host execution and service-specific values.
- ecommerce-admin-ui/.env.example and ecommerce-storefront/.env.example:
  browser API/rewrite URLs.

For a variable that must be set interactively, the syntax is:

| Shell | Example |
| --- | --- |
| macOS/Linux | export FOO=value |
| PowerShell | $env:FOO="value" |
| CMD | set FOO=value |

The normal Compose workflow does not require these assignments. Internal service
tokens and local passwords in examples are development-only placeholders, not
production secrets.

## Development Seed Data

The portable entry point is python dev-seed/scripts/seed.py or npm run seed.
The optional dev-seed/seed.sh wrapper is a Unix convenience only; Windows
developers should invoke the Python entry point directly. Available commands:

~~~text
npm run seed
npm run seed:validate
npm run seed:dry-run
npm run seed:refresh-images
npm run seed:reset
~~~

The dataset is synthetic general e-commerce demo data: 10 top-level
categories, 29 local category images (all roots plus partial child coverage), 37 products, 111 variants/SKUs, local product galleries, three
inventory locations, 10 fake customers, and API-created cart/order/payment/
fulfillment/shipment scenarios. It uses Meesho only as a public marketplace
variety reference and does not copy its branding, UI, identifiers, customer
data, or hosted images. See [dev-seed/README.md](dev-seed/README.md) for the
full API-first seed contract, actual counts, safety rules, and limitations.

seed:reset removes only the eight named local Compose volumes and requires
SEED_ENV=development. It never targets production URLs.

## Testing

Backend tests run per service because there is no Maven aggregator:

~~~text
mvn -f auth-service/pom.xml clean test
mvn -f catalog-service/pom.xml clean test
mvn -f inventory-service/pom.xml clean test
mvn -f order-service/pom.xml clean test
mvn -f cart-service/pom.xml clean test
mvn -f customer-service/pom.xml clean test
mvn -f payment-service/pom.xml clean test
mvn -f shipping-service/pom.xml clean test
~~~

The integration suites use Testcontainers PostgreSQL. They are not disabled or
silently replaced with an in-memory database. Docker Desktop, Colima, and Linux
Docker are detected through the Docker API; explicit DOCKER_HOST remains
authoritative when a developer has configured one.

Frontend checks:

~~~text
npm --prefix ecommerce-admin-ui run lint
npm --prefix ecommerce-admin-ui test
npm --prefix ecommerce-admin-ui run build
npm --prefix ecommerce-storefront run lint
npm --prefix ecommerce-storefront test
npm --prefix ecommerce-storefront run build
~~~

## Admin UI

The Admin UI runs on port 3000 and reads browser-safe service URLs from
ecommerce-admin-ui/.env.local. It does not receive backend service secrets.
Open http://localhost:3000 after Auth is seeded.

## Storefront

The Storefront runs on port 3001. Its Next.js rewrites keep browser calls
same-origin; the backend origins are configured in
ecommerce-storefront/.env.local. Open http://localhost:3001 after the backend
services are healthy.

## API Documentation

Current service names, ports, authentication requirements, and endpoint contracts
are in [docs/api-contracts.md](docs/api-contracts.md). Each service also exposes
Swagger UI at http://localhost:<port>/swagger-ui.html and OpenAPI JSON at
http://localhost:<port>/v3/api-docs.

## Troubleshooting

See [docs/troubleshooting.md](docs/troubleshooting.md) for Docker availability,
port conflicts, Testcontainers, PostgreSQL, service health, JWT/JWKS, CORS,
Windows Docker Desktop, macOS Colima, Linux Docker, and tool-version issues.

## Production Deployment

The local Compose files and seed data are development tooling. Production must
provide managed PostgreSQL, cloud service URLs, TLS, managed secrets, persistent
JWT signing keys, restricted CORS origins, real gateway/carrier credentials, and
observability. Never deploy the example passwords, sandbox webhook secrets,
localhost URLs, or development seed data.
