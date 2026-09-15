# Exercise: Apply CI/CD with Jenkins


## A note on how this exercise was built

This one *has* been run live, end to end, on a real Jenkins instance
against a real minikube cluster — including watching the pipeline fail,
fixing what broke, and watching it succeed. Three real problems came up
along the way, none of them Jenkinsfile syntax errors: the base Jenkins
image is missing `docker`, `mvn`, and a JDK new enough for this project;
the mounted Docker socket needs its permissions opened up before the
`jenkins` user can actually use it; and getting an image into a
Kubernetes cluster or a working kubeconfig into Jenkins both run into
the same root issue — tools like `minikube image load` assume they're
running on the same machine as the cluster, which isn't true when
Jenkins is its own separate container. 

The other thing worth knowing: this exercise was deliberately
simplified to need only **one** Jenkins Credential instead of three.
The original design needed an NVD API key, a container registry login,
and a kubeconfig upload — two of those were setup friction that had
nothing to do with the actual lesson (branch gating, an approval gate,
a failure notification), so they're gone. The one that's left, an NVD
key, genuinely can't be removed — confirmed directly by trying to run
the security scan without one: it doesn't just get slower, it fails
outright, since the tool has no vulnerability data to check against at
all on a fresh instance. See the Jenkinsfile's header comment for
exactly what changed and why.

## The scenario

`orderservice` has been containerized (module 12) and manually deployed
to Kubernetes (module 13) by hand, every time. Priya's ask: automate the
whole thing — build, test, security-scan, publish, and deploy to staging,
gated behind a human approval before anything touches staging, with a
notification if any of it fails.

## Exercise Prerequisites

1. A running Jenkins instance (Docker is the usual way:
   `docker run -p 8080:8080 -p 50000:50000 -v /var/run/docker.sock:/var/run/docker.sock jenkins/jenkins:lts`),
   with `docker`, `mvn`, and a JDK matching `orderservice/pom.xml`
   actually installed wherever its pipeline steps run — the base image
   ships with none of the three. See this repo's own dry run (in
   `docs/DEMO_SCRIPT.md`'s risk checklist) for exactly what had to be
   added by hand.
2. Plugins: Pipeline, Docker Pipeline, OWASP Dependency-Check, Workspace
   Cleanup.
3. A local Kubernetes cluster (reusing the one from the Kubernetes
   module) as the "staging" deploy target, with a working kubeconfig
   dropped into the Jenkins environment at `/var/jenkins_home/.kube-config`
   — this is a **one-time setup step done by whoever stands up this
   Jenkins instance**, not something each student configures.
4. One Jenkins Credential (Manage Jenkins → Credentials → System →
   Global credentials → Add Credentials): `nvd-api-key`, Secret text —
   free at nvd.nist.gov/developers/request-an-api-key, about a minute
   to get. See the Jenkinsfile's header comment for exactly why this is
   the one credential that couldn't be simplified away.
5. This repo pushed somewhere Jenkins can reach it (a local Git server,
   or any Git host) — a Multibranch Pipeline job needs a real repo with
   branches to discover, not just a local `Jenkinsfile` sitting in a
   directory.

## Exercise Steps

1. **Create a Multibranch Pipeline job** pointed at this repo. Jenkins
   will discover the `Jenkinsfile` automatically and build every branch
   it finds — this is what makes `when { branch 'main' }` meaningful
   later (a plain single-branch Pipeline job has no concept of "which
   branch is this").

2. **Push to a non-`main` branch first** and watch the build. You should
   see `Build and Test` and `Security Scan` run, but `Docker Build and
   Load`, `Approve Deploy to Staging`, and `Deploy to Staging` should all
   be **skipped** (not failed — skipped, shown greyed out in the stage
   view) because of the `when { branch 'main' }` guard.

3. **Merge to `main`** and watch the same build run again. This time all
   5 stages should attempt to run, and the pipeline should **pause** at
   `Approve Deploy to Staging` — check the build's console output or the
   Jenkins UI for a prompt with a "Deploy" button. Nothing after this
   point runs until someone clicks it.

4. **Click through the approval** and confirm `Deploy to Staging`
   actually applies the manifests and updates the running Deployment's
   image — `kubectl get pods -w` on the staging cluster should show the
   rolling update from the Kubernetes module happening again, this time
   triggered by Jenkins instead of you typing the command.

5. **Break something on purpose** (a failing test, or a real CVE
   introduced by pinning a dependency to an old version) and watch the
   pipeline fail at that stage. Confirm the `NOTIFY:` line in the
   console output for the `post { failure { ... } }` block actually
   fires (a real setup would send this to Slack or email instead —
   simplified here to keep the exercise credential-free).

6. **Re-run just the failed stage**, not the whole pipeline. See
   [`docs/SOLUTION.md`](docs/SOLUTION.md) for exactly how, and what it
   does and doesn't re-do.

## Common Pitfalls

- **`when { branch 'main' }` does nothing (or errors) on a plain
  Pipeline job.** This condition only means something in a Multibranch
  Pipeline, where Jenkins tracks which branch triggered each build. If
  you see stages you expected to skip running anyway (or vice versa),
  double-check the job type before debugging the Jenkinsfile itself.
- **The `input` step ties up a Jenkins executor while waiting.** On a
  small Jenkins instance with few executors, a forgotten approval gate
  can block every other job in the queue. Consider `agent none` at the
  top level with per-stage `agent` blocks, so the approval stage doesn't
  hold a heavyweight agent hostage while waiting on a human — not done in
  this exercise's Jenkinsfile for simplicity, but worth knowing for a
  real pipeline.
- **`NvdApiException: Invalid API Key` or `NoDataException: No
  documents exist`.** Either the `nvd-api-key` credential is missing
  or misspelled, or you're looking at an older attempt from before this
  exercise settled on needing that one credential (an earlier version
  tried running with none via `-DautoUpdate=false` — that fails
  outright too, it doesn't degrade gracefully, so it's not a valid
  workaround). Double-check the credential exists under exactly that
  ID.
- **OWASP Dependency-Check running slowly even with a key.** NVD still
  rate-limits fairly aggressively on a cold cache (same caveat as
  module 6). Cache `orderservice/target/dependency-check-data/` between
  builds if you're running this repeatedly.

## Task List

- [ ] Multibranch Pipeline job created, discovers the Jenkinsfile (Step 1)
- [ ] Non-main branch build correctly skips the last 3 stages (Step 2)
- [ ] `main` build pauses at the approval gate (Step 3)
- [ ] Approved build deploys and the rolling update is observable (Step 4)
- [ ] A deliberate failure triggers the notification (Step 5)
- [ ] A failed stage re-run understood (Step 6)

## Free Response

In 75–125 words: the `Security Scan` stage runs on *every* branch, but
`Docker Build and Load` only runs on `main`. What's the reasoning for
that specific split — why gate the expensive/deploy-adjacent stages by
branch, but not the security scan? What's the risk of *also* gating the
security scan to `main` only?

*(A good reflection response is between 75 and 125 words.)*


