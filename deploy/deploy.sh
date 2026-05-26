#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="${ENV_FILE:-.env.prod}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
DEPLOY_SHA="${DEPLOY_SHA:-}"
LOCK_DIR="$ROOT_DIR/.deploy.lock"
IMAGE_TAG="${DEPLOY_SHA:-latest}"
export IMAGE_TAG

cd "$ROOT_DIR"

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    printf 'Another deployment is currently running in %s.\n' "$ROOT_DIR" >&2
    exit 1
fi
trap 'rmdir "$LOCK_DIR" 2>/dev/null || true' EXIT INT TERM

if [ ! -f "$ENV_FILE" ]; then
    printf 'Missing %s on the server. Configure production secrets before deployment.\n' "$ENV_FILE" >&2
    exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
    printf 'Tracked files have local changes on the server; refusing to overwrite them.\n' >&2
    exit 1
fi

printf 'Fetching origin/%s.\n' "$DEPLOY_BRANCH"
git fetch --prune origin "refs/heads/$DEPLOY_BRANCH:refs/remotes/origin/$DEPLOY_BRANCH"

TARGET_REF="origin/$DEPLOY_BRANCH"
if [ -n "$DEPLOY_SHA" ]; then
    TARGET_REF="$DEPLOY_SHA"
    if ! git merge-base --is-ancestor "$TARGET_REF" "origin/$DEPLOY_BRANCH"; then
        printf 'Commit %s is not contained in origin/%s; deployment refused.\n' "$TARGET_REF" "$DEPLOY_BRANCH" >&2
        exit 1
    fi
fi

printf 'Deploying commit %s.\n' "$(git rev-parse --short "$TARGET_REF")"
git checkout --detach "$TARGET_REF"

DOMAIN="$(sed -n 's/^DOMAIN=//p' "$ENV_FILE" | tail -n 1 | tr -d '\r')"

certificate_exists() {
    docker compose --env-file "$ENV_FILE" run --rm --no-deps --entrypoint /bin/sh certbot -c \
        "test -f /etc/letsencrypt/live/$DOMAIN/fullchain.pem"
}

docker compose --env-file "$ENV_FILE" config --quiet
if [ -z "$DOMAIN" ] || ! certificate_exists; then
    printf 'No TLS certificate found for DOMAIN=%s. Run ./deploy/init-letsencrypt.sh once on EC2 first.\n' "$DOMAIN" >&2
    exit 1
fi

docker compose --env-file "$ENV_FILE" pull nginx backend postgres certbot
docker compose --env-file "$ENV_FILE" up -d --wait postgres
docker compose --env-file "$ENV_FILE" exec -T postgres /bin/sh -c \
    'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
    < ./deploy/migrations/V20260527__access_log_error_details.sql
docker compose --env-file "$ENV_FILE" up -d --remove-orphans
docker compose --env-file "$ENV_FILE" ps

printf 'Deployment finished for commit %s.\n' "$(git rev-parse --short HEAD)"
