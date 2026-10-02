# TaskFlow – Module Demo 21 Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, and structured JSON logging with correlation IDs) whose logs are now
written to a file, loaded into **Elasticsearch** and searchable in **Kibana**. You can
find everything one request did with a single search on its trace ID.

## How it works

```
TaskFlow ──writes──▶ logs/taskflow.log ──import script──▶ Elasticsearch ◀──search── Kibana
```

| Stage | Handled by |
|-------|------------|
| Produce structured logs | Spring Boot, `logging.structured.format.file: ecs` |
| Write to a file | `logging.file.name: logs/taskflow.log` |
| Collect and ship | `scripts/import-logs.sh` |
| Store and index | Elasticsearch 8.17.4 |
| Search and explore | Kibana 8.17.4 |

The console output is unchanged. The file adds a second copy of the same JSON, because
a file is what gets shipped anywhere.

**The collection step is simplified.** A production setup runs an agent such as
Filebeat, which watches the log file continuously and ships each new line as soon as
it's written. Here, `scripts/import-logs.sh` does the same job once, by hand, so you
can focus on searching the logs. The destination and the search experience are the
same.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- Elasticsearch and Kibana 8.17.4
- Maven
- Docker (Keycloak, Vault, Elasticsearch and Kibana)

## Before you start: check Docker's memory

Elasticsearch is the most memory-hungry service in this course. In Docker Desktop, open
**Settings → Resources** and make sure at least **4 GB** of memory is allocated. If a
container exits with **code 137**, it ran out of memory and was killed.

`docker-compose.yml` already caps Elasticsearch's heap at 512 MB to keep this
lightweight, but Docker still needs room to run Keycloak, Vault and Elasticsearch
together.

## Set up

1. **Start everything:**
   ```bash
   docker compose up -d
   ```
   Elasticsearch and Kibana take longer to become ready than Keycloak or Vault. Give
   them **1 to 2 minutes**.
2. **Check Elasticsearch:** <http://localhost:9200> should return JSON with the
   tagline `"You Know, for Search"`.
3. **Check Kibana:** <http://localhost:5601> should load the Kibana UI. There is no
   login, because security is disabled.
4. **Store the Vault secret** (see the Module 15 project):
   ```bash
   export VAULT_ADDR=http://localhost:8200
   export VAULT_TOKEN=taskflow-root-token
   vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
   ```
5. **Start TaskFlow**, in a terminal where `VAULT_TOKEN` is exported:
   ```bash
   mvn spring-boot:run
   ```

## Try it

1. **Generate some logs.** Make a few requests, including the partner fetch so there's
   a richer line to find later:
   ```bash
   curl -i http://localhost:8080/api/v1/tasks
   curl "http://localhost:8080/api/v1/partners/fetch?url=https://trusted-partner.com/data"
   ```
   The partner request fails to connect, because that domain doesn't exist for this
   project. The log line is still written.
2. **Note a trace ID.** Copy the `X-Trace-Id` value from one of the response headers
   (`curl -i` shows them).
3. **Check the log file.** `logs/taskflow.log` now exists and holds JSON lines.
4. **Load the logs into Elasticsearch:**
   ```bash
   chmod +x scripts/import-logs.sh
   ./scripts/import-logs.sh
   ```
   It prints how many lines it indexed into the `taskflow-logs` index.
5. **Create a data view in Kibana.** Open <http://localhost:5601>, go to
   **Stack Management → Data Views → Create data view**, and use:
   - **Index pattern:** `taskflow-logs*`
   - **Timestamp field:** `@timestamp`
6. **Search.** Open **Discover** and search for the trace ID from step 2. You get that
   one request's log lines, with no terminal-hunting.

## The import script

`scripts/import-logs.sh` reads `logs/taskflow.log` line by line and posts each
line to Elasticsearch as its own document. If the log file doesn't exist yet, it tells
you to make some requests first.

Running it again **imports every line again**, so documents appear more than once.
To start clean, delete the index first:

```bash
curl -X DELETE http://localhost:9200/taskflow-logs
```

## Project structure

```
.
├── docker-compose.yml                        # Keycloak, Vault, Elasticsearch, Kibana
├── scripts/import-logs.sh                    # Ships the log file into Elasticsearch
├── logs/taskflow.log                         # Generated at runtime, not committed
├── pom.xml
└── src/main/
    ├── java/com/taskflow/
    │   ├── filter/CorrelationIdFilter.java   # X-Trace-Id into every log line
    │   ├── config/                           # SecurityConfig, PartnerApiKeyProperties, PartnerRequestProperties
    │   ├── controller/                       # Task, Account, Report, Config, Partner controllers
    │   ├── service/                          # TaskService, AccountService
    │   ├── repository/AccountRepository.java
    │   ├── model/                            # Task, Account
    │   ├── dto/AccountDTO.java
    │   ├── security/                         # Retired classes, kept for reference
    │   └── util/PasswordHashGenerator.java
    └── resources/
        ├── application.yml                   # Console and file structured logging
        ├── data.sql
        └── static/demo-trigger.html
```

## What this doesn't cover

- **A live collection agent** such as Filebeat, or a sidecar, instead of a manual import.
- **Other logging platforms.** Splunk and Loki are only compared in concept.

## Notes

- Elasticsearch and Kibana run with **security disabled** and a single node. That is
  for local development only. Never run them like this in production.
- `logs/` is in `.gitignore`, so the log file is never committed.
- Logs can contain sensitive data. Before shipping them anywhere central, make sure
  nothing secret is written to them (see Module 19).
- Tasks and accounts live in memory and are reset on restart.
