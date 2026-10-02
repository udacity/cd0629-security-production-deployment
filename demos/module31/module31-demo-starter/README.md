# TaskFlow – Module 31 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, structured logs, Prometheus and Grafana monitoring, a Docker image,
Kubernetes manifests, unit tests and a Jenkins CI pipeline). It deploys as a **single
Deployment**, and Hibernate creates and drops its database schema on every start.

Use this project as the starting point for blue-green deployments and database
migrations.

## The problems

### Hibernate owns the schema

```yaml
jpa:
  hibernate:
    ddl-auto: create-drop
```

With `create-drop`, every restart wipes and recreates the schema from scratch, and demo
data is loaded from `data.sql`. That is fine for a demo and catastrophic for a real
production database. Schema changes also have no history: nothing records what changed,
when, or in what order.

### In-place deployments

`k8s/deployment.yaml` defines one Deployment behind one Service. Deploying a new
version means updating that Deployment in place. For a while, old and new Pods are
mixed together, and if something is wrong there is no clean, instant way to go back.

## What's already in place

- `Jenkinsfile` with Checkout, Build, Test, Package and Docker Build stages, and a
  `jenkins/` folder for the Jenkins controller (Module 29)
- Unit tests for `TaskService`
- `k8s/` with a ConfigMap, Secret, Deployment and Service (Module 27)
- A multi-stage `Dockerfile`

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- H2 in-memory database
- Maven
- Docker, Kubernetes and Jenkins

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

Check that the seeded data is there:

```bash
curl "http://localhost:8080/api/v1/accounts/search?name=dev1"
```

## See the problems

1. Open `src/main/resources/application.yml` and find `ddl-auto: create-drop`. Restart the
   app, and everything is recreated from scratch.
2. Open `src/main/resources/data.sql`. The seed data lives in a loose file, outside any
   tracked, ordered change history.
3. Open `k8s/deployment.yaml` and `k8s/service.yaml`. There's one Deployment and one
   Service, with no second environment to switch to.

## Project structure

```
.
├── Jenkinsfile                               # Checkout → Build → Test → Package → Docker Build
├── jenkins/                                  # Jenkins controller image
├── k8s/
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml                       # One Deployment, updated in place
│   └── service.yaml
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/taskflow/                # Application code
    │   └── resources/
    │       ├── application.yml               # ddl-auto: create-drop
    │       ├── data.sql                      # Seed accounts
    │       └── static/demo-trigger.html
    └── test/java/com/taskflow/service/
        └── TaskServiceTest.java
```

## Notes

- Tasks and accounts live in memory and are reset on restart.
