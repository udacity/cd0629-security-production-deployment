# TaskFlow – Module Demo 33 Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, Flyway migrations, structured logs, Prometheus and Grafana monitoring, a
Docker image, Kubernetes manifests and a Jenkins pipeline) with a **real N+1 query
problem and its fix**, **caching**, SQL logging and an explicit connection pool size.

## What's in place

| Feature | Where |
|---------|-------|
| A relationship that makes N+1 possible | `Account` has many `Transaction` |
| N+1 endpoint, deliberately slow | `GET /api/v1/accounts/transaction-summary/vulnerable` |
| N+1 fix | `GET /api/v1/accounts/transaction-summary/fixed`, using `@EntityGraph` |
| Caching | `@Cacheable("accountSearch")` on `AccountService.searchByNameSafe`, with Caffeine |
| SQL visibility | `org.hibernate.SQL: DEBUG` in `application.yml` |
| Explicit pool size | `hikari.maximum-pool-size: 10` |
| Profiling | Java Flight Recorder, started with `jcmd` |

### The N+1 problem

`Transaction` has a lazy `@ManyToOne` to `Account`, and `Account` has a matching
`@OneToMany` list. Lazy loading is the correct default, and it is also what makes N+1
possible.

- **`vulnerable`:** `accountRepository.findAll()` runs one query for the accounts. Then,
  for every account, `getTransactions()` runs another query the moment it's touched.
  With 3 accounts that is 4 queries, and with 100 accounts it is 101. The count grows
  with the data.
- **`fixed`:** `findAllWithTransactions()` is annotated with
  `@EntityGraph(attributePaths = "transactions")`, so Hibernate fetches accounts and
  their transactions in **one** query with a join. The response has the same shape and
  the same data, however many accounts exist.

Both endpoints return `AccountTransactionSummary` (account name and transaction count),
which keeps `internalRiskScore` out of this response path, as in Module 13.

### Migrations

| Migration | What it does |
|-----------|--------------|
| `V4__create_transaction_table.sql` | Creates the `transaction` table with a foreign key to `account` |
| `V5__seed_demo_transactions.sql` | Adds three transactions per account, enough to make the query burst visible |

### Caching

`@Cacheable("accountSearch")` on `searchByNameSafe` keys the cache on the search name. The
same name searched twice hits the database once, and the second call is served from
memory. `@EnableCaching` on `TaskFlowApplication` is required: without it, `@Cacheable`
silently does nothing. With `spring-boot-starter-cache` and `caffeine` both on the
classpath, Spring Boot configures the cache manager for you.

### Connection pool

`hikari.maximum-pool-size: 10` matches Hikari's default, now stated explicitly.
However many concurrent requests the app appears to handle, only this many database
queries run at once.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Flyway, Hibernate, Caffeine
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

The startup log should list the Flyway migrations `V1` to `V5` as applied.

## Try it

**1. Count the queries for the N+1 endpoint.** Call it and watch the console, which now
prints every SQL statement:

```bash
curl http://localhost:8080/api/v1/accounts/transaction-summary/vulnerable
```

Count the `select` statements: one for the accounts, then one more per account.

**2. Count them for the fixed endpoint.**

```bash
curl http://localhost:8080/api/v1/accounts/transaction-summary/fixed
```

The response is the same, and there's now a single `select`.

**3. Profile the running app with Java Flight Recorder.** No extra tools are needed.

```bash
jps
```

Find TaskFlow's process ID (listed by its main class name), then record for 60 seconds:

```bash
jcmd <pid> JFR.start duration=60s filename=taskflow-profile.jfr
jcmd <pid> JFR.check
```

While it records, send some traffic, such as the requests above. When it finishes, the
`.jfr` file is in the directory the app was started from. Opening it needs a flight
recorder viewer such as JDK Mission Control, which isn't covered here, but the recording
itself is real.

**4. See the cache work.** Call the same search twice:

```bash
curl "http://localhost:8080/api/v1/accounts/search?name=dev1"
curl "http://localhost:8080/api/v1/accounts/search?name=dev1"
```

The first call prints a `select` in the console. The second prints none, because it is
served from the cache.

**5. Run a simple load test**, with no extra tooling:

```bash
for i in {1..20}; do curl -s -o /dev/null -w "%{time_total}\n" "http://localhost:8080/api/v1/accounts/search?name=dev1"; done
```

Look for the **worst** single value, not just a rough average. The slowest request is
what a real user feels. Real load-testing tools such as Gatling and JMeter report
percentiles for the same reason.

## Project structure

```
.
├── Jenkinsfile
├── jenkins/                                  # Jenkins controller image
├── k8s/                                      # Blue and green deployments, Service, ConfigMap, Secret
├── Dockerfile
├── docker-compose.yml
├── pom.xml                                   # Adds spring-boot-starter-cache and caffeine
└── src/
    ├── main/
    │   ├── java/com/taskflow/
    │   │   ├── TaskFlowApplication.java      # @EnableCaching
    │   │   ├── model/                        # Account (has transactions), Transaction
    │   │   ├── dto/AccountTransactionSummary.java
    │   │   ├── repository/AccountRepository.java   # findAllWithTransactions with @EntityGraph
    │   │   ├── service/AccountService.java   # Vulnerable and fixed summaries, @Cacheable search
    │   │   └── controller/AccountController.java
    │   └── resources/
    │       ├── application.yml               # SQL logging, pool size
    │       ├── db/migration/                 # V1 to V5 Flyway migrations
    │       └── static/demo-trigger.html
    └── test/java/com/taskflow/service/
        └── TaskServiceTest.java
```

## What this doesn't cover

- **Garbage collector tuning** (G1, Generational ZGC, Shenandoah). A meaningful
  comparison needs a memory-heavy workload this project doesn't have.
- **Real load-testing tools** such as Gatling and JMeter.
- **Infrastructure as code**, such as Terraform.

## Notes

- **The vulnerable endpoint stays in the project on purpose**, so you can compare it with
  the fixed one. Don't copy that pattern into real code.
- **A cache can serve stale data.** `accountSearch` has no expiry or invalidation here.
  Real caches need an eviction policy, and shouldn't hold data that must always be
  current.
- **Don't share a `.jfr` file casually.** Flight recordings can include system details
  such as paths, system properties and environment information.
- Tasks and accounts live in memory and are reset on restart.
