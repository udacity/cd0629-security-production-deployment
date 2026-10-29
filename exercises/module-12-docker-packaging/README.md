# Exercise: Apply Docker to Package a Spring Boot Application

## The scenario

`orderservice` has been running via `mvn spring-boot:run` on someone's
laptop since the correlation-ID module. Priya wants it actually
deployable — packaged as a real container image — and since orders need
to persist somewhere durable and inventory lookups are getting hit
repeatedly for the same SKUs, this is also where `orderservice` picks up
real dependencies: **Postgres** (placed orders persist to a
`placed_orders` table) and **Redis** (inventory lookups are cached for
30s via Spring's `@Cacheable`, so `inventoryservice` isn't re-queried on
every repeat request for the same SKU).

## Before you start

*Note*: This exercise requires Docker, therefore we strongly advise running the exercise on your local machine, so you can be fully hands-on and implement these tools yourself. That said, all exercises are optional.

You need Docker (Docker Desktop is fine), JDK 25, and Maven installed.
Make sure Docker is running before Step 1: `docker ps` should print a
(possibly empty) table, not an error.

## Exercise Steps

1. **Build the layered image and check its size.**
   ```bash
   cd orderservice
   docker build -t udabank/orderservice:layered .
   docker images udabank/orderservice:layered
   ```
   Expect a size of roughly 600MB. Most of that is the JRE base image and
   the dependency layer — Spring Boot, Hibernate, and the Postgres and
   Redis clients pull in a fair amount.

2. **Bring up the full stack and check startup time.** From the module
   root (the folder that contains `docker-compose.yml`), start
   `orderservice`, `inventoryservice`, Postgres, and Redis together:
   ```bash
   cd ..   # back to the module root, if you are still in orderservice/
   docker compose up -d --build
   docker compose logs orderservice | grep "Started OrderServiceApplication"
   ```
   Expect a startup time of around 4 seconds. Then place an order and
   confirm it was saved and cached:
   ```bash
   curl http://localhost:8081/orders/ORD-1
   docker compose exec postgres psql -U orderservice -d orderservice -c "SELECT * FROM placed_orders;"
   docker compose exec redis redis-cli KEYS '*'
   ```
   You should see your order in the `placed_orders` table and a key like
   `inventory::SKU-123` in Redis (the cached inventory lookup).

3. **Compare startup with and without a JDK 25 AOT cache.** This step
   runs the jar directly on your machine, not in a container, so you
   measure the JVM itself without container startup overhead. Postgres,
   Redis, and `inventoryservice` from Step 2 stay running, but the
   `orderservice` container must be stopped first because both want port
   8081:
   ```bash
   docker compose stop orderservice
   cd orderservice
   mvn -DskipTests package

   # one-time: record a training run and produce the cache
   java -XX:AOTMode=record -XX:AOTCacheOutput=app.aot -jar target/orderservice-*.jar
   # in another terminal, run: curl http://localhost:8081/orders/ORD-1
   # then stop the app (Ctrl+C) so it writes app.aot

   # baseline
   java -jar target/orderservice-*.jar
   # with the cache
   java -XX:AOTCache=app.aot -jar target/orderservice-*.jar
   ```
   For each run, note the "Started OrderServiceApplication in X seconds"
   line. Expect the cached run to start roughly 30% faster.

4. **Try Jib and compare.** `orderservice/pom.xml` already has the
   `jib-maven-plugin` configured. From the `orderservice` folder, run:
   ```bash
   mvn compile jib:dockerBuild
   ```
   With JDK 25, expect this to fail. Read the error message Maven prints,
   and compare it with Step 1, where the Dockerfile build worked. That
   comparison is the point of this step.

## Common Pitfalls

- **`java -Djarmode=layertools`** is the old syntax (Spring Boot before
  3.3). Spring Boot 4 (and 3.3+) uses **`-Djarmode=tools`** with an
  `extract` subcommand instead. The old flag doesn't error loudly, it
  just doesn't do what you expect. Check that
  `java -Djarmode=tools -jar app.jar list-layers` lists your 4 layers
  before trusting that the Dockerfile copies them correctly.
- **`-XX:AOTMode=record` alone isn't enough.** It needs
  `-XX:AOTCacheOutput=<file>` (or `-XX:AOTConfiguration=<file>` for the
  two-step workflow), or the JVM refuses to start. `-XX:AOTCache=<file>`
  is for *using* an existing cache, not producing one.
- **`port is already allocated` / `address already in use` on 5432.**
  You probably have a Postgres already running on your machine. Either
  stop it, or change the Postgres `ports` line in `docker-compose.yml` to
  `"5433:5432"` and run the host-side `java` commands in Step 3 with
  `DB_PORT=5433`. Containers are unaffected, since they talk to Postgres
  over the compose network on 5432.
- **A port looks free but Docker still can't bind it, or `docker ps`
  hangs.** Docker Desktop's networking may be in a bad state. Restart
  Docker Desktop and wait until `docker ps` responds normally before you
  retry `docker compose up`.

## Task List

- [ ] Layered image built, size checked (Step 1)
- [ ] Full stack up via compose, startup time checked, order persisted to
      Postgres, inventory lookup cached in Redis (Step 2)
- [ ] AOT cache comparison run, both startup times recorded (Step 3)
- [ ] Jib attempted, and the result understood (Step 4)

## Free Response

In 75–125 words: the layered Dockerfile and a plain single-stage
`COPY app.jar` Dockerfile produce an image of roughly the *same total
size* — layering doesn't shrink anything. So what is layering actually
buying you, concretely, the next time someone changes one line of
`OrderController` and pushes? Walk through what Docker's build cache does
differently for the layered version vs. the single-COPY version on that
next build.

*(A good reflection response is between 75 and 125 words.)*
