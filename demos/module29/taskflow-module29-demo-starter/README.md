# TaskFlow – Module 29 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, structured logs, Prometheus and Grafana monitoring, a Docker image and
Kubernetes manifests). It has **no automated tests and no CI/CD pipeline**.

Use this project as the starting point for adding tests and a Jenkins pipeline.

## The problem

Every module since Module 3 has had `spring-boot-starter-test` in `pom.xml`, and nothing
uses it. The project has no tests at all. Building and shipping is manual too: compile
on a laptop, build an image by hand, and hope nothing differs between that laptop and
production.

A CI/CD pipeline runs the same build, test and package steps automatically, in a clean
environment, on every change.

## What's already in place

- A multi-stage `Dockerfile` and `.dockerignore` (Module 25)
- Kubernetes manifests in `k8s/` (Module 27)
- `docker-compose.yml` for the supporting services (Keycloak, Vault, Elasticsearch,
  Kibana, Prometheus and Grafana)

There is no `src/test/`, no `Jenkinsfile` and no `jenkins/` folder.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- Micrometer with the Prometheus registry
- Maven
- Docker and Kubernetes

## Prerequisites

The partner API key comes from Vault, so Vault must be running with the secret stored.
Follow the setup in the Module 15 project:

```bash
docker compose up -d
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=taskflow-root-token
vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
```

## Run it

In the same terminal where `VAULT_TOKEN` is exported:

```bash
mvn spring-boot:run
```

## See the problem

1. Open `pom.xml` and find `spring-boot-starter-test`. It is a dependency with no
   tests behind it.
2. Run the tests:
   ```bash
   mvn test
   ```
   Maven reports that there are no tests to run.
3. Look in the project folder. There's no `Jenkinsfile`, so nothing builds, tests or
   packages the app automatically.

## Project structure

```
.
├── k8s/                                      # Kubernetes manifests
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
