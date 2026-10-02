# TaskFlow – Module 33 Starter

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, Flyway migrations, structured logs, Prometheus and Grafana monitoring, a
Docker image, Kubernetes manifests and a Jenkins pipeline). Its data model is
**completely flat**, it has **no caching**, and nobody has looked at how it performs.

Use this project as the starting point for finding and fixing performance problems.

## The problem

Performance problems show up under load, not in a quick local test. A request that takes
200 ms normally can take 8 seconds on a busy day, and users who refresh and retry make it
worse. Two common causes:

- **N+1 queries.** Code loads a list of records, then runs one extra query per record.
  One query quietly becomes 101.
- **Repeated identical work.** The same lookup hits the database every time, though the
  answer rarely changes.

The starter can't show an N+1 problem yet. `Account` has no relationships to other
entities, and real financial data is never this flat. There's also nothing to profile,
count or measure.

## What's already in place

- `Account` is a standalone entity, seeded by Flyway migrations `V1` to `V3`
- `GET /api/v1/accounts/search?name=...` queries the database every time
- A Hikari database connection pool at its default size, never stated anywhere
- `jps` and `jcmd`, which ship with the JDK, can inspect the running app with no new
  tools

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Flyway
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

## See the problem

1. Open `src/main/java/com/taskflow/model/Account.java`. There's no relationship to any
   other entity.
2. Search for an account twice in a row:
   ```bash
   curl "http://localhost:8080/api/v1/accounts/search?name=dev1"
   curl "http://localhost:8080/api/v1/accounts/search?name=dev1"
   ```
   Both calls hit the database, because nothing caches the result.
3. Open `application.yml`. The connection pool size isn't set anywhere, so it silently
   uses the default.

## Project structure

```
.
├── Jenkinsfile
├── jenkins/                                  # Jenkins controller image
├── k8s/                                      # Blue and green deployments, Service, ConfigMap, Secret
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/taskflow/                # Application code
    │   └── resources/
    │       ├── application.yml
    │       ├── db/migration/                 # V1 to V3 Flyway migrations
    │       └── static/demo-trigger.html
    └── test/java/com/taskflow/service/
        └── TaskServiceTest.java
```

## Notes

- Tasks and accounts live in memory and are reset on restart.
