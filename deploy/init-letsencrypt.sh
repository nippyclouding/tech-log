#!/usr/bin/env sh
set -eu

ENV_FILE="${ENV_FILE:-.env.prod}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
export IMAGE_TAG

if [ ! -f "$ENV_FILE" ]; then
    printf 'Missing %s. Copy .env.prod.example and set deployment values first.\n' "$ENV_FILE" >&2
    exit 1
fi

read_value() {
    sed -n "s/^$1=//p" "$ENV_FILE" | tail -n 1 | tr -d '\r'
}

DOMAIN="$(read_value DOMAIN)"
CERTBOT_EMAIL="$(read_value CERTBOT_EMAIL)"

if [ -z "$DOMAIN" ] || [ -z "$CERTBOT_EMAIL" ]; then
    printf 'DOMAIN and CERTBOT_EMAIL must be set in %s.\n' "$ENV_FILE" >&2
    exit 1
fi

case "$DOMAIN" in
    *[!a-zA-Z0-9.-]* | .* | *.)
        printf 'DOMAIN must be a DNS hostname only, for example blog.example.com.\n' >&2
        exit 1
        ;;
esac

CERTIFICATE_PATH="./data/certbot/conf/live/$DOMAIN/fullchain.pem"

mkdir -p "./data/certbot/conf/live/$DOMAIN" "./data/certbot/www"

certificate_exists() {
    [ -f "$CERTIFICATE_PATH" ] || docker compose --env-file "$ENV_FILE" run --rm --no-deps --entrypoint /bin/sh certbot -c \
        "test -f /etc/letsencrypt/live/$DOMAIN/fullchain.pem"
}

apply_database_migrations() {
    docker compose --env-file "$ENV_FILE" up -d --wait postgres
    docker compose --env-file "$ENV_FILE" exec -T postgres /bin/sh -c \
        'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
        < ./deploy/migrations/V20260527__access_log_error_details.sql
    docker compose --env-file "$ENV_FILE" exec -T postgres /bin/sh -c \
        'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
        < ./deploy/migrations/V20260527__admins.sql
}

if certificate_exists; then
    printf 'An existing certificate was found for %s; starting the deployment.\n' "$DOMAIN"
    docker compose --env-file "$ENV_FILE" pull nginx backend postgres certbot
    apply_database_migrations
    docker compose --env-file "$ENV_FILE" up -d --remove-orphans
    exit 0
fi

printf 'Pulling application images and creating a temporary certificate for initial startup.\n'
docker compose --env-file "$ENV_FILE" pull nginx backend postgres certbot
apply_database_migrations
docker compose --env-file "$ENV_FILE" run --rm --no-deps --entrypoint /bin/sh nginx -c \
    "mkdir -p /etc/letsencrypt/live/$DOMAIN && openssl req -x509 -nodes -newkey rsa:2048 -days 1 -keyout /etc/letsencrypt/live/$DOMAIN/privkey.pem -out /etc/letsencrypt/live/$DOMAIN/fullchain.pem -subj /CN=localhost"

docker compose --env-file "$ENV_FILE" up -d nginx

printf 'Requesting a Let'\''s Encrypt certificate for %s.\n' "$DOMAIN"
docker compose --env-file "$ENV_FILE" run --rm --no-deps --entrypoint /bin/sh certbot -c \
    "rm -rf /etc/letsencrypt/live/$DOMAIN /etc/letsencrypt/archive/$DOMAIN /etc/letsencrypt/renewal/$DOMAIN.conf"
docker compose --env-file "$ENV_FILE" run --rm --no-deps --entrypoint certbot certbot \
    certonly --webroot --webroot-path /var/www/certbot \
    --domain "$DOMAIN" --email "$CERTBOT_EMAIL" \
    --rsa-key-size 4096 --agree-tos --no-eff-email

docker compose --env-file "$ENV_FILE" exec nginx nginx -s reload
docker compose --env-file "$ENV_FILE" up -d certbot

printf 'HTTPS deployment is active at https://%s.\n' "$DOMAIN"
