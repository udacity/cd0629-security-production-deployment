# Solution: Apply Structured Logging with Correlation IDs

## What changed

`orderservice/TraceIdFilter.java` — same logic as `inventoryservice`'s:
extract `trace-id` from an incoming `traceparent` header if present,
generate one if not, always generate a fresh `span-id`, put all three
(`trace.id`, `span.id`, `correlation.id`) into MDC for the duration of the
request.

`orderservice/TraceparentPropagatingInterceptor.java` — reads the current
`trace.id` from MDC, generates a new `span.id` for this specific outbound
hop, and adds a `traceparent: 00-{trace-id}-{span-id}-01` header to the
call to `inventoryservice`.

## Verified end-to-end (both services actually running)

```
orderservice:     "message":"Handling order request for orderId=ORD-1",
                   "trace":{"id":"6da1ccb833c0beb93d643614c4e29666"},
                   "span":{"id":"fd2fb0fcc93f1b4a"}

inventoryservice: "message":"Checking inventory for sku=SKU-123",
                   "trace":{"id":"6da1ccb833c0beb93d643614c4e29666"},
                   "span":{"id":"6b899f1356ad74b5"}
```

Same `trace.id` in both, different `span.id` per hop — exactly W3C Trace
Context's model (one trace, one span per service that touches the
request).

## A real gotcha hit while building this

`RestClient.Builder` isn't auto-configured by `spring-boot-starter-web`
alone in Spring Boot 4 — it's a separate module now,
`spring-boot-restclient`, same modularization pattern as
`spring-boot-webmvc-test` from an earlier module. Without it,
`orderservice` fails at startup with "Parameter 0 of constructor in
InventoryClient required a bean of type 'RestClient$Builder' that could
not be found" — nothing to do with tracing, just a missing dependency.
Both `starter/orderservice/pom.xml` and this solution already have it, so
you shouldn't hit this — flagging it here in case you're adapting this
pattern into a different project and the same error shows up.

## Verifying

```bash
cd inventoryservice && mvn spring-boot:run
# in a second terminal
cd orderservice && mvn spring-boot:run
# in a third
curl http://localhost:8081/orders/ORD-1
```

Compare the `trace.id` field in both services' stdout.
