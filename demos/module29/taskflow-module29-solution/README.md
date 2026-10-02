# TaskFlow – Module 29 Demo Solution

A Spring Boot REST API (Keycloak-issued JWTs, role hierarchy, method security, CORS
and security headers, OWASP mitigations, Vault for secrets, a Config Server for
settings, structured logs, Prometheus and Grafana monitoring, a Docker image and
Kubernetes manifests) with **unit tests** and a **Jenkins CI/CD pipeline** that
builds, tests and packages it automatically.

## What's in place

| Feature | Where |
|---------|-------|
| Unit tests | `src/test/java/com/taskflow/service/TaskServiceTest.java` |
| Pipeline definition | `Jenkinsfile` |
| Jenkins controller image | `jenkins/Dockerfile` |
| Zero-touch Jenkins setup | `jenkins/init.groovy.d/basic-security.groovy` and `jenkins/plugins.txt` |
| Jenkins container | `jenkins` service in `docker-compose.yml`, profile `containerized` |

### The tests

`TaskServiceTest` has four tests of `TaskService`: creating a task, listing tasks,
finding a task by ID, and completing a task. `TaskService` has no injected
dependencies, so these are plain, fast unit tests with no Spring context.

`TaskService.completeTask` carries a `@PreAuthorize` annotation (Module 7). That only
does anything when Spring wraps the bean in a security proxy, and a plain
`new TaskService()` never gets one. The tests therefore check the underlying logic only,
not the authorization rule. The running app covers the rule.

### The pipeline

```
Checkout → Build → Test → Package → Docker Build
```

| Stage | Runs in | What it does |
|-------|---------|--------------|
| Checkout | Jenkins controller | Checks out the source |
| Build | Ephemeral Maven container | `mvn compile` |
| Test | Ephemeral Maven container | `mvn test`, then publishes the JUnit report |
| Package | Ephemeral Maven container | `mvn package -DskipTests` |
| Docker Build | Jenkins controller | `docker build -t taskflow-api:latest .` |

The controller never compiles or tests TaskFlow itself. Build, Test and Package each
start a fresh container from `maven:3.9-eclipse-temurin-25`, the same image the
`Dockerfile` uses, and discard it afterwards. Only the final stage runs on the
controller, because that is the one place with access to the host's Docker socket.

The JUnit report is published even when tests fail, so you can see exactly which test
broke.

### Jenkins setup

- **Controller image:** `jenkins/jenkins:lts-jdk21`, left unmodified apart from the Docker
  CLI and plugins. Jenkins' own JVM support can lag behind the newest JDK, so JDK 25
  stays confined to the build containers that need it.
- **No setup wizard.** `basic-security.groovy` creates the login and marks setup as
  complete. The login is `admin` / `admin`.
- **Plugins:** `git`, `workflow-aggregator`, `docker-workflow` and `junit`.
- **Docker socket mounted.** The controller starts build containers and runs the
  final image build by talking to your machine's Docker daemon (Docker-outside-of-Docker,
  not Docker-in-Docker).
- **The project is mounted read-only** at `/taskflow-source`, including its `.git`
  history, so Jenkins can check it out through a real `file://` URL, just as it would
  from GitHub.

## Tech stack

- Java 25
- Spring Boot 4.0.7 (Spring Security 7, Spring Data JPA, Actuator)
- JUnit 5 (through `spring-boot-starter-test`)
- Jenkins (LTS, JDK 21 controller)
- Maven
- Docker and Kubernetes

## Prerequisites

- Docker Desktop
- This project must be a **Git repository with at least one commit**, on the branch you
  configure in Jenkins (`main` below). Jenkins checks out committed content from the
  repository's history. It doesn't read your working folder directly. If you copied the
  files without a `.git` folder, run:
  ```bash
  git init -b main
  git add .
  git commit -m "Initial commit"
  ```
- Vault running with the secret stored (see the Module 15 project), if you also want to
  run the app itself.

## Run the tests locally

```bash
mvn test
```

## Set up Jenkins

1. **Build and start Jenkins** from the project root:
   ```bash
   docker compose --profile containerized up -d --build jenkins
   ```
   The first build downloads the base image and installs plugins, which can take
   several minutes.
2. **Log in** at <http://localhost:8082> with `admin` / `admin`. You go straight to the
   dashboard.
3. **Create the pipeline job.** This step is manual:
   - **New Item**, name it `taskflow-pipeline`, choose **Pipeline**, and click OK.
   - Under **Pipeline**, set **Definition** to **Pipeline script from SCM**.
   - **SCM:** Git. **Repository URL:** `file:///taskflow-source`.
   - **Branch Specifier:** `*/main`, or whatever your repository's default branch is.
   - **Script Path:** `Jenkinsfile`.
   - Save.

## Try it

1. **Run the pipeline.** Open `taskflow-pipeline` and click **Build Now**.
2. **Watch the stages** in the Jenkins UI. Click a stage to see its log.
3. **Check the test report.** After the Test stage, open the build's **Test Result**
   page to see the published JUnit results.
4. **Check the image:**
   ```bash
   docker images | grep taskflow-api
   ```
5. **Make a test fail, then fix it.** Change an assertion in `TaskServiceTest`, for
   example flip `assertTrue` to `assertFalse` in `findById_locatesAnExistingTask`.
   **Commit the change**, then click **Build Now** again. The Test stage goes red, and
   Package and Docker Build don't run. Revert the change, commit, and build again to
   get back to green.

Jenkins builds what's **committed**. An uncommitted edit won't be picked up, so run
`git commit` before each **Build Now**.

## Project structure

```
.
├── Jenkinsfile                               # Checkout → Build → Test → Package → Docker Build
├── jenkins/
│   ├── Dockerfile                            # Jenkins controller image
│   ├── plugins.txt
│   └── init.groovy.d/basic-security.groovy   # admin/admin login, skips the setup wizard
├── k8s/                                      # Kubernetes manifests
├── Dockerfile                                # Multi-stage build, non-root runtime
├── .dockerignore
├── docker-compose.yml                        # Supporting services, plus Jenkins (profile: containerized)
├── prometheus.yml
├── alerts.yml
├── scripts/import-logs.sh
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/taskflow/                # Application code
    │   └── resources/                        # application.yml, data.sql, static pages
    └── test/java/com/taskflow/service/
        └── TaskServiceTest.java              # Unit tests for TaskService
```

## What this doesn't cover

- **Webhooks or polling** to trigger builds automatically on every push. Here you start
  builds by hand.
- **Deploying** the built image. The pipeline stops at a local Docker image.
- **Tests beyond `TaskService`**, such as controller, security or integration tests.

## Notes

- **Docker socket access is root-equivalent on the host.** Mounting it is fine for a
  throwaway local demo, but a production Jenkins should use a locked-down agent model.
- **The Jenkins controller runs as root** on purpose, to avoid Docker socket
  permission problems that vary between Mac, Linux and Windows. Don't copy that for a
  real controller.
- **The `admin` / `admin` login** is for local use only.
- The project is mounted at `/taskflow-source` read-only, so Jenkins can't change it.
