# Exercise: Apply Kubernetes Deployment for a Spring Boot Service

**Estimated time:** 25–35 minutes

## A note on how this exercise was built

This is a guide, like the ELK and Prometheus modules — but for a more
specific reason than those two. Standing up a real cluster needs
`minikube` or `kind`, and while I have `kubectl` here, **neither
`minikube` nor `kind` is installed**, and separately, Docker Desktop (which
either tool would run on top of) was left in an unresponsive state by the
previous module's restart. So there was no live cluster available to
deploy to at all, regardless of how the exercise was scoped.

What I could still do, and did:
- Confirm the exact Actuator properties needed for `/actuator/health/liveness`
  and `/actuator/health/readiness` to work **by reading Spring Boot's own
  shipped configuration metadata** (`spring-configuration-metadata.json`
  inside the real `spring-boot-health-4.0.0.jar` on this machine) — not
  guessed, not copied from memory. Specifically:
  `management.endpoint.health.probes.enabled` defaults to `true` already,
  but `management.health.livenessstate.enabled` and
  `...readinessstate.enabled` default to **`false`** outside a
  Kubernetes-detected environment, so `orderservice/application.yml`
  explicitly sets both — without them, the probe *endpoints* would exist
  but report empty/meaningless status.
- Structurally validate every manifest in `k8s/` (`kind`, `apiVersion`,
  `metadata` all present and well-formed) — `kubectl --dry-run=client`
  itself hung indefinitely with no cluster configured, so this is a
  Python/YAML-level check, not `kubectl`'s own schema validation.
- Everything else here — the manifests actually deploying correctly, the
  probes actually passing, the rolling update actually working — is
  unverified. Genuinely go run this yourself if you want confidence
  beyond "it's well-formed YAML written by someone who's deployed to K8s
  before."

## The scenario

`orderservice` is now containerized (previous module) with real Postgres
and Redis dependencies. Priya's next ask: deploy it to Kubernetes
properly — multiple replicas behind a Service, non-sensitive config in a
ConfigMap, DB credentials in a Secret (not baked into the image or
hardcoded), and health probes so Kubernetes actually knows when a pod is
ready to receive traffic vs. when to restart it.

## Exercise Prerequisites

1. `kubectl` installed.
2. A local cluster: `minikube start` or `kind create cluster`. (Neither
   was available when this was built — see above.)
3. Docker, for building the images `kubectl` will reference.
4. Everything else is in this directory:
   - `orderservice/`, `inventoryservice/` — same two services from the
     Docker Packaging module (see that module for the Dockerfiles).
   - `k8s/` — all the manifests: ConfigMap, Secret, Deployment + Service
     for `orderservice` (2 replicas, with liveness/readiness probes),
     plus supporting Deployment/Service pairs for `inventoryservice`,
     Postgres, and Redis so the whole thing is actually runnable, not
     just `orderservice` in isolation with nothing to talk to.

## Exercise Steps

1. **Start your cluster and point Docker at it** (so images you build
   locally are visible to the cluster without pushing to a registry):
   ```bash
   minikube start
   eval $(minikube docker-env)
   # or, for kind: kind create cluster, then `kind load docker-image` after each build
   ```

2. **Build both images** (using the Dockerfiles from the Docker Packaging
   module):
   ```bash
   docker build -t udabank/orderservice:layered ./orderservice
   docker build -t udabank/inventoryservice:latest ./inventoryservice
   ```

3. **Apply the manifests:**
   ```bash
   kubectl apply -f k8s/orderservice-secret.yaml
   kubectl apply -f k8s/orderservice-configmap.yaml
   kubectl apply -f k8s/postgres.yaml
   kubectl apply -f k8s/redis.yaml
   kubectl apply -f k8s/inventoryservice-deployment.yaml
   kubectl apply -f k8s/orderservice-deployment.yaml
   kubectl apply -f k8s/orderservice-service.yaml
   ```

4. **Watch the pods come up, and watch the probes specifically:**
   ```bash
   kubectl get pods -w
   ```
   You should see 2 `orderservice` pods reach `Running` and `1/1 Ready` —
   not ready immediately, only after the readiness probe
   (`/actuator/health/readiness`) starts passing. If a pod sits at `0/1
   Ready` indefinitely, `kubectl describe pod <name>` will show the probe
   failures directly.

5. **Confirm it actually works** (port-forward, since this is a
   ClusterIP Service with no external access by design):
   ```bash
   kubectl port-forward svc/orderservice 8081:8081
   curl http://localhost:8081/orders/ORD-1
   ```

6. **Roll an update, and watch pod lifecycle.** Change something trivial
   in `OrderController` (a log message is enough), rebuild the image with
   a new tag, update the Deployment, and watch:
   ```bash
   docker build -t udabank/orderservice:layered-v2 ./orderservice
   kubectl set image deployment/orderservice orderservice=udabank/orderservice:layered-v2
   kubectl rollout status deployment/orderservice
   kubectl get pods -w
   ```
   Watch for: a new pod starting *before* an old one terminates (that's
   the default `RollingUpdate` strategy — no downtime, always at least 2
   pods serving), and the new pod not receiving traffic until its
   readiness probe passes.

## Common Pitfalls

- **Readiness/liveness probes return 404 or empty status**, even though
  `/actuator/health` itself works. This is almost always the
  `livenessstate`/`readinessstate` properties from the note above — the
  probe *paths* exist by default, but without those two properties set to
  `true`, there's no actual indicator contributing to them outside a
  cluster Spring Boot recognizes as Kubernetes.
- **`imagePullPolicy: IfNotPresent` matters** when using `minikube`'s
  Docker daemon (`eval $(minikube docker-env)`) or `kind load
  docker-image` — without it, Kubernetes defaults to `Always` for
  `:latest`-style tags and will try to pull from a real registry, which
  fails for an image that only exists locally.
- **The Secret uses `stringData`, not `data`.** If you add more secret
  values later, don't hand-compute base64 for `data:` — just add another
  key under `stringData:` and let the API server encode it.

## Task List

- [ ] Cluster running, images built and visible to it (Steps 1–2)
- [ ] All manifests applied, no errors (Step 3)
- [ ] Both `orderservice` pods reach `2/2 Running` / `Ready` (Step 4)
- [ ] `/orders/{id}` works through a port-forward (Step 5)
- [ ] Rolling update completed with zero dropped requests, observed pod
      lifecycle during the rollout (Step 6)

## Free Response

In 75–125 words: during the rolling update in Step 6, at what point does
Kubernetes send traffic to the *new* pod instead of the old one — and
what would happen to in-flight requests if the readiness probe passed
before the application was actually able to handle a real request (e.g.,
before its Redis connection pool was ready)? What's the practical
difference between the liveness probe's job and the readiness probe's
job in this scenario?

*(A good reflection response is between 75 and 125 words.)*

See [`docs/SOLUTION.md`](docs/SOLUTION.md) for one answer, and for the
module's own "rolling an update and observing pod lifecycle" walkthrough.
