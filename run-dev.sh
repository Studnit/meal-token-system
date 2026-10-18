#!/usr/bin/env bash
# DEV profile: local PostgreSQL database mealtoken_dev (user postgres / password postgres unless DEV_DB_* are set).
set -e
cd "$(dirname "$0")"
[ -f .env ] && set -a && . ./.env && set +a
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
