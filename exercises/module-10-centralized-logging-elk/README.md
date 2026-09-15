# Exercise: Apply Centralized Logging with ELK

**Estimated time:** 30–40 minutes (longer than other modules — this one
involves standing up real infrastructure, not just editing code)

## A note on how this exercise is different

Every other module in this series is a starter/solution Spring Boot
project you build, run, and verify with `curl` in a couple of minutes.
This one isn't — it needs Elasticsearch, Kibana, and Filebeat running via
Docker Compose, and part of the deliverable (a Kibana dashboard and an
alert rule) is built by clicking through a UI, not by writing code. That
makes it closer to a **local build** exercise: everything you need is
here and verified to be correct where it can be (the Spring Boot config
compiles and runs; `docker-compose.yml` and `filebeat.yml` are syntax
and reference-checked), but actually standing up the stack and building
the dashboard is on you, locally, with Docker.

## The scenario

Udabank's `orderservice` and `inventoryservice` (same two services from
the correlation-ID module) log structured JSON to stdout, but that's
useless during an incident if nobody's watching the right terminal. Priya
wants logs from both services centralized in Elasticsearch with a Kibana
dashboard the on-call engineer can actually look at.

## Exercise Prerequisites

1. Docker Desktop (or another Docker Engine + Compose v2) installed and
   running.
2. At least ~4GB of RAM available to Docker — Elasticsearch and Kibana
   are not lightweight. If your machine is tight on resources, this is
   the module to skip or come back to later.
3. This directory (`module-10-centralized-logging-elk/`) — everything
   else needed is already here:
   - `docker-compose.yml` — the full stack: Elasticsearch, Kibana,
     Filebeat, and both Spring Boot services (built from their local
     `Dockerfile`s).
   - `filebeat.yml` — ships each container's stdout to Elasticsearch,
     parsing it as JSON (both services already log ECS-format JSON, from
     the correlation-ID module).
   - `orderservice/`, `inventoryservice/` — same two services as the
     correlation-ID module, plus one new endpoint (`/orders/{id}/expedite`)
     that's deliberately broken, for the "simulated incident" step below.

## Exercise Steps

1. **Stand up the stack.**
   ```bash
   docker compose up --build
   ```
   Wait for Kibana to log something like `http server running at
   http://0.0.0.0:5601` — Elasticsearch and Kibana both take a minute or
   two to become healthy, and `docker-compose.yml` has Kibana/Filebeat
   wait on Elasticsearch's healthcheck, so this should come up in the
   right order on its own.

2. **Confirm logs are actually arriving.** In a second terminal:
   ```bash
   curl http://localhost:8081/orders/ORD-1   # generates a log line in both services
   curl -s "http://localhost:9200/udabank-logs-*/_count" | python3 -m json.tool
   ```
   The count should be greater than 0 and climbing each time you hit the
   endpoint. If it's stuck at 0, see the Common Pitfalls section below
   before going further — nothing past this point will work until logs
   are actually landing in Elasticsearch.

3. **Create the data view in Kibana.** Open `http://localhost:5601`
   → Stack Management → Data Views (called "Index Patterns" in older
   Kibana versions) → Create data view → enter `udabank-logs-*` → select
   `@timestamp` as the time field. If Kibana instead shows an empty-state
   screen ("Add integration" / "Upload a file" / "Add sample data") with
   no "Create data view" button, it means no `udabank-logs-*` indices
   exist yet — see the Common Pitfalls entries below before continuing.

4. **Explore in Discover.** Kibana → Discover, select the `udabank-logs-*`
   pattern. You should see structured fields you can filter/sort on
   directly — `service.name`, `log.level`, `trace.id`, `message` — not
   just raw text. Try filtering `service.name : "orderservice"`.

5. **Build the dashboard.** Kibana → ☰ menu → **Analytics** (click the
   chevron next to it to expand the group — "Dashboards" is nested under
   there and easy to miss if you click the "Analytics" label itself,
   which just opens an overview page instead) → Dashboards → Create
   dashboard → Create visualization (Lens) for each panel:
   - **Error rate by service**: a bar chart — horizontal axis `@timestamp`
     (date histogram), vertical axis Count of records, breakdown
     `service.name` — with the Lens query bar set to
     `log.level : "ERROR"`.
   - **p95 response time**: this requires a numeric latency field, which
     these services don't currently emit — as a stand-in, build a count
     panel of requests per service over time instead (same axes as
     above, no `log.level` filter), and note in your Free Response below
     what you'd add to the services to make a real p95 panel possible
     (hint: a request-duration filter/interceptor logging a numeric
     field).
   - **Top error messages**: a data table — rows: Top values of
     `message` (see the pitfall below, you'll actually need
     `message.keyword`), metric: Count of records — with the query bar
     set to `log.level : "ERROR"`.

   Set the dashboard's time range (top right) to something wider than
   the default "Last 15 minutes" — e.g. "Last 24 hours" — or your panels
   will show "No results found" even once data exists, simply because
   the test traffic falls outside the default window.

6. **Simulate an incident.** With the dashboard open in one tab, run this
   in a terminal:
   ```bash
   for i in $(seq 1 30); do curl -s http://localhost:8081/orders/ORD-1/expedite > /dev/null; done
   ```
   Refresh the dashboard (or set it to auto-refresh). You should see the
   error-rate panel spike and `"surcharge-service unreachable"` show up
   as the top error message.

7. **Create an alert.** Kibana → Stack Management → Rules → Create rule
   → "Elasticsearch query" rule type → query
   `service.name: "orderservice" and log.level: "ERROR"` against index
   `udabank-logs-*` → condition: count is above some threshold (e.g. 10)
   over the last 5 minutes → action: whatever's available in your local
   Kibana setup (server log connector works without any extra setup, if
   you don't want to configure email/Slack).

## Common Pitfalls

- **Filebeat refuses to start.** Filebeat checks that `filebeat.yml` is
  only writable by its owner — `chmod 600 filebeat.yml` before running
  `docker compose up` (already set correctly on the copy in this repo,
  but if you edit and re-save it from some editors, permissions can
  reset). Check `docker compose logs filebeat` for
  `"config file can only be writable by the owner"` if logs aren't
  arriving.
- **No `udabank-logs-*` documents at all.** Check `docker compose logs
  filebeat` for connection errors to Elasticsearch, and confirm the
  `orderservice`/`inventoryservice` containers actually have the
  `co.elastic.logs/enabled=true` label picked up (`docker inspect
  <container> | grep co.elastic.logs`).
- **Logs arrive but `message` is the whole JSON blob, not parsed
  fields.** This means the `co.elastic.logs/json.keys_under_root: "true"`
  hint isn't being applied — double check the labels are on the
  container Filebeat is actually watching (`docker compose ps` to
  confirm container names match what you expect).
- **`docker compose ps` shows filebeat running, `docker compose logs
  filebeat` prints nothing at all, and the count stays at 0.** Filebeat
  8.15's own logs go to `/usr/share/filebeat/logs/*.ndjson` inside the
  container by default, not stdout — `docker compose logs` will look
  empty even when filebeat is actively erroring. Check with:
  ```bash
  docker exec <filebeat-container> sh -c "cat /usr/share/filebeat/logs/*.ndjson"
  ```
- **Hits the above and finds `"Loading and starting Inputs completed.
  Enabled inputs: 0"` plus `"Configuration template cannot be resolved:
  field 'data.kubernetes.container.id' not available"`.** This is a
  bug in filebeat 8.15's docker autodiscover: `hints.enabled: true`
  tries to resolve a Kubernetes-only template field even in plain
  single-node Docker mode, so it never builds an input for either
  service container — nothing is ever harvested. The label-based
  condition (`docker.container.labels.co_elastic_logs/enabled`) also
  doesn't reliably match in this autodiscover version. The fix used in
  this repo's `filebeat.yml`: drop `hints.enabled` and use an explicit
  `templates:` block that matches on container name instead
  (`contains: docker.container.name: "service"`, which catches both
  `orderservice` and `inventoryservice`).
- **Inputs start (harvesters running, per the log) but the count is
  still 0, with `"error loading template: failed to put data stream:
  ... no matching index template found for data stream"`.** Filebeat
  8.x defaults to provisioning a *data stream*, which conflicts with a
  dated classic index like `udabank-logs-%{+yyyy.MM.dd}` and breaks the
  Elasticsearch connection outright before any doc is indexed. Fix:
  explicitly add `setup.template.type: "index"` alongside
  `setup.template.name`/`pattern` to force classic index mode.
- **Kibana → Stack Management shows "Data Views" instead of "Index
  Patterns," and creating one from the top-level "no data" screen shows
  only "Add integration" / "Upload a file" / "Add sample data" with no
  "Create data view" button.** Two separate things here: (1) Kibana
  renamed "Index Patterns" to "Data Views" in 8.x — same feature. (2)
  The empty-state screen only appears when Elasticsearch has *no*
  indices matching what you type — it means no `udabank-logs-*` data
  has landed yet, not that the feature is missing. Fix the underlying
  ingestion problem (the filebeat pitfalls above) first, then the
  "Create data view" button appears normally.
- **Can't find "Dashboards" in the left nav at all.** It's nested under
  the collapsible **Analytics** group in the ☰ menu — click the chevron
  next to "Analytics" to expand it, not the "Analytics" label itself
  (that just navigates to an overview page).
- **"Top values" on the `message` field errors out or isn't offered as
  an option when building the "Top error messages" panel.** The
  `message` field here is mapped as `match_only_text` (a space-optimized
  text type used by the Filebeat/ECS index template) which has **no
  keyword sub-field and isn't aggregatable at all** — Lens can't bucket
  on it directly. Fix: add a runtime field on the data view (Stack
  Management → Data Views → your view → Add field) named
  `message.keyword`, type `Keyword`, with "Set value" script:
  ```painless
  emit(params._source['message'] != null ? params._source['message'] : '')
  ```
  Then use `message.keyword` (not `message`) as the Rows field in the
  data table.
- **After running the incident simulation (Step 6), the error-rate
  panel and Elasticsearch count still show 0 for a minute or two.** If
  filebeat's harvester has been idle (no new lines written to a
  container's log file for a while), it takes a bit to notice the file
  has grown again and re-read it. This is normal lag, not a broken
  pipeline — give it 10–15 seconds and re-run the count check
  (`curl -s "http://localhost:9200/udabank-logs-*/_count" -H
  "Content-Type: application/json" -d
  '{"query":{"term":{"log.level":"ERROR"}}}'`) before assuming something
  is wrong.

## Task List

- [ ] Stack is up: `docker compose ps` shows all 5 services running/healthy
- [ ] Logs are landing in Elasticsearch (Step 2's count check)
- [ ] Index pattern created, Discover tab shows structured fields
- [ ] Dashboard built with the 3 panels from Step 5
- [ ] Simulated incident (Step 6) visibly shows up as an error-rate spike
- [ ] Alert rule created (Step 7)

## Free Response

You ran the simulated incident in Step 6. In 75–125 words: what did the
dashboard actually tell you, and what didn't it tell you? Specifically —
could you tell *why* the surcharge lookup was failing from the dashboard
alone, or only *that* it was failing and *how often*? What's one thing
you'd add to these services' logging (a field, a panel, anything) that
would have gotten you closer to root cause without needing to read the
source code?

*(A good reflection response is between 75 and 125 words — a bit longer
than other modules', since there's more to observe here.)*

See [`docs/SOLUTION.md`](docs/SOLUTION.md) for one answer, and for the
module's own "reading the dashboard" walkthrough.
