#!/usr/bin/env bash
# PROD profile: refuses to start until every required setting exists. Put them in a file named .env (see .env.example).
set -e
cd "$(dirname "$0")"
[ -f .env ] && set -a && . ./.env && set +a
missing=""
for v in DB_URL DB_USERNAME DB_PASSWORD QR_SECRET ADMIN_PASSWORD; do
  [ -z "${!v}" ] && missing="$missing $v"
done
if [ -n "$missing" ]; then
  echo "Missing required settings for the prod profile:$missing"
  echo "Example:  export QR_SECRET=\$(openssl rand -hex 24)   then set the others, or put them in .env"
  exit 1
fi
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
