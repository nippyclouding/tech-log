# EC2 Docker HTTPS Deployment

This deployment runs four containers on one EC2 instance:

- `nginx`: serves the React build on ports `80` and `443`, terminates TLS, and proxies application paths to Spring Boot.
- `backend`: runs Spring Boot internally on port `8080`; it is not published to the Internet.
- `postgres`: persists the application database in a Docker volume.
- `certbot`: renews the Let's Encrypt certificate using the Nginx webroot challenge.

## Prerequisites

1. Point an A record such as `blog.example.com` to the EC2 Elastic IP.
2. In the EC2 security group, allow inbound TCP `80` and `443`. Keep database and backend ports closed.
3. Install Docker Engine and the Docker Compose plugin on EC2.
4. Configure the GitHub OAuth callback URL as `https://blog.example.com/login/oauth2/code/github`.

## First Deployment

From the project directory on EC2:

```sh
cp .env.prod.example .env.prod
vi .env.prod
chmod +x deploy/init-letsencrypt.sh
./deploy/init-letsencrypt.sh
```

Set `DOMAIN`, `CERTBOT_EMAIL`, `FRONTEND_ORIGIN=https://<DOMAIN>`, database passwords, OAuth secrets, admin password, and mail values in `.env.prod` before running the script.

The initialization script starts Nginx with a temporary certificate so the HTTP ACME challenge can be served, obtains the real certificate, reloads Nginx, and starts automatic renewal.

## Subsequent Updates

When a certificate already exists, redeploy the application manually with:

```sh
./deploy/deploy.sh
```

Certificates and renewal state are stored under `data/certbot/` on the EC2 instance and are excluded from Git.

## GitHub Actions Deployment

The workflow in `.github/workflows/deploy.yml` runs the frontend checks and backend tests on each push to `main`. If verification succeeds, it connects to EC2 over SSH and invokes `deploy/deploy.sh` with the exact pushed commit SHA.

Before enabling the workflow:

1. Commit and push the deployment files, then clone the repository once on EC2, for example under `/home/ubuntu/tech-log`.
2. Create `.env.prod` only on EC2 and complete the first TLS deployment with `./deploy/init-letsencrypt.sh`.
3. Allow the GitHub Actions SSH public key in `~/.ssh/authorized_keys` for the EC2 deploy user.
4. If this repository is private, allow EC2 itself to fetch it by configuring a read-only GitHub deploy key or other Git authentication.
5. In the GitHub repository, create a `production` Environment and add these environment secrets:

| Secret | Value |
| --- | --- |
| `EC2_HOST` | EC2 Elastic IP or deployment DNS hostname |
| `EC2_SSH_USER` | SSH user, such as `ubuntu` |
| `EC2_SSH_PORT` | SSH port, usually `22`; may be omitted |
| `EC2_SSH_PRIVATE_KEY` | Private key whose public key is authorized on EC2 |
| `EC2_SSH_KNOWN_HOSTS` | Verified `known_hosts` line for EC2 |
| `EC2_DEPLOY_PATH` | Absolute server repository path, such as `/home/ubuntu/tech-log` |

Generate the `known_hosts` content from a trusted machine and confirm its fingerprint before storing it:

```sh
ssh-keyscan -H your-domain.example.com
```

`main` deployments then run automatically after a push. You can also run `CI and Deploy` from the GitHub Actions tab using `Run workflow` on the `main` branch.

The workflow never sends `.env.prod`, certificates, database data, or uploaded images through GitHub Actions. Those values remain on EC2.

## Operations

```sh
docker compose --env-file .env.prod ps
docker compose --env-file .env.prod logs -f nginx backend certbot
docker compose --env-file .env.prod run --rm --entrypoint certbot certbot renew --dry-run
```

The Certbot service checks renewal twice a day. Nginx reloads periodically to begin serving any renewed certificate.
