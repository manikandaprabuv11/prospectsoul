#!/bin/sh
# Creates the separate database Keycloak stores its own schema in. Runs only on
# first initialisation of the postgres-data volume.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	SELECT 'CREATE DATABASE ${KEYCLOAK_DB:-keycloak}'
	WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${KEYCLOAK_DB:-keycloak}')\gexec
EOSQL
