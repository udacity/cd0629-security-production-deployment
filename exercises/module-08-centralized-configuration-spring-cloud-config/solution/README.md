# Solution: Apply Centralized Configuration with Spring Cloud Config

## What changed

[`orderservice/application.yml`](orderservice/src/main/resources/application.yml)
went from a hardcoded `welcome:` block to:

```yaml
spring:
  application:
    name: orderservice
  config:
    import: "optional:configserver:http://localhost:8888"
```

`spring.application.name: orderservice` is what tells the config server
which app-specific files to layer in — nothing else in this file needs to
reference `orderservice.yml` by name.

## Solution walkthrough: reviewing precedence rules

Verified end-to-end (config server + client both actually running, not
just inspected):

| Request | File it resolves to | Message |
|---|---|---|
| `GET /orderservice/default` (or the client, no profile) | `orderservice.yml` | "Welcome to Udabank Order Service" |
| `GET /orderservice/dev` (or the client, `dev` profile active) | `orderservice-dev.yml` | "Welcome to Udabank Order Service — DEV environment" |

The precedence order (most specific wins): `{app}-{profile}.yml` >
`{app}.yml` > `application-{profile}.yml` > `application.yml`. The config
server's own response (`curl http://localhost:8888/orderservice/dev`)
lists every property source it layered together, in that priority order —
that's the fastest way to answer "why did I get this value" without
guessing.

## A real gotcha hit while building this

Just having `spring-cloud-starter-config` on the classpath makes Spring
Boot **refuse to start at all** unless `spring.config.import` mentions
`configserver:` somewhere — there's a startup check for exactly this,
with its own error message pointing at
`spring.cloud.config.import-check.enabled=false` as the escape hatch. The
starter project uses that flag so it boots today with a hardcoded value;
part of the fix here is removing it now that `spring.config.import` is
actually set. (This is the same shape of gotcha as
`spring.cloud.vault.enabled` from the Vault module — Spring Cloud
components tend to activate hard as soon as they're on the classpath,
regardless of whether you've finished configuring them.)

## Natural next step (not part of this exercise)

The real brief for this module also covers `/actuator/refresh` — updating
a value in `config-repo` and pushing that change to a *running* service
without a restart. Worth knowing it exists (add
`spring-boot-starter-actuator`, expose the `refresh` endpoint, annotate
any `@Value`-injected bean with `@RefreshScope`, then `POST
/actuator/refresh`), but it's a genuinely separate moving part from "wire
a client to a config server," which is why it's not part of this pass.
