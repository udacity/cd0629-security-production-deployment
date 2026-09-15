# Exercise: Apply Blue-Green Deployment with Jenkins and Flyway

**Estimated time:** 35–45 minutes — the most involved module in this
series, combining CI/CD (module 14), Kubernetes (module 13), and a real
schema migration into one coordinated rollout.

## A note on how this exercise was built

Unlike modules 13–14, this one has actually been run live, end to end,
against a real Jenkins instance, a real minikube cluster, and — the
part that matters most here — a real Postgres database that already
had a live row in it before the migration ran. Specifically verified:
blue deployed and served traffic; the Flyway migration ran against
that live database while blue kept serving 100% of requests undisturbed;
the pre-existing order blue had already written got its `product_sku`
backfilled correctly, not just new rows; green passed its smoke test;
the traffic switch happened with a curl loop running the entire time
and every single request came back `200`, confirming the zero-downtime
claim isn't just theoretical; blue was decommissioned; and the rollback
pipeline was confirmed to actually restore blue and flip the Service
selector back. Three real problems came up along the way, none of them
about the blue-green concept itself — missing `mvnw`, Postgres only
being reachable from inside the cluster, and a container registry that
doesn't exist — and the fixes for all three are the same pattern
established in module 14's Jenkinsfile.

## The scenario

`orderservice`'s `placed_orders.sku` column needs renaming to
`product_sku` — a small clarity improvement that's actually a live
production database column, which makes it non-trivial. Priya wants zero
downtime and, ideally, a rollback path that doesn't involve restoring
from backup if it goes wrong partway through.

## What's here

- `orderservice-blue/` — the **unmodified** current app (identical to
  the end of module 14). Still reads and writes only `sku`. Never
  touched by this exercise's migrations.
- `orderservice-green/` — the v2 app: dual-writes both `sku` and
  `product_sku` on every insert, and now uses Flyway
  (`orderservice-green/pom.xml`'s `flyway-maven-plugin`) instead of
  Hibernate `ddl-auto` to manage schema changes.
- `orderservice-green/src/main/resources/db/migration/` — `V2` (add the
  column) and `V3` (backfill existing rows), the "expand" half of
  expand-contract. (`V4`/dropping `sku` is the "contract" half — see
  "What this exercise doesn't do" below.)
- `k8s/` — blue and green as **separate** Deployments, running
  simultaneously, plus one Service whose `selector.version` field is the
  entire traffic switch.
- `Jenkinsfile` — build → migrate → deploy green → smoke-test green
  (bypassing the Service, hitting green's pods directly) → **pause for
  approval** → switch the Service selector → scale blue to 0 (not
  delete).
- `Jenkinsfile.rollback` — a **separate** pipeline (see its header
  comment for why) that scales blue back up and flips the selector back.

## Exercise Prerequisites

Same Jenkins instance and cluster as module 14 — no new tooling needed,
`docker`, `buildx`, `mvn`, a matching JDK, `kubectl`, and a working
kubeconfig should already be set up there. This exercise needs exactly
one Jenkins Credential beyond what module 14 already required:
1. `staging-db-credentials` — a Jenkins Credential (Username/password)
   for the migration stage. In this repo's k8s Secret, that's
   `orderservice` / `orderservice` — same values, just also registered
   as a Jenkins Credential so the pipeline can use them without them
   being hardcoded into the Jenkinsfile.
2. A **Multibranch Pipeline** job pointed at this repo (same pattern as
   module 14), for the main `Jenkinsfile`.
3. A second, plain **Pipeline** job (not multibranch — it doesn't need
   to react to branch pushes) pointed at `Jenkinsfile.rollback`,
   manual-trigger only.

## Exercise Steps

1. **Deploy blue first**, as the known-good starting state:
   ```bash
   kubectl apply -f k8s/orderservice-configmap.yaml
   kubectl apply -f k8s/orderservice-secret.yaml
   kubectl apply -f k8s/postgres.yaml
   kubectl apply -f k8s/redis.yaml
   kubectl apply -f k8s/inventoryservice-deployment.yaml
   kubectl apply -f k8s/orderservice-blue-deployment.yaml
   kubectl apply -f k8s/orderservice-service.yaml
   curl (through a port-forward) http://localhost:8081/orders/ORD-1
   ```

2. **Trigger the main Jenkinsfile on `main`.** Watch it run migrations
   (`V2`, `V3`) against the live database while blue keeps serving 100%
   of traffic — check `kubectl get pods` during this stage and confirm
   blue's pods never restart or show any disruption.

3. **Watch the smoke test hit green directly**, not through the Service —
   confirm this in the Jenkins console output (`kubectl port-forward
   deployment/orderservice-green ...`, not `svc/orderservice`).

4. **Approve the traffic switch** when prompted. Immediately after,
   `kubectl get svc orderservice -o jsonpath='{.spec.selector}'` should
   show `version: green`.

5. **Confirm zero dropped requests during the switch** — run a tight curl
   loop against the Service (not a pod directly) spanning the moment you
   click "Switch," and confirm nothing 5xxs or times out. A Service
   selector patch is close to instantaneous at the `kube-proxy` level,
   which is the whole appeal of this approach over, say, redeploying
   with a new image tag.

6. **Run the rollback pipeline** and confirm it actually restores blue —
   `kubectl get pods` should show blue back at 2 replicas and the Service
   selector back to `blue`.

## Common Pitfalls

- **`./mvnw: not found`.** Same gap as module 14 — the Maven wrapper was
  never committed to either `orderservice-blue/` or `orderservice-green/`.
  Both the `Build and Test` and `Run Flyway Migration` stages call plain
  `mvn` instead, which needs Maven and a matching JDK actually present
  wherever the pipeline's shell steps run.
- **The Flyway migration stage can't reach Postgres.** Postgres only has
  a cluster-internal DNS name (`postgres`), resolvable from inside pods,
  not from wherever Jenkins itself runs. The migration stage
  port-forwards Postgres out of the cluster first
  (`kubectl port-forward svc/postgres 15432:5432 &`) and points
  `flyway.url` at `localhost:15432` — the same shape of fix as the smoke
  test stage already used for reaching green directly.
- **`docker push` fails or hangs against `registry.udabank.example`.**
  That hostname doesn't exist anywhere — same fictional-registry issue
  as module 14's original Jenkinsfile. Fixed the same way: build locally
  and copy the image straight into the cluster's own runtime with
  `docker save | docker exec -i minikube docker load`, no registry
  involved.
- **No webhook from GitHub to this Jenkins instance.** It's running on
  `localhost`, unreachable from GitHub's servers — pushing a commit
  does not trigger a build by itself. Click **Scan Repository Now** on
  the job after every push.

## Task List

- [ ] Blue deployed and confirmed working as the baseline (Step 1)
- [ ] Migration runs cleanly while blue serves traffic undisturbed (Step 2)
- [ ] Smoke test confirmed to hit only green (Step 3)
- [ ] Traffic switch confirmed via the Service selector (Step 4)
- [ ] Zero dropped requests measured during the switch (Step 5)
- [ ] Rollback pipeline tested and confirmed working (Step 6)

## Free Response

In 75–125 words: name one migration-ordering mistake that would pass
this exercise's smoke test (Step 3) but still cause real user-facing
errors once traffic actually switches to green at scale. Why would the
smoke test miss it?

*(A good reflection response is between 75 and 125 words.)*


## What this exercise doesn't do

Only the "expand" half of expand-contract is built here — adding the
column and backfilling it. The "contract" half (a v3 app that reads
`product_sku` exclusively, followed by a final migration dropping `sku`)
is a **second**, separate blue-green cycle that follows exactly the same
pattern demonstrated here, run only after enough time has passed to be
confident nothing is still depending on the old column. Building it out
wasn't essential to demonstrating the pattern and would have doubled
this exercise's size for limited additional teaching value — but it's
worth knowing that a real expand-contract migration is rarely "done" in
one deploy.
