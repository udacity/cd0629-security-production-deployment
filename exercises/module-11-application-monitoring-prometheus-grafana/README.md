# Exercise: Apply Application Monitoring with Prometheus and Grafana

**Estimated time:** 30–40 minutes

## A note on how this exercise is different

Same shape as the ELK module: this needs real infrastructure (Prometheus,
Grafana, Alertmanager, an OpenTelemetry Collector) via Docker Compose, and
part of the deliverable — reading a Grafana dashboard, watching an alert
fire — is observation, not code. What's verified here: `orderservice`'s
custom metrics (`orders_placed_total`, `order_processing_seconds`) are
real, compiled, and confirmed against a running instance's actual
`/actuator/prometheus` output — not guessed at. The Grafana dashboard
(`grafana/dashboards/udabank-orders.json`) uses those exact, verified
metric names and labels, and is wired up for automatic provisioning, so
it should load correctly the first time you bring the stack up — but I
haven't run Grafana itself to confirm the panels render, since that needs
the full stack live.

## The scenario

`orderservice` and `inventoryservice` (same two services from the
correlation-ID and ELK modules) have no visibility into request rate,
latency, or JVM health beyond reading log lines one at a time. Priya wants
a Grafana dashboard the on-call engineer can glance at, plus an alert
that pages before a slow degradation becomes an outage.

## Exercise Prerequisites

1. Docker Desktop (or Docker Engine + Compose v2) installed and running.
2. Everything else is in this directory already:
   - `docker-compose.yml` — Prometheus, Grafana, Alertmanager, an OTel
     Collector, and both Spring Boot services.
   - `orderservice/` — now exposes `/actuator/prometheus`, exports OTLP
     metrics/traces to the collector, and has two custom metrics wired
     into `OrderController`: a `orders_placed_total` counter and an
     Observation-instrumented `order_processing_seconds` timer (see
     `OrderMetrics.java`).
   - `prometheus/alert_rules.yml` — a latency SLO-burn alert, already
     written against the real metric name.
   - `grafana/dashboards/udabank-orders.json` — a 4-panel dashboard,
     auto-provisioned on startup.

## Exercise Steps

1. **Stand up the stack.**
   ```bash
   docker compose up --build
   ```

2. **Generate some traffic** so there's data to look at:
   ```bash
   for i in $(seq 1 20); do curl -s http://localhost:8081/orders/ORD-$i > /dev/null; sleep 1; done
   ```

3. **Confirm metrics are flowing** two ways — direct scrape and via the
   OTLP collector:
   ```bash
   curl -s http://localhost:8081/actuator/prometheus | grep orders_placed_total
   curl -s http://localhost:9090/api/v1/query?query=orders_placed_total | python3 -m json.tool
   ```

4. **Open the dashboard.** `http://localhost:3000` (anonymous access is
   enabled for this exercise — no login needed) → Dashboards → Udabank
   folder → "Udabank - orderservice". You should see all 4 panels with
   real data from Step 2's traffic: JVM memory, request rate, p95
   latency, orders placed per minute.

5. **Check the alert rule loaded.** `http://localhost:9090/alerts` —
   `OrderProcessingLatencySLOBurn` should be listed in state "Inactive"
   (defined and evaluating, just not firing yet).

6. **Simulate an incident.** Generate sustained slow traffic:
   ```bash
   for i in $(seq 1 40); do curl -s http://localhost:8081/orders/ORD-$i/slow > /dev/null; done
   ```
   Watch the p95 latency panel in Grafana climb, and after ~2 minutes of
   sustained latency above the 500ms threshold, check
   `http://localhost:9090/alerts` again — `OrderProcessingLatencySLOBurn`
   should move to "Pending" and then "Firing". Once firing, check
   `http://localhost:9093` (Alertmanager) — it should show the same alert.

## Common Pitfalls

- **Dashboard panels show "No data."** Almost always means Prometheus
  isn't scraping successfully yet — check
  `http://localhost:9090/targets`; both `orderservice` and
  `inventoryservice` should show as "UP". If they're down, the services
  probably haven't finished starting when Prometheus first tried — give
  it another scrape interval (5s) or restart the `prometheus` container.
- **`order_processing_seconds_bucket` doesn't exist / p95 panel is
  empty.** This metric only gets histogram buckets because of
  `management.metrics.distribution.percentiles-histogram.order_processing: true`
  in `application.yml` — the property key has to match the metric name
  with underscores (`order_processing`), not dots (`order.processing`);
  the dotted form silently does nothing, no error, which is an easy trap.
- **Alert never fires even under Step 6's load.** The rule requires the
  condition to hold for a full 2 minutes (`for: 2m`) — a short burst of
  slow requests that stops before 2 minutes elapses won't trigger it.
  Keep the load loop running longer, or lower `for:` in
  `prometheus/alert_rules.yml` temporarily to see it fire faster.

## Task List

- [ ] Stack is up: `docker compose ps` shows all 6 services running
- [ ] Metrics confirmed flowing both via direct scrape and via the OTLP
      collector (Step 3)
- [ ] Dashboard loads with real data in all 4 panels (Step 4)
- [ ] Alert rule visible in Prometheus (Step 5)
- [ ] Simulated incident (Step 6) moves the alert from Inactive → Pending → Firing

## Free Response

In 75–125 words: the alert fires 2 minutes into sustained latency — not
immediately. What's the tradeoff `for: 2m` is making, and why might a
team choose 2 minutes over, say, 30 seconds or 10 minutes? What would you
look at on the dashboard, in the 2 minutes before the alert fires, to
decide whether to start responding early or wait for the page?

*(A good reflection response is between 75 and 125 words.)*