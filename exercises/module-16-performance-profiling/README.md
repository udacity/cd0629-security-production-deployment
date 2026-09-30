# Exercise: Apply Performance Profiling and Optimization

**Estimated time:** 25–30 minutes

## The scenario

`orderservice` — same service from every module in this series — has a
new endpoint, `GET /orders`, that lists every order with its line items
(an order-history page, basically). Priya's report: it's slow under real
traffic, even though a single request in isolation feels instant. Find
out why, using real profiling, not guesswork.

## A note on scope

Unlike the last several modules, this one **is** fully live-verified —
no Docker needed for the core exercise, since JFR profiling and load
testing just need a running JVM. Independently re-run since: the N+1
plus undersized pool reproduced the same shape of failure (p99 in the
thousands of ms, throughput falling short of the 200 rps target), the
JFR evidence trail (`ConcurrentBag.borrow` showing up in roughly 1 in 4
`ThreadPark` events) reproduced too, and applying the documented fix
produced an even larger improvement in that run — p99 dropped from
8,467ms to 21ms, a ~400× improvement, with the exact same two-line fix
described below. Exact numbers vary by machine, but the shape of the
result — dramatic, not marginal — holds. One substitution, stated
plainly: the
database is H2 in-memory instead of Postgres, purely so the whole thing
runs without Docker (which has been unavailable this session) — not
because H2 is realistic for a real profiling exercise. Part (c) of the
original brief (a Terraform/LocalStack touchpoint) is skipped — it
needs Docker for LocalStack, and it's a small, disconnected add-on to
the actual profiling work, not worth blocking the rest of the exercise
over. If you want it: a one-resource Terraform snippet (an S3 bucket) is
exactly what the brief describes, and `terraform apply` against
LocalStack once Docker's available is standard from there.

## The bug (don't peek at the code first)

Two things, stacked together — see [`docs/SOLUTION.md`](docs/SOLUTION.md)
for what they actually are and the real data that found them. Try to find
them yourself first with the steps below.

## starter/ vs solution/

This exercise, like the earlier modules in this series, ships as two
separate copies of `orderservice`:
- **`starter/orderservice/`** — the buggy version. This is what Steps
  1–3 below run against, to find the problem for real.
- **`solution/orderservice/`** — the exact same code, with the fix from
  `docs/SOLUTION.md` already applied. Use this for Step 4 instead of
  editing `starter/` in place — it means "compare two known states"
  instead of "live-edit code and hope it's right," and it's what lets
  you jump straight to re-measuring without retyping the fix yourself.

Every command below that says `mvn ...` or `java ...` needs to run from
inside whichever of those two directories is relevant to that step —
`cd starter/orderservice` or `cd solution/orderservice` first.

## Exercise Steps

1. **Run it and hit it once, normally** (from `starter/orderservice/`):
   ```bash
   mvn spring-boot:run
   curl http://localhost:8081/orders   # feels instant, ~500 orders, seeded on startup
   ```

2. **Load-test it with Gatling** (from `starter/orderservice/`, already
   configured — `src/test/java/simulations/OrdersListSimulation.java`,
   200 requests/sec for 30s):
   ```bash
   mvn gatling:test -Dgatling.simulationClass=simulations.OrdersListSimulation
   ```
   Run it **twice** — discard the first run's numbers (JIT warm-up skews
   them badly) and treat the second as your real baseline. Note the p99
   latency and whether throughput actually hit 200 rps or fell short.

3. **Capture a JFR recording while under load** (still in
   `starter/orderservice/`) — build a jar first (neither Step 1 nor 2
   packages one), then restart the app with recording on, then repeat
   Step 2's load test against this instance:
   ```bash
   mvn clean package -DskipTests
   java -XX:StartFlightRecording=filename=recording.jfr,settings=profile \
        -jar target/orderservice-0.0.1-SNAPSHOT.jar
   ```
   After stopping the app (so the recording flushes):
   ```bash
   jfr summary recording.jfr | grep ThreadPark
   jfr print --events jdk.ThreadPark --stack-depth 10 recording.jfr | grep -B8 "ConcurrentBag.borrow"
   ```
   `ConcurrentBag.borrow` showing up repeatedly, on request-handling
   (`http-nio-*`) threads, is your first real clue.

4. **Compare against `solution/orderservice/`.** Read the diff between
   `starter/orderservice/src/main/java/com/udabank/orderservice/PlacedOrderRepository.java`
   and the same file under `solution/`, plus `application.yml`'s
   `maximum-pool-size`, to see exactly what changed. Then `cd
   solution/orderservice`, build and run it the same way as Step 3, and
   re-run Step 2's load test against it. The improvement should be
   dramatic and immediate, not marginal.

5. **Compare G1 vs generational ZGC** on `solution/orderservice/`:
   ```bash
   java -XX:+UseG1GC -XX:StartFlightRecording=filename=g1.jfr,settings=profile -jar target/*.jar
   # load test, note p99, stop, then:
   java -XX:+UseZGC -XX:StartFlightRecording=filename=zgc.jfr,settings=profile -jar target/*.jar
   # load test again, compare p99 AND compare GC pause counts:
   jfr summary g1.jfr | grep -i GarbageCollection
   jfr summary zgc.jfr | grep -i GarbageCollection
   ```
   Note: on JDK 25, `-XX:+UseZGC` alone gives you generational ZGC — the
   old `-XX:+ZGenerational` flag was removed in JDK 24 and will make the
   JVM print a warning and ignore it if you pass it.

## Common Pitfalls

- **`zsh: command not found: jfr`.** `jfr` is a command-line tool
  bundled inside every JDK install, but macOS doesn't always put it on
  your terminal's PATH the way it does for `java`. Fix:
  `export PATH="$(/usr/libexec/java_home -v 25)/bin:$PATH"` once per
  terminal session, then `jfr` will work.
- **`./mvnw: not found`.** Same gap as every other module in this
  series — the Maven wrapper was never committed. Every command above
  uses plain `mvn` instead, which needs Maven and a JDK matching
  `pom.xml` (25) actually installed. Confirmed working with Java 25
  specifically — `mvn -version` should report that under "Java
  version," not whatever your system's default JDK happens to be.
- **The first Gatling run's numbers look better than the second.** This
  is expected, not a fluke — the JVM hasn't JIT-warmed yet on the first
  run, and H2's in-memory tables are freshly created with no page-cache
  history. The README's "discard the first run" instruction exists
  because of this; independently confirmed while verifying this
  exercise — the second run consistently showed *worse* p99 than the
  first, not better, because the real bottleneck (HikariCP pool
  starvation compounding the N+1) needs sustained load to fully show
  up.

## Task List

- [ ] Baseline p99 measured (after discarding the first warm-up run)
- [ ] JFR recording captured, `ConcurrentBag.borrow` found in the stack traces
- [ ] Both parts of the bug identified and fixed
- [ ] Re-measured p99 shows a dramatic improvement, not a marginal one
- [ ] G1 vs ZGC comparison run, pause counts noted

