# TaskFlow – Module 19 Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, and a Config Server for
settings) with **structured JSON logging**, a **correlation ID** on every request, and
a log line that no longer leaks a secret.

## What's in place

| Feature | How it works |
|---------|--------------|
| Structured logs | `logging.structured.format.console: ecs` writes each line as JSON |
| Correlation ID | `CorrelationIdFilter` puts an `X-Trace-Id` into the logging context for every request |
| Safe key logging | `PartnerController` logs only the last 4 characters of the API key |

### Structured logging

Spring Boot supports structured logging natively, with no extra dependency. `ecs` is
the Elastic Common Schema, a standard JSON format. Fields such as `@timestamp`,
`log.level` and `message` appear automatically, so logs from services written in other
languages can line up key for key.

The setting goes under the existing `logging:` key in `application.yml`:

```yaml
logging:
  structured:
    format:
      console: ecs
```

### Correlation IDs

`CorrelationIdFilter` does the following for each request:

1. Reads the `X-Trace-Id` request header. If it's missing or blank, it generates a new
   UUID.
2. Stores the ID in the **MDC**, a thread-local map that logging adds to every log line
   for as long as that request runs. No individual `log.info(...)` call needs to change.
3. Sets `X-Trace-Id` on the response, so the caller can quote it.
4. **Always removes the ID in a `finally` block.** MDC is thread-local and threads are
   reused, so without the cleanup, one request's ID could leak into an unrelated
   request on the same thread.

Spring Boot's structured logging includes MDC values in the JSON automatically, so
every log line during a request carries the same `traceId`.

`@Order(Ordered.HIGHEST_PRECEDENCE)` places the filter outside Spring Security's own
filter chain. A request that Security rejects with 401 or 403 still gets a trace ID.

This project uses a plain `X-Trace-Id` header to keep the idea clear. The W3C Trace
Context standard uses a `traceparent` header with a defined structure
(version, trace ID, span ID and flags), which is what a production system would use.

### Safe key logging

The old line printed the full secret. Now it keeps the useful part, a confirmation
that a key was attached, and masks the rest:

```java
log.info("Attaching partner API key ending in: {}", maskKey(partnerApiKeyProperties.getApiKey()));
```

`maskKey` returns `****` plus the last four characters, or just `****` when the key is
null or shorter than four characters.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- H2 in-memory database
- Maven
- Docker (Keycloak and Vault)

## Prerequisites

The partner API key comes from Vault, so Vault must be running with the secret stored.
Follow the setup in the Module 15 project:

```bash
docker compose up -d
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=taskflow-root-token
vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
```

The Config Server from Module 17 is optional. The app starts without it.

## Run it

In the same terminal where `VAULT_TOKEN` is exported:

```bash
mvn spring-boot:run
```

## Try it

**1. Check the JSON output.** Make any request and look at the console. Each line is
now a JSON object with fields such as `@timestamp`, `log.level` and `message`.

**2. Get a generated trace ID.** Send a request with no `X-Trace-Id` header and
inspect the response headers:

```bash
curl -i http://localhost:8080/api/v1/tasks
```

The response includes a freshly generated `X-Trace-Id`. Find the same value in the
`traceId` field of every JSON log line printed during that request.

**3. Reuse an incoming trace ID.** Pretend an upstream service already assigned one:

```bash
curl -i -H "X-Trace-Id: my-test-trace-123" http://localhost:8080/api/v1/tasks
```

The response returns `my-test-trace-123`, and the logs use it too. This is
propagation, not just per-request randomness.

**4. Check the masked key.** Call the partner endpoint:

```bash
curl "http://localhost:8080/api/v1/partners/fetch?url=https://trusted-partner.com/data"
```

The request fails to connect, because that domain doesn't exist for this project. The
log line is written before the request, and it reads
`Attaching partner API key ending in: ****5b4a`. The full secret never appears.

**5. See a rejected request keep its trace ID.** A request that Security rejects still
gets one:

```bash
curl -i -X POST http://localhost:8080/api/v1/tasks
```

You get `401`, and the response still includes an `X-Trace-Id`.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── filter/CorrelationIdFilter.java       # Extracts or generates X-Trace-Id, binds it to MDC
│   ├── config/                               # SecurityConfig, PartnerApiKeyProperties, PartnerRequestProperties
│   ├── controller/
│   │   ├── TaskController.java
│   │   ├── AccountController.java
│   │   ├── ReportController.java
│   │   ├── ConfigController.java
│   │   └── PartnerController.java            # Logs a masked key
│   ├── service/                              # TaskService, AccountService
│   ├── repository/AccountRepository.java
│   ├── model/                                # Task, Account
│   ├── dto/AccountDTO.java
│   ├── security/                             # Retired classes, kept for reference
│   └── util/PasswordHashGenerator.java
└── resources/
    ├── application.yml                       # structured logging: ecs
    ├── data.sql
    └── static/demo-trigger.html
```

## What this doesn't cover

- **Shipping logs** to a central system such as Elasticsearch or Loki.
- **Metrics and dashboards.**
- **Full distributed tracing** with OpenTelemetry. This project has a single service,
  so there is nothing to trace across.

## Notes

- The filter trusts whatever `X-Trace-Id` a caller sends. In a real system, validate
  the value (length and allowed characters) before logging it, or an untrusted client
  can put arbitrary text into your logs.
- Masking the last four characters of a secret is still a trade-off. For
  short or low-entropy secrets, consider logging nothing but a confirmation.
- Tasks and accounts live in memory and are reset on restart.
