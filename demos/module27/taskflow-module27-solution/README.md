# TaskFlow – Module 27 Demo Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, structured logs, Prometheus and Grafana monitoring, and a Docker image) that
can now be **deployed to Kubernetes**. Kubernetes keeps two copies running, replaces
any that die, and gives them one stable address.

## What's in `k8s/`

| File | Kind | What it does |
|------|------|--------------|
| `configmap.yaml` | ConfigMap | Non-sensitive settings: Vault's URI |
| `secret.yaml` | Secret | The Vault token |
| `deployment.yaml` | Deployment | Runs 2 replicas of the image, with health probes |
| `service.yaml` | Service (`ClusterIP`) | One stable internal address in front of the Pods |

### ConfigMap and Secret

Vault's URI is a location, like a hostname, so it isn't sensitive and belongs in the
ConfigMap. The token is sensitive, so it goes in the Secret. Both are injected into the
Pod as environment variables when it starts.

**A Kubernetes Secret is base64-encoded, not encrypted.** Anyone who can read the
Secret can decode it:

```bash
echo dGFza2Zsb3ctcm9vdC10b2tlbg== | base64 -d
```

Real encryption at rest is a separate, cluster-level concern (etcd encryption). Don't
treat "base64" as "secure," and never commit a real secret to a repository.

### Deployment

- **`replicas: 2`.** Kubernetes constantly works to keep exactly this many Pods
  running, and replaces any that die.
- **`imagePullPolicy: Never`.** This image is built locally and never pushed to a
  registry. Without this setting, Kubernetes tries to pull `taskflow-api:latest` from
  Docker Hub, fails, and every Pod stays in `ImagePullBackOff`.
- **`envFrom`.** Pulls in both the ConfigMap and the Secret.
- **Probes.** The liveness probe points at `/actuator/health/liveness` and the
  readiness probe at `/actuator/health/readiness`, the two endpoints from Module 23.
  Liveness failing makes Kubernetes restart the container. Readiness failing makes it
  stop sending traffic to that Pod.

### Service

`ClusterIP` makes the Service internal only. It gives the shifting, replaceable Pods one
address that never changes, and load-balances across whichever Pods are healthy.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- Spring Cloud 2025.1.2 (Vault and Config client)
- Maven
- Docker Desktop with its built-in Kubernetes
- `kubectl`

## Prerequisites

1. **Enable Kubernetes** in Docker Desktop: **Settings → Kubernetes → Enable
   Kubernetes**. The first start can take several minutes.
2. **Check that it's ready:**
   ```bash
   kubectl get nodes
   ```
   You should see one node with status `Ready`.
3. **Build the image locally**, from the project root. It must exist before the Pods
   can start:
   ```bash
   docker build -t taskflow-api:latest .
   ```
4. **Start Vault** and store the secret (see the Module 15 project). The Pods need it to
   start:
   ```bash
   docker compose up -d
   export VAULT_ADDR=http://localhost:8200
   export VAULT_TOKEN=taskflow-root-token
   vault kv put secret/taskflow partner.api-key=sk_live_hardcoded_demo_9f8e7d6c5b4a
   ```

Vault runs through Docker Compose, outside the cluster, so the Pods reach it through
`host.docker.internal`, set in `configmap.yaml`.

## Deploy

```bash
kubectl apply -f k8s/
kubectl get pods -w
```

Press Ctrl+C once both Pods show `Running` and `READY 1/1`.

## Try it

1. **Reach the Service.** In one terminal:
   ```bash
   kubectl port-forward service/taskflow-service 8080:8080
   ```
   `ClusterIP` isn't reachable from outside the cluster, and `port-forward` is the usual
   way for a developer to reach in locally.
2. **Check health.** In a second terminal:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
3. **Watch it heal itself.** Get a Pod's name and delete it:
   ```bash
   kubectl get pods
   kubectl delete pod <pod-name>
   ```
4. **Check again right away:**
   ```bash
   kubectl get pods
   ```
   A replacement Pod is already starting, with nobody telling it to, so the count goes
   back to 2.

## Clean up

```bash
kubectl delete -f k8s/
```

## Known limitation

**A full authenticated request doesn't work against the Pods yet**, for the same reason
as in Module 25. A JWT you get through Keycloak's host-facing URL
(`http://localhost:8081`) doesn't match the issuer the Pods expect, and the Pods have no
Keycloak address configured. Test with endpoints that don't need a token, such as
`/actuator/health` and `GET /api/v1/tasks`.

## What this doesn't cover

- **Ingress**, which is useful once you route between several services. This project
  has one.
- **LoadBalancer Services**, which need a cloud provider (or extra tooling) to mean
  anything locally.
- **The Horizontal Pod Autoscaler**, which needs `metrics-server` and a way to generate
  real CPU load.

## Project structure

```
.
├── k8s/
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   └── service.yaml
├── Dockerfile                                # Multi-stage build, non-root runtime
├── .dockerignore
├── docker-compose.yml                        # Supporting services
├── prometheus.yml
├── alerts.yml
├── scripts/import-logs.sh
├── pom.xml
└── src/main/
    ├── java/com/taskflow/
    │   ├── filter/CorrelationIdFilter.java
    │   ├── config/                           # SecurityConfig, PartnerApiKeyProperties, PartnerRequestProperties
    │   ├── controller/                       # Task, Account, Report, Config, Partner controllers
    │   ├── service/                          # TaskService, AccountService
    │   ├── repository/AccountRepository.java
    │   ├── model/                            # Task, Account
    │   ├── dto/AccountDTO.java
    │   ├── security/                         # Retired classes, kept for reference
    │   └── util/PasswordHashGenerator.java
    └── resources/
        ├── application.yml
        ├── data.sql
        └── static/demo-trigger.html
```

## Notes

- The Vault token in `secret.yaml` is a fixed demo value for local development only.
  Never commit a real secret to a repository.
- Tasks and accounts live in memory, and each Pod has its own copy, so the two Pods
  don't share data.
- Tasks and accounts are reset when a Pod restarts.
