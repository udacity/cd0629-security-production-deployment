# TaskFlow – Module 27 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, structured logs in Elasticsearch and Kibana, and Prometheus and Grafana
monitoring) with a `Dockerfile`, so it can run as a single container. It has **no
Kubernetes manifests**.

Use this project as the starting point for deploying TaskFlow to Kubernetes.

## The problem

One container, run by Docker or Docker Compose, has no supervisor. If the container
crashes on a healthy machine, nothing notices and nothing restarts it. There's also no
way to run several copies, replace unhealthy ones automatically, or give them one
stable address.

Kubernetes handles those jobs. It keeps a declared number of copies running, replaces
any that die, and routes traffic only to the ones that are ready.

## What's already in place

- A multi-stage `Dockerfile` and `.dockerignore` (Module 25) that build a small image
  running as a non-root user
- Health endpoints at `/actuator/health/liveness` and `/actuator/health/readiness`
  (Module 23), which Kubernetes can use as probes
- `docker-compose.yml` for the supporting services (Keycloak, Vault, Elasticsearch,
  Kibana, Prometheus and Grafana)

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- Micrometer with the Prometheus registry
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

## Run it

Build and run the container as in Module 25:

```bash
docker build -t taskflow-api:latest .

docker run -p 8080:8080 \
  -e SPRING_CLOUD_VAULT_URI=http://host.docker.internal:8200 \
  -e VAULT_TOKEN=taskflow-root-token \
  --add-host=host.docker.internal:host-gateway \
  taskflow-api:latest
```

Check that it works:

```bash
curl http://localhost:8080/actuator/health
```

## See the problem

1. Look in the project folder. There's no `k8s/` directory and no manifests.
2. In another terminal, stop the container:
   ```bash
   docker ps
   docker kill <container-id>
   ```
3. Nothing brings it back. The app stays down until you start it again by hand.

## Project structure

```
.
├── Dockerfile                                # Multi-stage build, non-root runtime
├── .dockerignore
├── docker-compose.yml                        # Supporting services
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

- Tasks and accounts live in memory and are reset on restart.
