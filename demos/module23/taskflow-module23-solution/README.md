# TaskFlow – Module 23 Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, and structured logs in Elasticsearch and Kibana) with **application
monitoring**: Micrometer metrics scraped by **Prometheus**, graphed in **Grafana**,
plus a latency alert rule and a locked-down `/actuator/refresh`.

## What's in place

| Feature | How it works |
|---------|--------------|
| Metrics endpoint | `micrometer-registry-prometheus` plus `prometheus` in the actuator exposure list |
| Scraping | Prometheus reads `/actuator/prometheus` every 15 seconds |
| Latency percentiles | `percentiles-histogram` is enabled for `http.server.requests`, so Prometheus can compute a real p99 |
| Business metric | `partner.fetch.requests` counter in `PartnerController`, tagged by outcome |
| Health probes | Separate liveness and readiness endpoints |
| Alert | `alerts.yml`: p99 latency over 9 seconds on the partner fetch endpoint |
| Dashboards | Grafana |
| Secured reload | `/actuator/refresh` now requires `MANAGER` |

### Metrics

- **Percentile histograms.** An average can hide slow requests. With a histogram,
  Prometheus can calculate the 99th percentile, which shows how the slowest requests
  behave.
- **A custom counter.** Spring Boot gives every endpoint HTTP metrics for free. This
  counter tracks something the app specifically cares about: partner fetch
  requests, tagged `outcome=attempted` (passed the allowlist) or `outcome=blocked`
  (rejected by it). A counter only goes up, like a mile marker. In Prometheus it is
  named `partner_fetch_requests_total`.
- **Liveness vs readiness.** `/actuator/health/liveness` says whether the process is
  alive. `/actuator/health/readiness` says whether it can serve traffic right now.
  They are separate signals, and they shouldn't be confused.

### Alert

`alerts.yml` defines one rule, `PartnerFetchTooSlow`. It fires when the p99 latency of
`/api/v1/partners/fetch` stays above **9 seconds** for 1 minute. It makes "too slow" an
explicit definition in configuration, instead of a guess. There is no Slack or
PagerDuty behind it: Prometheus evaluates the rule, and you can see it in the Alerts tab.

### Securing the actuator

`/actuator/refresh` triggers a live config reload, and until now anyone could call it.
`SecurityConfig` now requires `MANAGER` for it. `/actuator/health` and
`/actuator/prometheus` stay open on purpose. In a real deployment they are
usually reachable only from inside the private network (load-balancer probes and
Prometheus's scraper), so network isolation protects them instead of application-level
authentication.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- Micrometer with the Prometheus registry
- Prometheus v3.0.1 and Grafana 11.4.0
- Maven
- Docker (Keycloak, Vault, Elasticsearch, Kibana, Prometheus and Grafana)

## Prerequisites

Docker Desktop should have at least 4 GB of memory, because Elasticsearch runs
alongside everything else. If you don't need log search for this module, you can start
just the services you want instead of everything in the compose file.

## Set up

1. **Start the services:**
   ```bash
   docker compose up -d
   ```
2. **Store the Vault secret** (see the Module 15 project):
   ```bash
   export VAULT_ADDR=http://localhost:8200
   export VAULT_TOKEN=taskflow-root-token
   vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
   ```
3. **Start TaskFlow**, in a terminal where `VAULT_TOKEN` is exported:
   ```bash
   mvn spring-boot:run
   ```
4. **Check that Prometheus is scraping TaskFlow.** Open
   <http://localhost:9090/targets>. The `taskflow-api` target should be **UP**. If it
   shows **DOWN**, TaskFlow isn't running yet, or `/actuator/prometheus` isn't
   reachable.
5. **Open Grafana** at <http://localhost:3001>, with the login `admin` / `admin`. It
   offers to change the password, which you can skip for local development.

Grafana uses host port **3001**, not its default 3000, because Module 11's
`cors-frontend` uses 3000.

TaskFlow runs on your machine, not inside Docker, so `prometheus.yml` scrapes
`host.docker.internal:8080` to reach it from the Prometheus container. That hostname
works on Docker Desktop. The compose file adds an `extra_hosts` entry in case you are
on native Linux Docker.

## Try it

1. **Check the secured endpoint.** With no token:
   ```bash
   curl -i -X POST http://localhost:8080/actuator/refresh
   ```
   You get `403`. With a `manager1` token from Keycloak (see the Module 9 project), the
   same request succeeds:
   ```bash
   curl -i -X POST http://localhost:8080/actuator/refresh \
     -H "Authorization: Bearer <manager1_access_token>"
   ```
2. **Generate partner traffic.** Make allowed and blocked requests:
   ```bash
   # Passes the allowlist (then fails to connect, because the domain doesn't exist)
   curl "http://localhost:8080/api/v1/partners/fetch?url=https://trusted-partner.com/data"

   # Blocked by the allowlist
   curl "http://localhost:8080/api/v1/partners/fetch?url=http://169.254.169.254/latest/meta-data/"
   ```
3. **Read the raw metrics.** Open <http://localhost:8080/actuator/prometheus> and find
   `partner_fetch_requests_total`, with one series per outcome.
4. **Query Prometheus.** Open <http://localhost:9090> and run:
   ```
   partner_fetch_requests_total
   ```
   The numbers match the requests you just made.
5. **Check the health probes.**
   ```bash
   curl http://localhost:8080/actuator/health/liveness
   curl http://localhost:8080/actuator/health/readiness
   ```
6. **Graph it in Grafana.** Go to **Connections → Data sources → Add data source →
   Prometheus**, set the URL to `http://prometheus:9090` (container to container, on
   the same Docker network), and save. Then create a dashboard with a panel that
   graphs `partner_fetch_requests_total`.
7. **Look at the alert.** In Prometheus, open the **Alerts** tab. `PartnerFetchTooSlow`
   is loaded. It only fires if the p99 latency of the partner endpoint stays above 9
   seconds.

## Project structure

```
.
├── docker-compose.yml                        # Keycloak, Vault, Elasticsearch, Kibana, Prometheus, Grafana
├── prometheus.yml                            # Scrape config for TaskFlow
├── alerts.yml                                # PartnerFetchTooSlow alert rule
├── scripts/import-logs.sh                    # Ships the log file into Elasticsearch
├── logs/                                     # Generated at runtime, not committed
├── pom.xml
└── src/main/
    ├── java/com/taskflow/
    │   ├── filter/CorrelationIdFilter.java
    │   ├── config/                           # SecurityConfig, PartnerApiKeyProperties, PartnerRequestProperties
    │   ├── controller/                       # PartnerController has the counter
    │   ├── service/                          # TaskService, AccountService
    │   ├── repository/AccountRepository.java
    │   ├── model/                            # Task, Account
    │   ├── dto/AccountDTO.java
    │   ├── security/                         # Retired classes, kept for reference
    │   └── util/PasswordHashGenerator.java
    └── resources/
        ├── application.yml                   # prometheus exposed, histograms, health probes
        ├── data.sql
        └── static/demo-trigger.html
```

## What this doesn't cover

- **OpenTelemetry and the wider Grafana stack**, and commercial platforms such as Datadog.
- **A full alerting pipeline** (Slack, PagerDuty). The alert is a rule only.
- **Service level objectives, error budgets and burn rates**, which are covered in
  concept only.

## Notes

- Grafana's `admin` / `admin` login and the open Prometheus and Grafana ports are for
  local development only.
- `/actuator/health` and `/actuator/prometheus` are open without authentication here.
  In production, keep them reachable only from inside your network.
- Tasks and accounts live in memory and are reset on restart.
