# Exercise: Apply Centralized Configuration with Spring Cloud Config

**Estimated time:** 10–15 minutes

## The scenario

Udabank runs several backend services, each with its own `application.yml`.
Nobody can say for certain which value is live in prod without SSHing in
and checking — a classic "who changed this and when" problem. Priya wants
`orderservice` pulling its config from a central Config Server instead.

## What's here

- `config-server/` — given, not part of the exercise. A working Spring
  Cloud Config Server backed by the local Git repo in `config-repo/`.
- `config-repo/` — given. A real (local) Git repo with three files
  demonstrating precedence: `application.yml` (generic default),
  `orderservice.yml` (this app's default), `orderservice-dev.yml`
  (this app's dev-profile override).
- `orderservice/` — the exercise. Currently has `welcome.message`
  hardcoded locally instead of centralized.

## Your task

One file: [`orderservice/src/main/resources/application.yml`](orderservice/src/main/resources/application.yml).
The TODO comments there have the exact property and why it's needed.

## Running it

Two terminals:

```bash
# Terminal 1
cd config-server && ../../../mvnw spring-boot:run   # or: mvn spring-boot:run

# Terminal 2 (after the config server logs "Started ConfigServerApplication")
cd orderservice && mvn spring-boot:run
```

## Verifying your work

```bash
curl http://localhost:8081/message
# Right now: {"message":"Hardcoded local message — not centralized"}
# After your fix: {"message":"Welcome to Udabank Order Service"}
```

Then stop `orderservice` and restart it with the dev profile active, to
see precedence in action:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
curl http://localhost:8081/message
# {"message":"Welcome to Udabank Order Service — DEV environment"}
```

You can also query the config server directly to see exactly what it
resolves for a given app/profile, without going through the client at all:

```bash
curl http://localhost:8888/orderservice/dev
```
