# Deployment profiles

This repository keeps two useful runtime profiles:

| Profile | Use | Database topology | Public ports |
| --- | --- | --- | --- |
| Independent service Compose files | Work on one service at a time | One Postgres container per service | Service and local database ports are published for development |
| `single-vm` | Full-stack integration, staging, or a low-traffic single VM | One Postgres server with eight isolated databases and roles | Only the reverse proxy is published |

The application architecture remains eight independently built Spring Boot
services in both profiles. The single-VM profile changes placement, not domain
ownership.

## Single VM

See [single-vm/README.md](single-vm/README.md). The short form is:

```text
cp deploy/single-vm/.env.example deploy/single-vm/.env
# replace every change-me value
docker compose --env-file deploy/single-vm/.env \
  -f deploy/single-vm/compose.yml config
docker compose --env-file deploy/single-vm/.env \
  -f deploy/single-vm/compose.yml up -d --build
```

If the Docker Compose plugin is not installed, `docker-compose` can be used in
the same commands. The `.env.example` file is intentionally safe to inspect,
but it is not suitable for a public deployment until its values are replaced.

## Future cloud placement

The same service images can be placed on a managed container platform or
Kubernetes. Use managed PostgreSQL, object storage for catalog media, the
platform load balancer for the proxy boundary, and the platform secret manager.
Those provider mappings are intentionally documented only; provider-specific
deployment manifests are not part of this repository yet.
