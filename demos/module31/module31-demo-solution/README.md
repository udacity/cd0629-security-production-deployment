# TaskFlow – Module 31 Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, structured logs, Prometheus and Grafana monitoring, a Docker image, unit
tests and a Jenkins pipeline) that deploys using **blue-green** on Kubernetes and
manages its database schema with **Flyway migrations**.

## What's in place

| Feature | Where |
|---------|-------|
| Versioned database migrations | `src/main/resources/db/migration/` |
| Hibernate only validates the schema | `ddl-auto: validate` in `application.yml` |
| Blue and green environments | `k8s/deployment-blue.yaml` and `k8s/deployment-green.yaml` |
| Traffic switch | The `version` label in `k8s/service.yaml`'s selector |
| Pipeline stages for green | `Deploy Green`, `Verify Health` and `Promote` in the `Jenkinsfile` |

### Flyway migrations

Flyway owns the schema. Hibernate's `ddl-auto: validate` only checks that the `Account`
entity matches what Flyway created. Letting both create the schema would make them fight
over who is in charge.

| Migration | What it does |
|-----------|--------------|
| `V1__create_account_table.sql` | Creates the `account` table |
| `V2__seed_demo_accounts.sql` | Inserts the three demo accounts (these used to live in `data.sql`) |
| `V3__add_account_region_column.sql` | Adds a nullable `region` column |

Seed data is a migration too, so it is tracked and ordered like any other change. `V3` is
the **expand** half of the expand-and-contract pattern: it is nullable and backward
compatible, so an old version of the code never notices it, and a new version can use it
straight away. The **contract** half, dropping an old column once nothing reads it, isn't
built here, because it only makes sense well after the new version has been live.

`pom.xml` uses `spring-boot-starter-flyway`. Spring Boot 4.x no longer auto-configures
Flyway from `flyway-core` alone, so with only that dependency, migrations silently never
run.

### Blue-green deployment

Both Deployments carry the label `app: taskflow-api`. Each also has its own `version`
label:

| Deployment | Label | Image |
|------------|-------|-------|
| `taskflow-api-blue` | `version: blue` | `taskflow-api:v1` |
| `taskflow-api-green` | `version: green` | `taskflow-api:v2` |

`k8s/service.yaml` selects `version: blue`, so the Service sends traffic to blue's Pods
only, even while green is fully running. **Changing that one value to `green` is the
entire traffic switch.** It takes effect almost instantly. To roll back, change it back.
Blue is never touched or scaled down, so rollback needs no redeploy and no rebuild.

Green uses a rebuilt copy of the same code, tagged `v2`, so the exercise proves the
switching mechanism. A real green deployment would carry actual changes.

### The pipeline

The `Jenkinsfile` extends the Module 29 pipeline:

```
Checkout → Build → Test → Package → Docker Build → Deploy Green → Verify Health → Promote
```

| Stage | What it does |
|-------|--------------|
| Docker Build | Builds `taskflow-api:v2` |
| Deploy Green | Applies `deployment-green.yaml` and waits for the rollout |
| Verify Health | Port-forwards to green directly, bypassing the Service, and checks `/actuator/health/readiness` before green gets any traffic |
| Promote | Patches the Service selector to `green` |

The pipeline shows the same steps you run by hand below. **The deploy stages need
`kubectl` and access to your cluster inside the Jenkins controller**, which the Jenkins
setup in this project doesn't provide. Treat them as a reference for how the by-hand
steps map into a pipeline, and do the cutover with `kubectl` yourself.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Flyway (`spring-boot-starter-flyway`)
- H2 in-memory database
- Maven
- Docker, Kubernetes and Jenkins

## Prerequisites

- Docker Desktop with Kubernetes enabled (`kubectl get nodes` shows a `Ready` node)
- Vault running with the secret stored (see the Module 15 project):

```bash
docker compose up -d
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=taskflow-root-token
vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
```

## Check the migrations

Run the app natively:

```bash
mvn spring-boot:run
```

```bash
curl "http://localhost:8080/api/v1/accounts/search?name=dev1"
```

You get the same `dev1` account as before, now created by Flyway migrations instead of
Hibernate and `data.sql`. The startup log also lists the Flyway migrations it applied.

## Try blue-green

1. **Build the blue image** with an explicit version tag, not `latest`:
   ```bash
   docker build -t taskflow-api:v1 .
   ```
2. **Deploy blue**, with the ConfigMap, Secret and Service. Green comes later:
   ```bash
   kubectl apply -f k8s/configmap.yaml -f k8s/secret.yaml -f k8s/deployment-blue.yaml -f k8s/service.yaml
   kubectl get pods
   ```
3. **Reach the Service** in one terminal:
   ```bash
   kubectl port-forward service/taskflow-service 8080:8080
   ```
   In another terminal, check that it answers:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
4. **Build the green image and deploy it** next to blue:
   ```bash
   docker build -t taskflow-api:v2 .
   kubectl apply -f k8s/deployment-green.yaml
   kubectl get pods
   ```
   Four Pods run now, two blue and two green. The Service still routes only to blue,
   and the health check on port 8080 is unaffected.
5. **Verify green directly**, bypassing the Service:
   ```bash
   kubectl port-forward deployment/taskflow-api-green 18080:8080
   ```
   In another terminal:
   ```bash
   curl http://localhost:18080/actuator/health/readiness
   ```
6. **Cut over** with one command:
   ```bash
   kubectl patch service taskflow-service -p '{"spec":{"selector":{"app":"taskflow-api","version":"green"}}}'
   ```
7. **Check the original URL.** `curl http://localhost:8080/actuator/health` on the same
   port-forward is now answered by green. Nothing about the client changed.
   (A running `port-forward` to a Service keeps its connection to the Pod it started
   with, so if it still seems to hit blue, stop it and start it again.)
8. **Roll back** by flipping the selector value back to `blue`:
   ```bash
   kubectl patch service taskflow-service -p '{"spec":{"selector":{"app":"taskflow-api","version":"blue"}}}'
   ```
   It is immediate. Blue was never changed or scaled down.

## Clean up

```bash
kubectl delete -f k8s/
```

## Project structure

```
.
├── Jenkinsfile                               # Includes Deploy Green, Verify Health, Promote
├── jenkins/                                  # Jenkins controller image
├── k8s/
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment-blue.yaml                  # version: blue, image v1
│   ├── deployment-green.yaml                 # version: green, image v2
│   └── service.yaml                          # Selector picks blue or green
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/taskflow/                # Application code
    │   └── resources/
    │       ├── application.yml               # ddl-auto: validate
    │       ├── db/migration/                 # V1, V2, V3 Flyway migrations
    │       └── static/demo-trigger.html
    └── test/java/com/taskflow/service/
        └── TaskServiceTest.java
```

## What this doesn't cover

- **Rolling and canary deployments.** Only blue-green is built here.
- **The contract step** of expand-and-contract.
- **Automated promotion and rollback in the pipeline.** The cutover here is a manual
  `kubectl` command.

## Notes

- Each Pod runs its own in-memory H2 database, and Flyway runs in each one on startup. In
  a real system, blue and green would share one external database, which is exactly why
  schema changes must stay backward compatible.
- A blue-green deployment runs two full copies of the app at once, so it uses twice the
  resources while both are up.
- `k8s/secret.yaml` holds a base64-encoded fake demo token. Never commit a real secret
  to a repository.
- Tasks and accounts reset when a Pod restarts.
