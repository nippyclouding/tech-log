# EC2 Docker HTTPS Deployment

This deployment runs four containers on one EC2 instance:

- `nginx`: serves the React build on ports `80` and `443`, terminates TLS, and proxies application paths to Spring Boot.
- `backend`: runs Spring Boot internally on port `8080`; it is not published to the Internet.
- `postgres`: persists the application database in a Docker volume.
- `certbot`: renews the Let's Encrypt certificate using the Nginx webroot challenge.

The `nginx` and `backend` images are built in GitHub Actions and published to GitHub Container Registry (GHCR). EC2 only pulls and runs images; it does not run Node or Gradle builds during deployment.

## Prerequisites

1. Point an A record such as `blog.example.com` to the EC2 Elastic IP.
2. In the EC2 security group, allow inbound TCP `80` and `443`. Keep database and backend ports closed.
3. Install Docker Engine and the Docker Compose plugin on EC2.
4. Configure the GitHub OAuth callback URL as `https://blog.example.com/login/oauth2/code/github`.
5. Authenticate Docker on EC2 to GHCR if the published packages are private:

```sh
printf '%s' '<GITHUB_PAT_WITH_READ_PACKAGES>' | docker login ghcr.io -u '<GITHUB_USERNAME>' --password-stdin
```

Use a GitHub classic personal access token with `read:packages` for the EC2 pull-only login. This token is stored by Docker on EC2 and is not added to `.env.prod`.

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

For the first deployment, the `latest` application images must already exist in GHCR. Push to `main` once to let the workflow publish them. Keep the repository Actions variable `DEPLOY_ENABLED` unset until EC2 and the initial certificate are prepared; the deploy job will be skipped during this bootstrap push.

## Subsequent Updates

When a certificate already exists, redeploy the application manually with:

```sh
./deploy/deploy.sh
```

Certificates and renewal state are stored under `data/certbot/` on the EC2 instance and are excluded from Git.

## GitHub Actions Deployment

The workflow in `.github/workflows/deploy.yml` runs the frontend checks and backend tests on each push to `main`. If verification succeeds, it builds the `nginx` and `backend` Docker images in GitHub Actions, pushes both a commit-SHA tag and `latest` tag to GHCR, then connects to EC2 over SSH. EC2 invokes `deploy/deploy.sh` and pulls the exact commit-SHA images.

The SSH deployment step checks out the target commit before invoking `deploy/deploy.sh`. This ensures that a deployment which changes the deployment script or introduces a database migration executes the script and migrations from the same commit as the new application image.

Before enabling the workflow:

1. Commit and push the deployment files, then clone the repository once on EC2, for example under `/home/ubuntu/tech-log`.
2. Create `.env.prod` only on EC2 and complete the first TLS deployment with `./deploy/init-letsencrypt.sh`.
3. Allow the GitHub Actions SSH public key in `~/.ssh/authorized_keys` for the EC2 deploy user.
4. If this repository is private, allow EC2 itself to fetch it by configuring a read-only GitHub deploy key or other Git authentication.
5. If the GHCR packages are private, log Docker on EC2 into `ghcr.io` with a pull-only token as shown above.
6. In the GitHub repository, create a `production` Environment and add these environment secrets:

| Secret | Value |
| --- | --- |
| `EC2_HOST` | EC2 Elastic IP or deployment DNS hostname |
| `EC2_SSH_USER` | SSH user, such as `ubuntu` |
| `EC2_SSH_PORT` | SSH port, usually `22`; may be omitted |
| `EC2_SSH_PRIVATE_KEY` | Private key whose public key is authorized on EC2 |
| `EC2_SSH_KNOWN_HOSTS` | Verified `known_hosts` line for EC2 |
| `EC2_DEPLOY_PATH` | Absolute server repository path, such as `/home/ubuntu/tech-log` |

7. After initial HTTPS setup succeeds on EC2, add a repository Actions variable, not a secret, under `Settings` > `Secrets and variables` > `Actions` > `Variables`:

| Variable | Value |
| --- | --- |
| `DEPLOY_ENABLED` | `true` |

Generate the `known_hosts` content from a trusted machine and confirm its fingerprint before storing it:

```sh
ssh-keyscan -H your-domain.example.com
```

`DEPLOY_ENABLED` must not be an environment-level variable: the deploy job checks it before the `production` environment is attached to a runner. After the repository variable is configured, `main` deployments run automatically after a push. You can also run `CI and Deploy` from the GitHub Actions tab using `Run workflow` on the `main` branch.

The workflow never sends `.env.prod`, certificates, database data, or uploaded images through GitHub Actions. Those values remain on EC2.

For existing PostgreSQL volumes, `deploy/deploy.sh` starts PostgreSQL first and applies the idempotent SQL files in `deploy/migrations` before starting a backend image that validates the schema. This is required when an application update adds persisted fields such as administrator-visible error details on access logs.

## Scaling the EC2 Instance

You can change an EBS-backed EC2 instance from `t3.small` to `t3.medium` later without migrating this deployment. Docker volumes, PostgreSQL data, issued certificates, `.env.prod`, and uploaded images remain on the attached EBS volume across a normal stop/start instance type change.

Before resizing, take an EBS snapshot or database backup. Then:

1. Stop the EC2 instance in the AWS console.
2. Choose `Actions` > `Instance settings` > `Change instance type`.
3. Select `t3.medium`.
4. Start the instance.
5. Confirm containers and HTTPS:

```sh
docker compose --env-file .env.prod ps
curl -I https://<your-domain>
```

If an Elastic IP is associated with the instance, the domain continues to resolve to the same address. Do not terminate and recreate the instance as a resizing substitute unless you have deliberately restored its data volume and configuration.

## Operations

```sh
docker compose --env-file .env.prod ps
docker compose --env-file .env.prod logs -f nginx backend certbot
docker compose --env-file .env.prod run --rm --entrypoint certbot certbot renew --dry-run
```

The Certbot service checks renewal twice a day. Nginx reloads periodically to begin serving any renewed certificate.

If HTTPS is active but an automated deployment reports `No TLS certificate found`, do not issue a replacement certificate immediately. Certbot may have created `data/certbot/conf/live/<DOMAIN>` with permissions readable inside the container but not directly by the `ubuntu` host user. The deployment scripts validate existing certificates from the Certbot container so that the certificate can remain protected on the host.

The deployment scripts also strip carriage returns while reading `DOMAIN` and `CERTBOT_EMAIL`; an `.env.prod` file edited with CRLF line endings must not turn an existing certificate path into a false missing-certificate failure.
