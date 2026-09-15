# Exercise: Apply Structured Logging with Correlation IDs

**Estimated time:** 10–20 minutes

## The scenario

Udabank's `orderservice` calls `inventoryservice` on every order lookup.
When something goes wrong in production, there's no way to find the one
`inventoryservice` log line that belongs to a specific `orderservice`
request — each service's logs are an island. You're wiring in trace
propagation so one request produces a shared trace ID across both.

## What's here

- `inventoryservice/` — given, not part of the exercise. Already logs in
  ECS JSON format and already has a working `TraceIdFilter` — this is
  your worked example.
- `orderservice/` — the exercise. Calls `inventoryservice` for every
  order, but right now nothing ties the two services' logs together.

## Your tasks

1. [`orderservice/.../TraceIdFilter.java`](orderservice/src/main/java/com/udabank/orderservice/TraceIdFilter.java) —
   same pattern as `inventoryservice`'s (already working) filter of the
   same name. Mirror it.
2. [`orderservice/.../TraceparentPropagatingInterceptor.java`](orderservice/src/main/java/com/udabank/orderservice/TraceparentPropagatingInterceptor.java) —
   the actual new skill: propagate the current trace onto the outbound
   call to `inventoryservice`, so its `TraceIdFilter` continues the same
   trace instead of starting a new one.

## Running it

Two terminals:

```bash
# Terminal 1
cd inventoryservice && mvn spring-boot:run

# Terminal 2
cd orderservice && mvn spring-boot:run
```

## Verifying your work

```bash
curl http://localhost:8081/orders/ORD-1
```

Then check both services' stdout. Right now (before your fix):
`orderservice`'s log line for this request has no `trace`/`span`/
`correlation` fields at all, and `inventoryservice` generates its own
unrelated trace ID. After your fix, both services' log lines for the same
request show the **same** `trace.id` (and `correlation.id`) — with a
different `span.id` per hop, since each service is its own step in the
trace.

```bash
grep "Handling order" # in orderservice's output
grep "Checking inventory" # in inventoryservice's output
# compare the trace.id field in both
```
