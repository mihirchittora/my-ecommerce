# Auth Service

## Purpose

`auth-service` is the standalone identity, authentication, and authorization
service for `my-ecommerce`. It runs on port `8085` and owns the `auth_db` database.

## Responsibilities

- User registration, password hashing, login, account locking, and password changes.
- Customer and staff roles, permissions, and role assignments.
- RS256 JWT access-token signing and public JWKS publication. Access tokens include the Auth-owned email and display-name claims used by Customer Service for profile-copy synchronization.
- Opaque refresh-token storage, rotation, replay detection, and logout revocation.
- Authentication audit events and development-only initial-admin bootstrap.

## Non-responsibilities

Auth does not own products, categories, SKUs, inventory, orders, frontend sessions,
Catalog tables, or Inventory tables. It never imports Catalog or Inventory entities
and never connects to their databases.

## Overall Architecture

```text
ecommerce-admin-ui ──register/login/refresh──> auth-service :8085
                                             │
                                signed JWT + public JWKS
                                  ┌──────────┴──────────┐
                                  ▼                     ▼
                         Catalog :8081          Inventory :8082
                         catalog_db              inventory_db
```

Auth owns the private signing key. Catalog and Inventory use Spring Security's
resource-server support and the public JWKS URI. They validate signatures, issuer,
audience, expiry, and permissions locally; they do not make a synchronous Auth
database request per API call.

## Why Auth is a Separate Service

Identity data has a different lifecycle and security boundary from product and
stock data. Separating it prevents domain services from storing passwords or
duplicating role logic, allows independent deployment, and gives all APIs one
consistent token and permission model. Each service still owns its own database.

## Authentication vs Authorization

Authentication establishes who the subject is. Authorization evaluates the
permission authorities in that subject's access token. Roles are only aggregates
for assigning permissions and for UI display; API code checks permission authorities
such as `PRODUCT_UPDATE` and `INVENTORY_ADJUST`.

## User Model

The `users` table has a UUID primary key, normalized case-insensitive email,
`password_hash`, first and last names, `UserStatus`, verification state, failed
login count, temporary lock expiry, timestamps, and last-login time. Email is not
the primary key. Password hashes and refresh-token values are never returned by an
API.

Statuses are `ACTIVE`, `INACTIVE`, `LOCKED`, and `PENDING`. Only ACTIVE users can
authenticate. Five failed attempts lock an account for fifteen minutes by default;
both values are configurable.

## Customer vs Staff

Public registration always creates a `CUSTOMER` and ignores role-like fields because
the public request does not contain them. Staff and admin users use the same `users`
table and are assigned roles through protected admin APIs or the development bootstrap.
The configured bootstrap administrator receives every currently seeded application
role, and assigning `SUPER_ADMIN` through the protected admin API applies the same
role invariant.

## Roles

The initial migrations seed the roles currently needed by Catalog, Inventory, and
read-only Cart support:

- `CUSTOMER`
- `CATALOG_ADMIN`
- `INVENTORY_ADMIN`
- `SUPER_ADMIN`

Future order, support, warehouse, and finance roles can be added by a later
migration when those domains are implemented; they are intentionally not active
seed data today.

## Permissions

Catalog permissions include `CATALOG_READ`, product and category CRUD, and product
image upload/delete. Inventory permissions include read, receive, adjust, transfer,
reserve, confirm, release, reconcile, location management, and unit read. Cart
support uses `CART_READ`. Payment operations use `PAYMENT_READ`,
`PAYMENT_REFUND`, and `PAYMENT_RETRY`. The auth administration permissions are
`USER_READ`, `USER_CREATE`, `USER_UPDATE`, `USER_ROLE_ASSIGN`, `ROLE_READ`,
`PERMISSION_READ`, and `ROLE_PERMISSION_UPDATE`.

## Role-Permission Model

`role_permissions` is a unique many-to-many join table. The migration seeds the
default mappings once with conflict-safe inserts. `SUPER_ADMIN` receives all
currently seeded permissions, including domain permissions added by later
migrations. Role editing, when used, replaces a role's permission set through a
protected API.

## User-Role Model

`user_roles` is a unique many-to-many join table. A user can have several roles and
the access token contains the union of permissions from those roles. `SUPER_ADMIN`
is kept attached to every currently seeded application role; removing another role
from a super-admin is rejected while `SUPER_ADMIN` remains assigned.

## Permission Catalog

The authoritative seed starts at `src/main/resources/db/migration/V2__seed_roles_and_permissions.sql`;
`V4__add_cart_read_permission.sql` adds the Cart support permission to existing
Auth databases. The active services use `PRODUCT_*`, `CATEGORY_*`,
`PRODUCT_IMAGE_*`, `INVENTORY_*`, and `CART_READ` permissions plus the
security-administration permissions above. The migration also reserves
`ORDER_READ`, `ORDER_CREATE`, `ORDER_UPDATE`,
`ORDER_CANCEL`, `CUSTOMER_READ`, and `CUSTOMER_UPDATE` as future-domain metadata;
they may be unused by a particular downstream service, but they are included in
the system-managed SUPER_ADMIN permission set.
`V5__grant_all_roles_to_super_admin_users.sql` backfills every seeded application
role onto existing users that already carry `SUPER_ADMIN`; `V6__grant_all_permissions_to_super_admin.sql`
backfills every seeded permission, including Customer and Order capabilities.
`V8__add_payment_permissions_and_roles.sql` adds the payment permission catalog,
the `PAYMENT_ADMIN`, `PAYMENT_OPERATIONS`, and `PAYMENT_READONLY` roles, and
backfills payment access for existing `SUPER_ADMIN` users.

## Registration Flow

```text
POST /api/v1/auth/register
  → normalize email
  → validate password and names
  → bcrypt hash password
  → assign CUSTOMER only
  → write USER_REGISTERED audit event
```

Example request:

```json
{"email":"customer@example.com","password":"StrongPassword123!","firstName":"John","lastName":"Doe"}
```

## Login Flow

```text
Client → POST /api/v1/auth/login → normalize email
      → load user and roles/permissions
      → verify bcrypt password and account state
      → sign RS256 JWT and create hashed refresh session
      → return accessToken + refreshToken
```

Invalid credentials use the generic message `Invalid email or password.` so the API
does not disclose whether an email exists.

## Access Token

Access tokens are JWTs with a configurable fifteen-minute default lifetime. They are
bearer tokens for API calls and expire naturally; there is no global access-token
blacklist in this initial implementation.

## JWT Claims

Tokens contain `sub`, `iss`, `aud`, `iat`, `exp`, `jti`, `roles`, and `permissions`.
They contain no password, password hash, refresh token, authorization header, or
unnecessary personal data. Permission values map directly to Spring authorities;
role values are also exposed as `ROLE_<name>` authorities for UI or future policy use.

## Signing Keys

Auth loads a PKCS#8 RSA private key from `JWT_PRIVATE_KEY_PATH` and an X.509 public
key from `JWT_PUBLIC_KEY_PATH`. If only the private key is supplied, the public key
is derived when possible. With neither path configured, a new ephemeral 2048-bit
key is generated for local development only, so restart invalidates development
tokens. Private keys are not committed to the repository.

## JWKS

`GET /.well-known/jwks.json` returns the public RSA JWK with the stable `kid` from
`JWT_KEY_ID`. Catalog and Inventory use `AUTH_JWK_SET_URI` and Spring's built-in
JWKS caching behavior.

## Refresh Tokens

Refresh tokens are random opaque values. Only their SHA-256 hash is stored in
`refresh_tokens`, together with user, family, issue, expiry, revocation, and
replacement metadata.

## Refresh Token Rotation

Each successful refresh revokes token A and issues token B in the same family. If a
revoked token is presented again, the service records `TOKEN_REUSE_DETECTED`, revokes
the remaining family, and returns `401`. Refresh tokens are also revoked for the
user after a successful password change.

## Logout

`POST /api/v1/auth/logout` requires the current access token and the refresh token
in the request body. It revokes that refresh session. Access tokens already issued
remain valid until their short expiry.

## Password Change

`POST /api/v1/auth/change-password` verifies the current password, bcrypt-hashes the
new password, revokes every refresh session for the user, and writes an audit event.
Passwords are never logged.

## Account Locking

The defaults are five failed attempts and a fifteen-minute lock. The response remains
generic for locked, inactive, missing, and bad-password cases. `AUTH_MAX_FAILED_ATTEMPTS`
and `AUTH_LOCK_DURATION` configure the policy.

## RBAC

Roles group permissions. Public registration can never select roles. User role
assignment and role-permission replacement require explicit security-management
permissions, so a caller cannot grant permissions to itself without already holding
the protected administration authority.

## Permission-Based Authorization

Protected endpoints use `hasAuthority("PRODUCT_UPDATE")`-style checks rather than
scattering role-name comparisons through controllers. Catalog and Inventory extract
the `permissions` JWT claim using a small Spring Security converter.

## Catalog Security

GET product/category browsing and image-file reads remain public. Product create,
update, delete; category create, update, delete; image upload; and image delete
require their corresponding permissions. The internal SKU lookup requires the
Inventory service secret header and is not authorized with a human
`INVENTORY_READ` permission.

## Inventory Security

All Inventory APIs require a bearer token. Receive, adjust, transfer, reservation,
confirmation, release, reconciliation, aggregate read, unit read, and location
management each map to the corresponding `INVENTORY_*` permission.

## Internal APIs

Inventory calls `GET /internal/catalog/skus/{sku}` over HTTP and never queries the
Catalog database. The endpoint is not anonymous. Configure the same high-entropy
secret as `CATALOG_SERVICE_TOKEN` on Inventory and `INVENTORY_SERVICE_TOKEN` on
Catalog; Inventory sends it as `X-Inventory-Service-Token`. This simple service
authentication mechanism is separate from user JWTs and human permissions.

## Service-to-Service Security

User-to-service calls use user JWTs and permissions. Service-to-service calls use a
dedicated service credential rather than forwarding a user's credentials. The
current Catalog SKU client uses a shared secret header; a future client-credentials
or service-JWT flow can replace it without changing Catalog's database boundary.

## CORS

Allowed origins default to `http://localhost:3000,http://localhost:3001` and are
configured with `ALLOWED_ORIGINS`. No wildcard origin is configured. Production
deployments should set the exact frontend origins and use TLS.

## CSRF

The APIs are stateless bearer-token APIs and do not use browser authentication
cookies, so CSRF protection is disabled for these API filter chains. If a future
browser session uses cookies, it must use HttpOnly/Secure/SameSite cookies and
enable an appropriate CSRF token repository rather than reusing this configuration.

## Database Schema

Flyway V1 creates `users`, `roles`, `permissions`, `user_roles`, `role_permissions`,
`refresh_tokens`, and `auth_audit_events`. Case-insensitive email uniqueness is a
PostgreSQL `LOWER(email)` unique index. Foreign keys and composite primary keys
enforce relationship uniqueness.

## Flyway Migrations

`V1__init_auth.sql` creates the schema, `V2__seed_roles_and_permissions.sql`
adds deterministic roles and mappings, and later migrations add domain permissions
and the super-admin role/permission invariants with `ON CONFLICT DO NOTHING`.
Hibernate is set to `ddl-auto=validate`; applied migrations are never rewritten.

## Audit Events

The service records registration, successful and failed login, logout, password
change, role changes, account lock/disable, token refresh, and refresh-token reuse.
Metadata is limited to operational values such as a short email hash or role name;
passwords, bearer tokens, refresh tokens, and authorization headers are never stored.

## API Reference

Public:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
GET  /.well-known/jwks.json
```

Authenticated:

```text
POST /api/v1/auth/logout
GET  /api/v1/auth/me
POST /api/v1/auth/change-password
```

Security administration:

```text
POST   /api/v1/users
GET    /api/v1/users?scope=ALL|SERVICE
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
PATCH  /api/v1/users/{id}/status
POST   /api/v1/users/{id}/roles          {"roleId":"..."}
POST   /api/v1/users/{id}/roles/{roleId} (compatibility form)
DELETE /api/v1/users/{id}/roles/{roleId}
GET    /api/v1/roles
POST   /api/v1/roles
GET    /api/v1/permissions
PUT    /api/v1/roles/{id}/permissions
```

## Swagger

Swagger UI is at [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
and OpenAPI JSON is at `/v3/api-docs`. The bearer scheme is available for protected
operations.

## Environment Variables

```text
AUTH_SERVER_PORT=8085
AUTH_DB_URL=jdbc:postgresql://localhost:5434/auth_db
AUTH_DB_USERNAME=postgres
AUTH_DB_PASSWORD=postgres
AUTH_ISSUER=http://localhost:8085
AUTH_AUDIENCE=ecommerce-api
JWT_PRIVATE_KEY_PATH=/run/secrets/auth-private.pem
JWT_PUBLIC_KEY_PATH=/run/secrets/auth-public.pem
JWT_KEY_ID=auth-key-1
INITIAL_ADMIN_EMAIL=mihirchittora6@gmail.com
INITIAL_ADMIN_PASSWORD=123456
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
```

See `.env.example` for the complete local list. Bootstrap values are development-
only inputs; do not commit them or use them as a production secret-management plan.

## Docker

`docker-compose.yml` starts only Auth PostgreSQL on host port 5434 by default and
can start the Auth service after building the jar. Catalog and Inventory retain
their own compose files and databases.

```bash
mvn -s /tmp/maven-clean-settings.xml -gs /tmp/maven-clean-settings.xml package
docker compose up --build
```

## Colima

Testcontainers works with Docker Desktop or Colima. The existing repository tests
detect `~/.colima/default/docker.sock` and `~/.docker/run/docker.sock`. If explicit
configuration is needed, set `DOCKER_HOST` and, for containers that need a mounted
socket, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` and `TESTCONTAINERS_HOST_OVERRIDE`.
Do not assume `/var/run/docker.sock` on macOS.

## Local Development

```bash
cd auth-service
docker compose up -d auth-db
mvn -s /tmp/maven-clean-settings.xml -gs /tmp/maven-clean-settings.xml spring-boot:run
```

Health: `http://localhost:8085/actuator/health`. The Auth, Catalog, and Inventory
databases can be started independently; startup order is only needed when making
cross-service calls. For local admin access set `INITIAL_ADMIN_EMAIL` and
`INITIAL_ADMIN_PASSWORD` before the first Auth startup.

## Maven

The developer environment may point Maven at a corporate Artifactory. This project
does not modify global settings. To force public Maven Central use:

```bash
mvn -s /tmp/maven-clean-settings.xml -gs /tmp/maven-clean-settings.xml clean test
```

## Tests

The Auth integration suite uses Testcontainers PostgreSQL and MockMvc. It covers
registration, duplicate normalized email, password validation, login and generic
failure, account locking, refresh rotation and reuse detection, logout,
password-change revocation, and permission-bearing JWT issuance. Catalog
and Inventory keep their domain integration tests; live smoke checks cover their
real resource-server filters and 401/403 behavior.
their legacy domain fixtures use a test-only `app.security.enabled=false` property
because those tests are intentionally focused on persistence/business behavior.

## Payment operations permissions

Migration `V8__add_payment_permissions_and_roles.sql` seeds the payment
operations capability used by Payment Service and the Admin UI:

| Permission | Meaning |
| --- | --- |
| `PAYMENT_READ` | View payment state, attempts, refunds, and safe provider references. |
| `PAYMENT_REFUND` | Issue full or partial refunds through the configured gateway. |
| `PAYMENT_RETRY` | Start a new provider attempt for a failed payment. |

The seeded roles are `PAYMENT_ADMIN` (all three permissions),
`PAYMENT_OPERATIONS` (read, refund, and retry), and `PAYMENT_READONLY` (read
only). `SUPER_ADMIN` is system-managed and receives all three permissions. The
Payment Service independently enforces these authorities on its internal admin
endpoints; frontend visibility is not the security boundary.

## Security Testing

Run each suite from its module directory:

```bash
mvn -s /tmp/maven-clean-settings.xml -gs /tmp/maven-clean-settings.xml clean test
```

The resource-server configuration rejects missing, expired, wrongly signed, wrong-
issuer, and wrong-audience tokens. A full running end-to-end check should register
or bootstrap a user, log in through Auth, call Catalog with a catalog permission,
call Inventory with an inventory permission, and verify that the wrong permission
returns `403`.

## Troubleshooting

- `401` means the bearer token is absent or failed signature/issuer/audience/expiry validation.
- `403` means the token is valid but lacks the endpoint permission.
- If Catalog/Inventory cannot fetch JWKS, confirm Auth is running and `AUTH_JWK_SET_URI` is reachable.
- If refresh returns `401` after a successful rotation, use the newest refresh token; reuse of an old token intentionally revokes its family.
- If local tokens stop working after an Auth restart, configure persistent PEM key paths; the no-path development fallback is ephemeral.
- If Testcontainers cannot connect, inspect `DOCKER_HOST`, Colima context, and the three Testcontainers variables before changing tests.

## Production Considerations

Use TLS, managed PostgreSQL credentials, a real secret manager, persistent RSA keys,
strict CORS origins, database backups, rate limiting at the edge, monitoring, and
centralized audit retention. Use a dedicated service identity for Catalog SKU calls;
do not share a human admin credential.

## Key Rotation

Generate a new RSA key pair and new `kid`, publish both old and new public keys in
JWKS during the overlap window, sign new tokens with the new key, wait for old
access tokens to expire, then retire the old JWK. The current single-key loader is
ready for the stable `kid` contract; multi-key overlap requires extending the key
loader to read a configured key set before production rotation.

## Token Expiration

Access tokens default to fifteen minutes. Refresh tokens default to thirty days and
are stored only as hashes. Logout and password changes revoke refresh sessions;
access tokens expire naturally.

## Secret Management

Never put private keys, initial passwords, DB passwords, refresh tokens, or real
service tokens in Git. Mount key files read-only or inject them through the runtime
secret manager. `.env.example` contains placeholders only.

## Rate Limiting

The service deliberately keeps infrastructure such as Redis out of this focused
implementation. Account locking provides a basic credential-attack control; add
edge/API-gateway rate limiting and alerting before production.

## Future Improvements

Email verification and recovery, MFA, external identity providers, a managed
client-credentials flow for service identities, multi-key JWKS rotation, and a
dedicated rate-limit store can be added without moving Auth into Catalog or Inventory.
