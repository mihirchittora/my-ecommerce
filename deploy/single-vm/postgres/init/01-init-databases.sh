#!/bin/bash
set -Eeuo pipefail

# This script runs only on first initialization of the named PostgreSQL volume.
# It creates roles and databases; Flyway remains responsible for application
# schemas and tables.

psql_admin=(psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --no-password -v ON_ERROR_STOP=1)

create_role() {
  local role_name="$1"
  local password="$2"
  "${psql_admin[@]}" -v role_password="$password" <<SQL
SELECT format('CREATE ROLE ${role_name} LOGIN PASSWORD %L', :'role_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${role_name}')\gexec
SQL
}

create_database() {
  local database_name="$1"
  local role_name="$2"
  "${psql_admin[@]}" <<SQL
SELECT 'CREATE DATABASE ${database_name} OWNER ${role_name}'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${database_name}')\gexec
REVOKE ALL ON DATABASE ${database_name} FROM PUBLIC;
GRANT CONNECT ON DATABASE ${database_name} TO ${role_name};
SQL

  psql --username "$POSTGRES_USER" --dbname "$database_name" --no-password -v ON_ERROR_STOP=1 <<SQL
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO ${role_name};
SQL
}

create_role catalog_user "$CATALOG_DB_PASSWORD"
create_role inventory_user "$INVENTORY_DB_PASSWORD"
create_role order_user "$ORDER_DB_PASSWORD"
create_role cart_user "$CART_DB_PASSWORD"
create_role auth_user "$AUTH_DB_PASSWORD"
create_role customer_user "$CUSTOMER_DB_PASSWORD"
create_role payment_user "$PAYMENT_DB_PASSWORD"
create_role shipping_user "$SHIPPING_DB_PASSWORD"

create_database catalog_db catalog_user
create_database inventory_db inventory_user
create_database order_db order_user
create_database cart_db cart_user
create_database auth_db auth_user
create_database customer_db customer_user
create_database payment_db payment_user
create_database shipping_db shipping_user

echo "Initialized eight isolated application databases and roles."
