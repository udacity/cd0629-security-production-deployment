# TaskFlow – Module 25 Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, structured logs in Elasticsearch and Kibana, and Prometheus and Grafana
monitoring) that can now be **packaged as a Docker image** and run in a container.

Running TaskFlow in Docker is **opt-in**, through a Compose profile. Plain
`docker compose up -d` still starts only the supporting services, and running with
`mvn spring-boot:run` still works as before.

## What's in place

| Feature | Where |
|---------|-------|
| Multi-stage image build | `Dockerfile` |
| Dependency-layer caching | `pom.xml` copied and resolved before `src` |
| Small, minimal runtime image | `eclipse-temurin:25-jre-alpine` |
| Non-root user | A dedicated `taskflow` user |
| Small build context | `.dockerignore` |
| Opt-in container service | `taskflow-api` in `docker-compose.yml`, profile `containerized` |

### The Dockerfile

**Stage 1, build:** `maven:3.9-eclipse-temurin-25` has Maven and JDK 25 together, so no
wrapper script is needed.

1. `COPY pom.xml` on its own, then `mvn dependency:go-offline`. This layer is
   rebuilt only when `pom.xml` changes, so the slow dependency download stays cached.
2. `COPY src`, then `mvn package -DskipTests`. A change to a Java file rebuilds only
   from here.

**Stage 2, runtime:** `eclipse-temurin:25-jre-alpine` has a JRE, not a JDK. There's no
compiler or build tooling, and Alpine adds few system packages, which gives a smaller
image and a smaller attack surface. Only the finished JAR is copied across
(`COPY --from=build`), so nothing from the build stage ships.

The app runs as an unprivileged `taskflow` user. If the app is ever compromised, root
inside the container would be a far worse outcome.

### `.dockerignore`

Keeps `target/`, `logs/`, `.git/`, IDE files and `.DS_Store` out of the build context,
so builds are faster and local files don't end up in the image.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- Maven
- Docker

## Prerequisites

- Docker Desktop
- Vault running with the secret stored, as in the Module 15 project:

```bash
docker compose up -d
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=taskflow-root-token
vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
```

## Build the image

```bash
docker build -t taskflow-api .
```

## Run it

### Option 1: Standalone container

Vault's port is published on your machine, and a standalone container isn't on the
Compose network, so it reaches Vault through the host:

```bash
docker run -p 8080:8080 \
  -e SPRING_CLOUD_VAULT_URI=http://host.docker.internal:8200 \
  -e VAULT_TOKEN=taskflow-root-token \
  --add-host=host.docker.internal:host-gateway \
  taskflow-api
```

Check that it works:

```bash
curl http://localhost:8080/actuator/health
```

### Option 2: In the full Compose stack

```bash
docker compose --profile containerized up -d
curl http://localhost:8080/actuator/health
```

Vault is a required import, so a successful start is proof the container reached Vault.

## Try the layer caching

1. Build the image once, so every layer is cached.
2. Change a line in any Java file. A comment is enough.
3. Build again:
   ```bash
   docker build -t taskflow-api .
   ```
4. In the build output, the dependency-download layer shows `CACHED`. Only the last
   few steps (copying the source and repackaging the JAR) run again.

## Networking inside Compose

Once TaskFlow runs inside the Compose network, `localhost` means the container itself,
not your machine or its sibling containers. The `taskflow-api` service therefore reaches
the others by **service name**:

| Setting | Value inside the container |
|---------|----------------------------|
| Keycloak issuer URI | `http://keycloak:8080/realms/taskflow` |
| Vault URI | `http://vault:8200` |

These are set through environment variables on the `taskflow-api` service.

## Known limitation

**A full authenticated request doesn't work against the containerized app yet.** A JWT
you get through Keycloak's host-facing URL (`http://localhost:8081`) carries an `iss`
claim of `http://localhost:8081/realms/taskflow`. The container expects
`http://keycloak:8080/realms/taskflow`, so the issuer doesn't match and the token is
rejected. This is a common Docker and Keycloak mismatch. Fixing it properly means
configuring Keycloak's hostname settings, which this project doesn't do.

Test the container with endpoints that don't need a token, such as
`/actuator/health` and `GET /api/v1/tasks`.

## Project structure

```
.
├── Dockerfile                                # Multi-stage build, non-root runtime
├── .dockerignore
├── docker-compose.yml                        # Supporting services, plus taskflow-api (profile: containerized)
├── prometheus.yml
├── alerts.yml
├── scripts/import-logs.sh
├── pom.xml
└── src/main/
    ├── java/com/taskflow/
    │   ├── filter/CorrelationIdFilter.java
    │   ├── config/                           # SecurityConfig, PartnerApiKeyProperties, PartnerRequestProperties
    │   ├── controller/                       # Task, Account, Report, Config, Partner controllers
    │   ├── service/                          # TaskService, AccountService
    │   ├── repository/AccountRepository.java
    │   ├── model/                            # Task, Account
    │   ├── dto/AccountDTO.java
    │   ├── security/                         # Retired classes, kept for reference
    │   └── util/PasswordHashGenerator.java
    └── resources/
        ├── application.yml
        ├── data.sql
        └── static/demo-trigger.html
```

## Notes

- `VAULT_TOKEN` and the demo secret are for local development only. Never bake secrets
  into an image, and pass them in at runtime as shown above.
- The Vault root token is static and for local development only. See the Module 15
  project.
- Tasks and accounts live in memory and are reset on restart.
