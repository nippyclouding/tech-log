# Tech Log 배포 및 EC2 운영 기록

## 1. 문서 목적

이 문서는 `techlog.site` 서비스의 운영 배포 구조, 현재까지 결정한 운영 방식, AWS EC2 준비 과정에서 수행한 작업, 최초 배포 및 이후 CI/CD 흐름을 기록한다.

기록 기준일은 `2026-05-27`이다. 일상 운영 점검과 데이터 보존 정책은 [`운영가이드.md`](./운영가이드.md)에 별도로 정리한다.

## 2. 확정한 운영 방안

### 2.1 인프라 구성

초기 운영은 별도의 RDS를 사용하지 않고, 하나의 EC2 인스턴스에서 애플리케이션과 PostgreSQL을 Docker Compose로 운영한다.

| 항목 | 결정 내용 |
| --- | --- |
| 도메인 | `techlog.site` |
| 도메인 관리 | 가비아 |
| 서버 | AWS EC2 |
| 인스턴스 타입 | `t3.small`로 시작 |
| 루트 EBS 볼륨 | `25 GiB`, `gp3` |
| 고정 IP | Elastic IP를 EC2에 연결하여 사용 |
| HTTPS | Nginx + Certbot + Let's Encrypt |
| 데이터베이스 | EC2 내부 PostgreSQL Docker 컨테이너 |
| 이미지 저장소 | GitHub Container Registry(GHCR) |
| 배포 자동화 | GitHub Actions가 이미지를 빌드하고 EC2는 이미지를 pull하여 실행 |

### 2.2 RDS를 사용하지 않는 이유와 책임 범위

초기 트래픽과 비용을 고려하여 PostgreSQL도 같은 EC2에서 컨테이너로 운영한다. 이 방식은 별도 RDS 비용이 없으므로 초기 비용이 낮고 구성이 단순하다.

대신 다음 책임은 운영자가 직접 관리해야 한다.

| 관리 대상 | 운영상 의미 |
| --- | --- |
| DB 백업 | EBS 스냅샷 또는 `pg_dump` 기반 정기 백업이 필요하다. |
| 장애 복구 | EC2 또는 볼륨 장애 시 DB 복구 절차를 직접 수행해야 한다. |
| 리소스 경쟁 | Spring Boot와 PostgreSQL이 한 EC2의 메모리와 CPU를 함께 사용한다. |
| 향후 확장 | 트래픽 또는 데이터 중요도가 커지면 RDS 이전을 검토한다. |

애플리케이션은 데이터베이스 접속 정보를 환경 변수로 받도록 구성되어 있으므로, 이후 RDS로 이전할 때에는 DB dump/restore와 datasource 환경 변수 변경 중심으로 분리할 수 있다.

### 2.3 EC2 크기 운영 방안

현재 Compose 구성은 Nginx, Certbot, Spring Boot, PostgreSQL을 함께 실행한다. `t3.micro`는 메모리 여유가 부족하므로 사용하지 않고 `t3.small`로 시작한다.

사용량이 증가하면 EBS-backed EC2는 다음 방식으로 `t3.medium`으로 변경할 수 있다.

1. PostgreSQL 백업 또는 EBS 스냅샷을 만든다.
2. EC2 인스턴스를 중지한다.
3. 인스턴스 타입을 `t3.medium`으로 변경한다.
4. 인스턴스를 다시 시작한다.
5. 컨테이너 상태와 HTTPS 접속을 확인한다.

Elastic IP를 유지하면 인스턴스 타입 변경 후에도 가비아 DNS의 A 레코드를 변경할 필요가 없다. EC2를 삭제하고 새로 만들면 볼륨 데이터와 서버 설정이 자동으로 유지되지 않으므로, 단순 사양 변경을 위해 인스턴스를 재생성하지 않는다.

## 3. 배포 아키텍처

### 3.1 서비스 흐름

```text
사용자 브라우저
    |
    | https://techlog.site (TCP 443)
    v
Elastic IP -> EC2
               |
               v
       Nginx 컨테이너
       - React 정적 파일 제공
       - HTTPS 종료
       - API 요청 reverse proxy
               |
               | Docker 내부 네트워크 / port 8080
               v
       Spring Boot backend 컨테이너
               |
               | Docker 내부 네트워크 / port 5432
               v
       PostgreSQL 컨테이너
```

### 3.2 Docker Compose 서비스

현재 [`docker-compose.yml`](../../docker-compose.yml)은 다음 네 개 서비스를 실행한다.

| 서비스 | 역할 | 외부 공개 여부 | 데이터 보존 |
| --- | --- | --- | --- |
| `nginx` | React 서비스, HTTPS 종료, Spring Boot proxy | `80`, `443` 공개 | 인증서 디렉터리 mount |
| `backend` | Spring Boot API 및 관리자 기능 | 공개하지 않음, Docker 내부 `8080` | 업로드 이미지를 Docker volume에 저장 |
| `postgres` | 애플리케이션 DB | 공개하지 않음, Docker 내부 `5432` | Docker volume에 DB 저장 |
| `certbot` | Let's Encrypt 인증서 발급/갱신 | Nginx webroot를 통해 검증 | 인증서 디렉터리 mount |

보안 그룹에서는 `80`, `443`만 인터넷에 열고, SSH `22`는 운영자 IP에서만 허용한다. Backend `8080`과 PostgreSQL `5432`는 외부에 공개하지 않는다.

### 3.3 영속 데이터 위치

| 데이터 | 저장 위치/볼륨 | 주의사항 |
| --- | --- | --- |
| PostgreSQL 데이터 | Docker volume `tech-log-postgres-data` | EC2 볼륨 손실 시 DB도 손실될 수 있으므로 백업 필요 |
| 업로드 이미지 | Docker volume `tech-log-images` | 백업 대상 |
| TLS 인증서 | EC2 프로젝트 경로 `data/certbot/conf/` | Git에 포함하지 않으며 백업 대상 |
| ACME webroot | EC2 프로젝트 경로 `data/certbot/www/` | Certbot 검증에 사용 |
| 운영 환경 변수 | EC2 프로젝트 경로 `.env.prod` | 비밀번호/토큰 포함, Git에 커밋 금지 |

## 4. 이미지 빌드 및 배포 방식

### 4.1 EC2에서 빌드하지 않는 구조

EC2 `t3.small`에서 React와 Spring Boot 빌드를 직접 수행하면 운영 컨테이너와 빌드 작업이 메모리를 경쟁하게 된다. 따라서 Docker 이미지 빌드는 GitHub Actions에서 수행하고, EC2는 이미 빌드된 이미지의 pull과 컨테이너 실행만 담당하도록 정했다.

사용하는 이미지:

```text
ghcr.io/nippyclouding/tech-log-nginx:<tag>
ghcr.io/nippyclouding/tech-log-backend:<tag>
postgres:16-alpine
certbot/certbot:latest
```

`tech-log-nginx` 이미지에는 React 빌드 산출물과 Nginx 설정이 포함된다. `tech-log-backend` 이미지에는 Spring Boot 애플리케이션이 포함된다.

### 4.2 GitHub Actions 흐름

[`/.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml)은 `main` 브랜치 push 또는 수동 실행 시 다음 순서로 동작한다.

1. Frontend에서 `npm ci`, `npm run lint`, `npm run build`를 수행한다.
2. Backend에서 `./gradlew test --no-daemon`을 수행한다.
3. 검증에 성공하면 Nginx와 Backend Docker 이미지를 빌드한다.
4. 각 이미지를 GHCR에 두 개 태그로 push한다.
   - 커밋 SHA 태그: 특정 배포 버전을 정확히 식별한다.
   - `latest` 태그: 최초 EC2 배포 시 사용한다.
5. Repository Actions variable `DEPLOY_ENABLED=true`가 설정된 이후에는 GitHub Actions가 `production` Environment의 secrets를 사용해 SSH로 EC2에 접속한다.
6. EC2의 [`deploy/deploy.sh`](../../deploy/deploy.sh)가 해당 커밋 SHA의 이미지를 pull하고 컨테이너를 갱신한다.

GitHub Actions의 SSH 단계는 `deploy.sh`를 호출하기 전에 먼저 대상 커밋을 checkout한다. 배포 스크립트 자체와 backend 이미지가 같은 커밋에서 함께 변경되는 경우에도 새 스크립트의 DB migration 등이 새 backend 기동 전에 실행되도록 하기 위함이다.

GHCR에는 이미 다음 패키지가 publish된 상태임을 확인했다.

```text
tech-log-nginx
tech-log-backend
```

### 4.3 최초 배포와 이후 배포의 차이

최초 배포에서는 아직 실제 TLS 인증서가 없으므로 EC2에서 [`deploy/init-letsencrypt.sh`](../../deploy/init-letsencrypt.sh)를 한 번 실행해야 한다.

```text
최초 배포:
EC2에서 latest 이미지 pull
-> 임시 인증서로 Nginx 시작
-> Certbot이 techlog.site 실제 인증서 발급
-> Nginx reload
-> Certbot 자동 갱신 컨테이너 시작

이후 배포:
GitHub Actions 검증 및 이미지 publish
-> EC2 SSH 접속
-> 커밋 SHA 이미지 pull
-> docker compose up -d
```

초기 HTTPS 발급이 완료되기 전에는 GitHub Repository Actions variable `DEPLOY_ENABLED` 값을 설정하지 않는다. HTTPS 발급과 EC2 준비가 끝난 후 `DEPLOY_ENABLED=true`로 활성화한다.

## 5. 현재까지 진행한 작업

### 5.1 완료 확인된 작업

| 순서 | 수행한 작업 | 결과 |
| --- | --- | --- |
| 1 | 가비아에서 `techlog.site` 도메인 구매 | 도메인 확보 |
| 2 | RDS 대신 EC2 내부 PostgreSQL 컨테이너를 운영하기로 결정 | 초기 비용 중심 운영 구조 확정 |
| 3 | EC2 저장소 용량을 재검토하여, 기존 `8 GiB` 인스턴스를 삭제하고 `25 GiB gp3`로 새 EC2 생성 | Docker 이미지/DB/로그 저장 여유 확보 |
| 4 | Docker 이미지 빌드를 GitHub Actions에서 수행하고 EC2는 pull만 하도록 프로젝트 배포 설정 구성 | `t3.small` 운영 부담 축소 |
| 5 | GitHub Actions를 통해 `tech-log-nginx`, `tech-log-backend` GHCR 패키지 publish 확인 | EC2가 pull할 이미지 준비 |
| 6 | EC2에 Docker 공식 APT 저장소 등록 | Docker 설치 경로 준비 |
| 7 | EC2에 Docker Engine 및 Docker Compose Plugin 설치 후 `hello-world`까지 확인 | Docker 실행 환경 준비 완료 |
| 8 | Elastic IP를 EC2에 연결하고 가비아의 `techlog.site` A 레코드에 등록 | 고정 IP 기반 도메인 연결 구성 |
| 9 | 로컬 PC에서 `nslookup techlog.site` 실행 | `techlog.site`가 Elastic IP `3.34.203.73`으로 해석되는 것을 확인 |
| 10 | EC2의 `/home/ubuntu/tech-log`에 GitHub 저장소 clone | Compose 및 배포 스크립트 실행 위치 준비 |
| 11 | EC2에서 GHCR 로그인 후 Nginx/Backend `latest` 이미지 pull | 최초 컨테이너 실행용 이미지 다운로드 완료 |
| 12 | EC2에 `.env.prod`를 작성하고 Compose 컨테이너 기동 | PostgreSQL, Backend, Nginx 실행 확인 |
| 13 | ACME challenge `404` 원인 조사 및 Nginx 설정 수동 반영 | webroot 파일이 HTTP `200 OK`로 제공되는 것을 확인 |
| 14 | Certbot webroot 방식으로 `techlog.site` 인증서 발급 | HTTPS 적용 완료, 인증서 만료일 `2026-08-24` |
| 15 | Certbot 컨테이너 기동 및 HTTPS 접속 확인 | 자동 갱신 점검이 가능한 운영 상태 확보 |
| 16 | GitHub Actions `production` secrets와 Repository Actions variable `DEPLOY_ENABLED=true` 설정 | `main` push 자동 배포 동작 확인 |
| 17 | Gmail SMTP 앱 비밀번호 설정 후 구독 확인 메일 발송/링크 처리 확인 | 이메일 구독 발송 동작 확인 |
| 18 | 접근 로그에 request ID와 서버 오류 상세 표시 추가 | 관리자 로그 화면에서 오류 추적 가능 |
| 19 | 기존 PostgreSQL volume에 접근 로그 상세 컬럼 migration 적용 | 새 backend schema validation 통과 |
| 20 | PostgreSQL 게시글 검색 null 파라미터 쿼리 수정 후 `main` push | 배포 후 공개/관리자 게시글 목록 응답 확인 필요 |
| 21 | 관리자 게시글 편집 목록을 페이지 조회 방식으로 수정 후 `main` push | 배포 후 편집 목록 로딩 확인 필요 |
| 22 | 대괄호 등 Markdown 문자가 있는 업로드 파일명 처리 수정 후 `main` push | 배포 후 해당 이미지 재삽입/표시 확인 필요 |

### 5.2 운영 후속 작업

서비스 공개와 자동 배포, Gmail SMTP 연결은 완료되었다. 아래 항목은 운영 안정성을 높이기 위해 이어서 수행하거나 확인할 작업이다.

| 순서 | 작업 |
| --- | --- |
| 1 | 댓글 GitHub 로그인을 사용할 경우 GitHub OAuth callback 및 credentials 동작을 운영 주소에서 확인 |
| 2 | PostgreSQL `pg_dump`와 업로드 이미지의 EC2 외부 정기 백업을 구성 |
| 3 | AWS Budgets 및 Cost Anomaly Detection을 설정하여 비용 급증을 탐지 |
| 4 | schema 변경이 늘어나기 전에 Flyway 도입 여부를 결정 |

## 6. 실제 실행한 EC2 명령과 의미

### 6.1 SSH 접속

로컬 PC에서 EC2에 접속할 때 사용하는 명령이다. 실제 키 이름과 Elastic IP는 생성한 리소스 값으로 사용한다.

```bash
chmod 400 <KEY_FILE>.pem
ssh -i <KEY_FILE>.pem ubuntu@<ELASTIC_IP>
```

| 명령 | 의미 |
| --- | --- |
| `chmod 400` | SSH 개인 키를 본인만 읽을 수 있게 제한한다. 권한이 넓으면 SSH가 키 사용을 거부할 수 있다. |
| `ssh -i ... ubuntu@...` | Ubuntu EC2 서버에 개인 키 인증으로 접속한다. |

### 6.2 Docker 설치 전 기본 패키지 준비

실행 과정에서 처음에는 `git`을 `gi`로 잘못 입력하여 `Unable to locate package gi` 오류가 발생했고, 바로 `git`으로 수정하여 정상 완료했다.

```bash
sudo apt install -y ca-certificates curl gi
# Error: Unable to locate package gi

sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

| 명령 | 수행한 내용 |
| --- | --- |
| `sudo apt install -y ca-certificates curl git` | HTTPS 저장소 접근에 필요한 인증서/curl과 소스 다운로드용 Git을 준비했다. 기존에 설치되어 있어 변경 없이 확인되었다. |
| `sudo install -m 0755 -d /etc/apt/keyrings` | APT 저장소 서명 키를 저장할 디렉터리를 생성했다. 같은 명령을 두 번 실행했지만 문제가 없다. |
| `sudo curl ... docker.asc` | Docker 공식 APT 저장소의 GPG 서명 키를 받았다. |
| `sudo chmod a+r ...` | APT가 Docker 서명 키를 읽을 수 있도록 읽기 권한을 부여했다. |

### 6.3 Docker 공식 APT 저장소 등록

다음 명령으로 Docker 공식 패키지 저장소를 EC2에 등록했다.

```bash
sudo tee /etc/apt/sources.list.d/docker.sources > /dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
```

작성 결과를 다음 명령으로 확인했다.

```bash
cat /etc/apt/sources.list.d/docker.sources
```

실제 확인된 결과:

```text
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: resolute
Components: stable
Architectures: amd64
Signed-By: /etc/apt/keyrings/docker.asc
```

`Suites: resolute`는 해당 EC2가 Ubuntu `26.04 LTS (Resolute)` 계열을 사용한다는 의미이다. `Architectures: amd64`는 x86_64 인스턴스용 Docker 패키지를 받는 설정이다.

### 6.4 Docker Engine 및 Compose 설치와 검증

다음 단계까지 실행하여 Docker 설치와 `hello-world` 컨테이너 실행이 완료되었음을 확인했다.

```bash
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo docker --version
sudo docker compose version
sudo systemctl status docker --no-pager

sudo usermod -aG docker ubuntu
exit

# EC2 재접속 후
docker --version
docker compose version
docker run --rm hello-world
```

| 명령 | 수행한 내용 |
| --- | --- |
| `sudo apt update` | 등록한 Docker 공식 저장소의 패키지 목록을 갱신했다. |
| `sudo apt install ...` | Docker Engine, CLI, 컨테이너 런타임, Buildx, Compose plugin을 설치했다. |
| `sudo docker --version` | Docker CLI가 설치되었는지 확인했다. |
| `sudo docker compose version` | 프로젝트 실행에 필요한 Compose V2 plugin 설치를 확인했다. |
| `sudo systemctl status docker --no-pager` | Docker daemon이 실행 중인지 확인했다. |
| `sudo usermod -aG docker ubuntu` | `ubuntu` 사용자가 이후 `sudo` 없이 Docker를 실행할 수 있게 Docker 그룹에 추가했다. |
| `exit` 후 재접속 | 새 그룹 권한을 현재 SSH 세션에 적용했다. |
| `docker run --rm hello-world` | Docker daemon이 이미지를 다운로드하고 컨테이너를 정상 실행할 수 있는지 최종 검증했다. |

### 6.5 저장소 clone, GHCR 로그인 및 이미지 pull

Docker 설치 후 EC2의 `/home/ubuntu/tech-log` 경로에 저장소를 clone하고, GitHub Container Registry에 로그인하여 GitHub Actions가 publish한 애플리케이션 이미지를 받았다.

```bash
cd /home/ubuntu
git clone https://github.com/nippyclouding/tech-log.git
cd tech-log

read -s CR_PAT
printf '%s' "$CR_PAT" | docker login ghcr.io -u nippyclouding --password-stdin
unset CR_PAT

docker pull ghcr.io/nippyclouding/tech-log-nginx:latest
docker pull ghcr.io/nippyclouding/tech-log-backend:latest
docker images | grep tech-log
```

Backend 이미지 pull 완료 시 실제로 확인한 출력:

```text
Digest: sha256:bed3ed9e86698d193a30274cb3604296db5d1318deb8fb894547f8d0e4dea69f
Status: Downloaded newer image for ghcr.io/nippyclouding/tech-log-backend:latest
ghcr.io/nippyclouding/tech-log-backend:latest
```

| 명령 | 수행한 내용 |
| --- | --- |
| `git clone ...` | EC2에 Compose 설정과 배포 스크립트가 포함된 프로젝트 소스를 내려받았다. |
| `docker login ghcr.io ...` | private GHCR 패키지를 pull할 수 있도록 EC2의 Docker 인증을 설정했다. |
| `docker pull ...:latest` | 최초 HTTPS 배포에서 실행할 Nginx 및 Backend 이미지를 EC2에 다운로드했다. |
| `docker images \| grep tech-log` | 받아진 애플리케이션 이미지가 로컬 Docker 이미지 목록에 있는지 확인했다. |

### 6.6 Elastic IP 및 DNS 연결 확인

Elastic IP를 EC2에 연결하고, 가비아 DNS의 `techlog.site` A 레코드가 해당 IP를 가리키도록 설정했다. 로컬 PC에서 다음 명령으로 DNS 해석 결과를 확인했다.

```bash
nslookup techlog.site
```

실제 확인 결과:

```text
Name:    techlog.site
Address: 3.34.203.73
```

따라서 `techlog.site`의 DNS는 EC2 Elastic IP로 연결된 상태이며, Certbot의 HTTP 검증을 진행할 수 있다. HTTPS 인증서 발급 동안에는 EC2 보안 그룹에서 TCP `80`이 외부에 열려 있어야 한다.

### 6.7 최초 HTTPS 발급 중 발생한 문제와 해결 기록

최초 실행 시 Let's Encrypt가 EC2에는 도달했지만 ACME challenge 파일에서 `404`를 받았다.

```text
Identifier: techlog.site
Type: unauthorized
Detail: 3.34.203.73: Invalid response from
http://techlog.site/.well-known/acme-challenge/...: 404
```

webroot mount와 테스트 파일은 정상임을 아래 과정으로 확인했다.

```bash
mkdir -p data/certbot/www/.well-known/acme-challenge
printf 'acme-ok\n' > data/certbot/www/.well-known/acme-challenge/check.txt

docker inspect tech-log-nginx --format '{{range .Mounts}}{{println .Source "->" .Destination}}{{end}}'
docker exec tech-log-nginx sh -c 'ls -l /var/www/certbot/.well-known/acme-challenge && cat /var/www/certbot/.well-known/acme-challenge/check.txt'
curl -i http://techlog.site/.well-known/acme-challenge/check.txt
```

확인 결과:

- 호스트 `data/certbot/www`는 컨테이너 `/var/www/certbot`에 정상 mount되었다.
- 컨테이너 내부에서 `check.txt`와 `acme-ok` 내용이 보였다.
- 외부 HTTP 요청은 `404 Not Found`였다.
- 이미지의 `/etc/nginx/templates/default.conf.template`에는 ACME location이 있었다.
- 그러나 `nginx -T` 출력에는 ACME location이 없었다.

원인은 Compose의 Nginx `command`가 공식 Nginx 이미지의 entrypoint 템플릿 변환 단계를 우회한 것이었다. 템플릿은 이미지에 존재했지만, Nginx가 실제로 읽는 `/etc/nginx/conf.d/default.conf`로 변환되지 않아 기본 설정이 요청을 처리했다.

프로젝트의 영구 수정은 [`docker-compose.yml`](../../docker-compose.yml)의 Nginx 시작 명령에 다음 변환을 추가하는 것이다.

```sh
envsubst '${DOMAIN}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf
```

발급 당시 EC2에서는 실행 중인 컨테이너에 설정을 즉시 적용하기 위해 임시 인증서를 만든 뒤 템플릿을 변환하고 Nginx를 reload했다.

```bash
cd /home/ubuntu/tech-log

docker exec tech-log-nginx mkdir -p /etc/letsencrypt/live/techlog.site
docker exec tech-log-nginx sh -c 'cd /etc/letsencrypt/live/techlog.site && openssl req -x509 -nodes -newkey rsa:2048 -days 1 -keyout privkey.pem -out fullchain.pem -subj /CN=localhost'

docker exec tech-log-nginx sh -c 'envsubst "\$DOMAIN" < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -t && nginx -s reload'

docker exec tech-log-nginx nginx -T 2>&1 | grep -A4 -B2 'acme-challenge'
curl -i http://techlog.site/.well-known/acme-challenge/check.txt
```

이후 `curl` 응답에서 `HTTP/1.1 200 OK`와 `acme-ok`를 확인했다. 임시 인증서를 제거한 뒤 실제 Let's Encrypt 인증서를 발급했다.

```bash
docker compose --env-file .env.prod run --rm --no-deps --entrypoint /bin/sh certbot -c 'rm -rf /etc/letsencrypt/live/techlog.site /etc/letsencrypt/archive/techlog.site /etc/letsencrypt/renewal/techlog.site.conf'

CERTBOT_EMAIL="$(sed -n 's/^CERTBOT_EMAIL=//p' .env.prod | tail -n 1)"

docker compose --env-file .env.prod run --rm --no-deps --entrypoint certbot certbot certonly --webroot --webroot-path /var/www/certbot --domain techlog.site --email "$CERTBOT_EMAIL" --rsa-key-size 4096 --agree-tos --no-eff-email

docker exec tech-log-nginx nginx -t
docker exec tech-log-nginx nginx -s reload
docker compose --env-file .env.prod up -d certbot
curl -I https://techlog.site
docker compose --env-file .env.prod ps
```

실제 발급 결과:

```text
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/techlog.site/fullchain.pem
Key is saved at:         /etc/letsencrypt/live/techlog.site/privkey.pem
This certificate expires on 2026-08-24.
```

Certbot이 만든 실제 인증서 디렉터리는 host의 `ubuntu` 사용자에게 직접 조회 권한이 없을 수 있다. 이 경우 HTTPS는 정상인데 `deploy.sh`가 host에서 `[ -f data/certbot/conf/live/<DOMAIN>/fullchain.pem ]`를 검사하면 인증서가 없다고 오판한다. 이후 배포 스크립트는 host 권한을 넓히지 않고, 동일 인증서 볼륨을 mount한 Certbot 컨테이너 내부에서 파일 존재를 검사하도록 수정하였다.

또한 `.env.prod`가 CRLF 줄바꿈으로 저장되면 shell에서 읽은 `DOMAIN` 뒤에 `\r`이 붙어 정상 인증서 경로를 찾지 못할 수 있다. 배포 스크립트는 `DOMAIN`과 `CERTBOT_EMAIL`을 읽을 때 이 문자를 제거하도록 구성한다.

확인용 명령:

```bash
cd /home/ubuntu/tech-log
sudo ls -la data/certbot/conf/live/techlog.site/
docker compose --env-file .env.prod run --rm --no-deps --entrypoint /bin/sh certbot -c 'test -f /etc/letsencrypt/live/techlog.site/fullchain.pem && echo certificate-ok'
```

애플리케이션의 영속 컬럼이 추가될 때 기존 PostgreSQL Docker volume은 초기화 SQL을 다시 실행하지 않는다. 현재 자동 배포는 backend를 교체하기 전에 PostgreSQL을 시작하고 `deploy/migrations/V20260527__access_log_error_details.sql`을 실행하여, 관리자 로그 화면에 표시할 request ID와 오류 상세 컬럼을 재실행 가능하게 추가한 뒤 Spring Boot의 schema validation을 통과시킨다.

## 7. 이어서 실행할 최초 배포 절차

### 7.1 완료된 Elastic IP와 가비아 DNS 설정

다음 설정은 완료되었다.

| 입력 항목 | 값 |
| --- | --- |
| 타입 | `A` |
| 호스트 | `@` 또는 루트 도메인을 의미하는 빈 값 |
| 값 | `3.34.203.73` |
| 대상 도메인 | `techlog.site` |

가비아의 `DNS 호스트` 메뉴는 자체 네임서버를 운영할 때 사용하는 기능이므로, 웹사이트 연결을 위한 A 레코드 입력 위치가 아니다. DNS 레코드 관리/설정 화면에서 등록해야 한다.

로컬 PC에서 아래 DNS 결과까지 확인했다.

```bash
nslookup techlog.site
```

```text
Name:    techlog.site
Address: 3.34.203.73
```

### 7.2 완료된 저장소 clone과 이미지 pull

EC2에는 저장소가 clone되었고 GHCR 이미지가 pull된 상태이다. 현재 이미지 상태를 재확인할 때에는 다음을 실행한다.

```bash
cd /home/ubuntu/tech-log
docker images | grep tech-log
```

PAT는 `.env.prod` 또는 Git 저장소에 저장하지 않는다. `docker login` 결과는 EC2 사용자 홈의 Docker 인증 설정으로 저장된다.

### 7.3 완료된 운영 환경 변수 작성

EC2 프로젝트 경로에 `.env.prod`를 작성하여 컨테이너 실행과 인증서 발급에 사용했다. 이 파일은 비밀번호와 OAuth/메일 자격 증명을 포함할 수 있으므로 Git에 commit하지 않는다.

```bash
cd /home/ubuntu/tech-log
cp .env.prod.example .env.prod
vi .env.prod
chmod 600 .env.prod
```

주요 입력 값:

| 환경 변수 | 설정 내용 |
| --- | --- |
| `DOMAIN` | `techlog.site` |
| `CERTBOT_EMAIL` | Let's Encrypt 알림을 받을 이메일 |
| `FRONTEND_ORIGIN` | `https://techlog.site` |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | GitHub OAuth App 운영 자격 증명 |
| `ADMIN_CONSOLE_PASSWORD` | 충분히 긴 관리자 비밀번호 |
| `POSTGRES_PASSWORD` | 충분히 긴 DB 비밀번호 |
| `SPRING_DATASOURCE_PASSWORD` | `POSTGRES_PASSWORD`와 동일하게 설정 |
| `MAIL_*` | 메일 발송을 사용할 경우 Gmail 앱 비밀번호 등 SMTP 값 |

메일 설정을 아직 준비하지 않았다면 최초 배포 시에는 다음처럼 이메일 전송 기능을 비활성화한다.

```env
MAIL_ENABLED=false
```

이 경우 사이트와 HTTPS 배포를 먼저 진행할 수 있으며, 이메일 구독 알림 발송은 SMTP 설정 후 `MAIL_ENABLED=true`로 변경하여 활성화한다.

GitHub OAuth App의 callback URL은 다음 값으로 등록한다.

```text
https://techlog.site/login/oauth2/code/github
```

### 7.4 완료된 HTTPS 최초 발급 및 서비스 시작

`techlog.site`의 인증서는 Certbot webroot 방식으로 발급되었으며, 만료일은 `2026-08-24`이다. 발급 과정의 `404` 문제와 실제 복구 명령은 `6.7 최초 HTTPS 발급 중 발생한 문제와 해결 기록`에 남겼다.

```bash
docker compose --env-file .env.prod ps
curl -I https://techlog.site
```

## 8. CI/CD 운영 설정

최초 HTTPS 배포 후 GitHub Repository의 `production` Environment와 Repository Actions variable이 구성되었고, `main` push 자동 배포가 동작하는 상태이다.

### 8.1 자동 배포 시 주의사항

HTTPS 최초 발급 과정에서 발견한 Nginx 템플릿 변환 수정은 이미 `docker-compose.yml`과 배포 이미지에 반영되었다. 이후 코드를 변경하면 `main` push로 GitHub Actions 검증, 이미지 publish, EC2 배포가 순서대로 수행된다.

DB schema 변경을 포함한 배포에서는 새 backend를 올리기 전에 migration이 실행되어야 한다. Workflow는 EC2에서 대상 커밋을 먼저 checkout한 다음 같은 커밋의 `deploy/deploy.sh`를 실행하도록 수정되어, 배포 스크립트와 애플리케이션 이미지의 버전이 달라지는 문제를 방지한다.

최근 운영 DB에는 `deploy/migrations/V20260527__access_log_error_details.sql`이 적용되어 있다. 관리자 React 전환 브랜치를 배포할 때에는 `deploy/migrations/V20260527__admins.sql`도 적용되어 `ADMINS` 테이블이 준비된 뒤 backend가 올라간다. 두 migration은 반복 실행 가능하다. `ddl.sql`과 `index.sql`은 기존 PostgreSQL volume 재배포 시 자동 재실행되지 않으며, 빈 DB를 최초 초기화할 때만 적용된다.

### 8.2 Secrets

| Secret | 내용 |
| --- | --- |
| `EC2_HOST` | EC2 Elastic IP 또는 `techlog.site` |
| `EC2_SSH_USER` | `ubuntu` |
| `EC2_SSH_PORT` | `22` |
| `EC2_SSH_PRIVATE_KEY` | GitHub Actions가 EC2에 접속할 private key |
| `EC2_SSH_KNOWN_HOSTS` | 검증한 EC2 SSH host key 라인 |
| `EC2_DEPLOY_PATH` | `/home/ubuntu/tech-log` |

### 8.3 Repository Actions variable

`DEPLOY_ENABLED`는 `production` Environment variable로 등록하면 안 된다. Workflow의 deploy job은 `environment: production`의 runner가 시작되기 전에 `if: vars.DEPLOY_ENABLED == 'true'` 조건을 평가하므로, Environment variable은 그 시점에 보이지 않아 job이 `skipped` 처리된다.

GitHub Repository에서 `Settings` > `Secrets and variables` > `Actions` > `Variables`에 아래 값을 등록한다.

| Variable | 값 |
| --- | --- |
| `DEPLOY_ENABLED` | `true` |

이 값이 설정된 이후 `main` 브랜치에 push하면 다음이 자동 수행된다.

```text
테스트 및 frontend build 검증
-> Nginx/Backend Docker 이미지 build
-> GHCR push
-> GitHub Actions가 EC2 SSH 접속
-> EC2에서 새 이미지 pull
-> 컨테이너 교체
```

## 9. 운영 시 기본 확인 및 백업

### 9.1 상태와 로그 확인

EC2 프로젝트 경로에서:

```bash
docker compose --env-file .env.prod ps
docker compose --env-file .env.prod logs -f nginx backend certbot postgres
curl -I https://techlog.site
```

### 9.2 인증서 갱신 점검

현재 실제 인증서의 최초 만료일은 `2026-08-24`이다. 만료일까지 기다렸다가 새 인증서를 발급하는 방식으로 운영하지 않는다. Certbot 컨테이너가 주기적으로 갱신 필요 여부를 점검하고, 인증서가 갱신되면 Nginx가 갱신 파일을 reload하여 사용하게 하는 구조이다.

Let's Encrypt의 현재 단기 인증서는 만료 전에 갱신하도록 운영해야 하며, 이 프로젝트의 Certbot 컨테이너는 12시간마다 다음 동작을 수행한다.

```text
certbot renew --webroot --webroot-path /var/www/certbot --quiet
```

Nginx 컨테이너는 6시간마다 reload하여 갱신된 인증서를 읽는다.

#### 최초 배포 직후 반드시 실행할 갱신 시험

`docker-compose.yml`의 Nginx 템플릿 변환 수정이 GitHub와 EC2에 적용된 뒤 아래를 실행한다.

```bash
cd /home/ubuntu/tech-log

curl -i http://techlog.site/.well-known/acme-challenge/check.txt
docker compose --env-file .env.prod run --rm --entrypoint certbot certbot renew --dry-run
```

성공 시 Certbot의 dry-run 성공 메시지가 출력되어야 한다. `curl`에서 ACME 테스트 파일이 `200 OK`가 아니거나 dry-run이 실패하면 자동 갱신도 실패하므로, 만료일까지 방치하지 않고 즉시 수정한다.

#### 운영 중 확인 명령

현재 브라우저에 제공되는 인증서 만료일 확인:

```bash
echo | openssl s_client -connect techlog.site:443 -servername techlog.site 2>/dev/null | openssl x509 -noout -issuer -subject -dates
```

Certbot이 관리하는 인증서 정보 확인:

```bash
docker compose --env-file .env.prod run --rm --entrypoint certbot certbot certificates
```

컨테이너와 갱신 로그 확인:

```bash
docker compose --env-file .env.prod ps
docker compose --env-file .env.prod logs --tail 100 certbot nginx
```

권장 점검 시점:

| 시점 | 확인 작업 |
| --- | --- |
| HTTPS 발급 직후 | `renew --dry-run` 성공 여부 확인 |
| 월 1회 | 실제 제공 인증서 만료일과 Certbot 로그 확인 |
| 만료 약 30일 전인 `2026-07-25` 전후 | 새 만료일로 자동 갱신되었는지 반드시 확인 |
| EC2 재생성, DNS 변경, 보안 그룹 변경, Nginx 변경 후 | ACME URL `200 OK` 및 `renew --dry-run` 재확인 |

#### 자동 갱신되지 않았을 때의 복구

만료일이 가까운데 인증서 날짜가 갱신되지 않았다면 다음 순서로 확인한다.

1. `techlog.site`가 여전히 Elastic IP `3.34.203.73`을 가리키는지 확인한다.
2. EC2 보안 그룹에서 HTTP `80`, HTTPS `443`이 외부 접근 가능 상태인지 확인한다.
3. ACME 테스트 파일이 HTTP에서 제공되는지 확인한다.
4. Certbot 갱신을 수동으로 실행하고 Nginx를 reload한다.

```bash
cd /home/ubuntu/tech-log

nslookup techlog.site
mkdir -p data/certbot/www/.well-known/acme-challenge
printf 'acme-ok\n' > data/certbot/www/.well-known/acme-challenge/check.txt
curl -i http://techlog.site/.well-known/acme-challenge/check.txt

docker compose --env-file .env.prod run --rm --entrypoint certbot certbot renew
docker exec tech-log-nginx nginx -t
docker exec tech-log-nginx nginx -s reload

echo | openssl s_client -connect techlog.site:443 -servername techlog.site 2>/dev/null | openssl x509 -noout -dates
```

이미 인증서가 만료된 경우에도 같은 webroot 갱신 절차를 수행할 수 있지만, 사용자는 복구 전까지 브라우저 인증서 경고를 보게 된다. 따라서 `2026-07-25` 전후에 실제 인증서의 새 만료일이 갱신됐는지 확인하는 것을 운영 필수 작업으로 둔다.

### 9.3 PostgreSQL 백업 필요성

RDS가 아닌 EC2 내부 컨테이너 DB를 사용하므로 최초 공개 운영 이후 백업 절차를 반드시 추가해야 한다.

최소 운영 방안:

1. EBS 스냅샷을 정기 생성한다.
2. PostgreSQL 논리 백업(`pg_dump`)을 정기 생성한다.
3. 논리 백업 파일은 같은 EC2 디스크만이 아닌 S3 등의 별도 저장소에 보관한다.
4. 인스턴스 타입 변경이나 큰 배포 작업 전에는 수동 백업을 수행한다.

### 9.4 감사 로그 보존 정책

Backend의 `AuditLogRetentionService`는 기본적으로 매일 오전 `03:15` (`Asia/Seoul`)에 90일보다 오래된 `ACCESS_LOGS`, `LOGIN_LOGS` 데이터를 삭제한다.

```env
AUDIT_LOG_RETENTION_DAYS=90
AUDIT_LOG_CLEANUP_CRON=0 15 3 * * *
```

`ACCESS_LOGS`에는 서버 오류 발생 시 request ID, 오류 타입, 오류 메시지, 제한된 stack trace가 포함될 수 있다. `LOGIN_LOGS`에는 로그인 결과, login ID, IP가 포함된다. 게시글, 댓글, 이메일 구독자, 업로드 이미지, PostgreSQL volume은 이 스케줄러로 삭제되지 않는다. 상세 운영 기준은 [`운영가이드.md`](./운영가이드.md)를 따른다.

## 10. 관련 프로젝트 파일

| 파일 | 역할 |
| --- | --- |
| [`docker-compose.yml`](../../docker-compose.yml) | EC2에서 실행되는 네 컨테이너 및 volume/network 정의 |
| [`.env.prod.example`](../../.env.prod.example) | EC2 운영 환경 변수 템플릿 |
| [`deploy/init-letsencrypt.sh`](../../deploy/init-letsencrypt.sh) | 최초 HTTPS 인증서 발급 및 초기 컨테이너 시작 |
| [`deploy/deploy.sh`](../../deploy/deploy.sh) | HTTPS 발급 후 수동/자동 재배포 실행 |
| [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) | 검증, GHCR 이미지 publish, EC2 자동 배포 workflow |
| [`DEPLOY_EC2.md`](../../DEPLOY_EC2.md) | 배포 절차 중심의 기존 기술 문서 |

## 11. 외부 참고 문서

| 문서 | 용도 |
| --- | --- |
| [Certbot Instructions](https://certbot.eff.org/instructions) | webroot 발급과 `renew --dry-run` 갱신 시험 참고 |
| [Let's Encrypt Integration Guide](https://letsencrypt.org/docs/integration-guide/) | 만료 전에 자동 갱신을 점검하는 운영 원칙 참고 |
