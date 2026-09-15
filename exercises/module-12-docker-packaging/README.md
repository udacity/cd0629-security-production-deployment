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

## What's here — and what's actually been built and measured

Unlike the ELK/Prometheus modules, most of this one *is* live-verified —
Docker builds and container runs, not just config review. Every number
in Steps 1–3 and 5 below was actually measured, not estimated. Step 4
(the full `docker-compose.yml` stack) is the one exception: the
equivalent setup — `orderservice` + `inventoryservice` + Postgres + Redis,
manually run and networked together with plain `docker run` — was
confirmed working (order persisted to Postgres, inventory lookup cached
in Redis), but `docker compose up` itself hit a Docker Desktop networking
issue partway through this exercise's build (a restart left the daemon
briefly unresponsive) and wasn't re-verified afterward. The compose file
is syntactically validated (`docker compose config` parses it and
resolves all the same values a manual `docker run` used successfully),
just not click-run-confirmed as literally `docker compose up`.

- `orderservice/Dockerfile` — a real 3-stage build: compile → extract
  Spring Boot's layered jar → assemble the runtime image from those
  layers (see the comments in the file for why layering matters).
- `docker-compose.yml` — `orderservice` + `inventoryservice` + Postgres +
  Redis, wired together over a Docker network.

## Exercise Steps

1. **Build the layered image and check its size.**
   ```bash
   cd orderservice
   docker build -t udabank/orderservice:layered .
   docker images udabank/orderservice:layered
   ```
   Measured on this machine: **621MB**. (Most of that is the JRE base
   image and the dependency layer — Spring Boot + Hibernate + the
   Postgres/Redis clients pull in a fair amount.)

2. **Run it and check startup time** (needs Postgres/Redis reachable —
   easiest via `docker compose up` from the module root, see Step 4):
   ```bash
   docker logs <container> | grep "Started OrderServiceApplication"
   ```
   Measured (running inside a real container, connected to real Postgres
   + Redis containers): **3.899 seconds**.

3. **Compare startup with and without a JDK 25 AOT cache.** This part
   was measured running the jar directly (not containerized) to isolate
   the JVM-level effect from container-startup overhead:
   ```bash
   # one-time: record a training run and produce the cache
   java -XX:AOTMode=record -XX:AOTCacheOutput=app.aot -jar target/orderservice-*.jar
   # (hit a few endpoints, then stop it — Ctrl+C or SIGTERM — so it writes app.aot)

   # baseline
   java -jar target/orderservice-*.jar
   # with the cache
   java -XX:AOTCache=app.aot -jar target/orderservice-*.jar
   ```
   Measured: **4.539s without the cache, 3.142s with it** — about a 31%
   reduction, with no errors or behavior differences in either run.

4. **Bring up the full stack** (app + Postgres + Redis + inventoryservice):
   ```bash
   cd .. # module root
   docker compose up -d --build
   curl http://localhost:8081/orders/ORD-1
   ```
   The equivalent manual setup (each container run individually, wired
   together on a shared Docker network) was confirmed working end-to-end:
   the order got persisted to Postgres and the inventory lookup got
   cached in Redis under `inventory::SKU-123`. `docker compose up` itself
   wasn't re-run after a Docker Desktop restart interrupted testing — if
   you hit anything unexpected here, that's the one part of this exercise
   genuinely worth double-checking yourself rather than trusting these
   notes fully.
   ```bash
   docker compose exec postgres psql -U orderservice -d orderservice -c "SELECT * FROM placed_orders;"
   docker compose exec redis redis-cli KEYS '*'
   ```

5. **Try Jib — and see what actually happens.** `orderservice/pom.xml`
   already has the `jib-maven-plugin` configured:
   ```bash
   mvn compile jib:dockerBuild
   ```
   On this machine, with JDK 25: **this fails.** See
   [`docs/SOLUTION.md`](docs/SOLUTION.md) for exactly why, and why that
   failure is itself the most useful part of this comparison.

## Common Pitfalls

- **`java -Djarmode=layertools`** is the old syntax (Spring Boot ≤3.1) —
  Spring Boot 4 (and 3.2+) uses **`-Djarmode=tools`** with an `extract`
  subcommand instead. The old flag doesn't error loudly, it just doesn't
  do what you expect — always check `java -Djarmode=tools -jar app.jar
  list-layers` actually lists your 4 layers before trusting the Dockerfile
  copies them correctly.
- **`-XX:AOTMode=record` alone isn't enough** — it needs
  `-XX:AOTCacheOutput=<file>` (or `-XX:AOTConfiguration=<file>` for the
  two-step workflow) or the JVM refuses to start at all with a clear
  error. `-XX:AOTCache=<file>` is for *using* an existing cache, not
  producing one.
- **A host port that looks free via `lsof` can still fail to bind** if
  Docker Desktop's own networking is in a bad state — this happened while
  building this exercise (`docker run -p 5433:5432 ...` failed with
  "address already in use" while `lsof -i :5433` showed nothing
  listening at all). Restarting Docker Desktop didn't resolve it cleanly
  either — the daemon was still unresponsive to basic commands like
  `docker ps` for a while afterward. If `docker compose up` fails this
  way and `lsof` shows the port genuinely free, it's Docker Desktop
  itself, not your compose file — but be prepared for a restart to take
  longer than expected, and don't assume it fixed things until `docker
  ps` responds normally again.

## Task List

- [ ] Layered image built, size checked (Step 1)
- [ ] Container started, startup time confirmed in logs (Step 2)
- [ ] AOT cache comparison run, both numbers recorded (Step 3)
- [ ] Full stack up via compose, order persisted to Postgres, inventory
      lookup cached in Redis (Step 4 — verified via the manual
      `docker run` equivalent, not yet re-confirmed as literal `docker
      compose up` — see the note in Step 4)
- [ ] Jib attempted, result (success or the specific failure) understood (Step 5)

## Free Response

In 75–125 words: the layered Dockerfile and a plain single-stage
`COPY app.jar` Dockerfile produce an image of roughly the *same total
size* — layering doesn't shrink anything. So what is layering actually
buying you, concretely, the next time someone changes one line of
`OrderController` and pushes? Walk through what Docker's build cache does
differently for the layered version vs. the single-COPY version on that
next build.

*(A good reflection response is between 75 and 125 words.)*
