# TaskFlow – Module 7 Demo Solution

A Spring Boot REST API with form-based login and **role-based authorization**:
a role hierarchy, URL-level and method-level security, and a per-task ownership
check.

## Features

| Feature | How it works |
|---------|--------------|
| Roles | `USER`, `MANAGER`, `ADMIN` |
| Role hierarchy | `ADMIN` implies `MANAGER`, which implies `USER` |
| URL-level rule | `DELETE /api/v1/tasks/**` requires `MANAGER` or higher |
| Method-level rule | Only a task's owner can complete it (`@PreAuthorize`) |
| Authentication | Form login, remember-me, 1 session per user, lockout after 5 failed attempts |

## Endpoints

| Endpoint | Access |
|----------|--------|
| `GET /api/v1/tasks` | Open to everyone |
| `POST /api/v1/tasks` | Any authenticated user; the caller becomes the task's owner |
| `DELETE /api/v1/tasks/{id}` | `MANAGER` or `ADMIN` |
| `PATCH /api/v1/tasks/{id}/complete` | Authenticated, and must be the task's owner |

## Demo users

All three share the password `password123`.

| Username | Role | Effective roles |
|----------|------|-----------------|
| `dev1` | `USER` | USER |
| `manager1` | `MANAGER` | MANAGER, USER |
| `admin1` | `ADMIN` | ADMIN, MANAGER, USER |

## How it works

- **Role hierarchy:** a `RoleHierarchy` bean in `SecurityConfig` means an
  `ADMIN` passes a `hasRole("MANAGER")` check without having `ROLE_MANAGER`
  granted directly.
- **Method security:** `@EnableMethodSecurity` turns on `@PreAuthorize`. A
  `MethodSecurityExpressionHandler` bean wires the hierarchy into method-level
  checks too. Without it, they would ignore the hierarchy.
- **Ownership check:** `TaskService.completeTask` uses
  `@PreAuthorize("#task.owner == authentication.name")`. The role doesn't
  matter here: an `ADMIN` still can't complete someone else's task.
- **Parameter names:** `#task` resolves by parameter name, so `pom.xml` enables
  the `-parameters` compiler flag.
- **Self-invocation gotcha:** `completeTaskInternally` calls `completeTask`
  directly on `this`, bypassing Spring's proxy, so the `@PreAuthorize` check
  never runs. Method security only applies to calls that go through the proxy,
  such as calls from another bean.

## Tech stack

- Java 25
- Spring Boot 4.0.0 (Spring Security 7, lambda DSL only)
- Maven
- Bouncy Castle (required by `Argon2PasswordEncoder`)

## Run it

```bash
mvn spring-boot:run
```

The app starts on <http://localhost:8080>.

## Try it

Log in at <http://localhost:8080/login>, then use the buttons on
<http://localhost:8080/demo-trigger.html> to create, delete and complete tasks.

1. **Create a task** as `dev1`. The response shows `"owner": "dev1"`.
2. **Delete it as `dev1`.** You get `403 Forbidden`, because delete needs `MANAGER`.
3. **Log in as `manager1` or `admin1` and delete it.** You get `204 No Content`.
4. **Complete someone else's task.** Create a task as `dev1`, then log in as
   `manager1` and try to complete it. It fails with `403`, because only the
   owner can.
5. **Complete your own task.** It returns the task with `"completed": true`.

Each user needs their own browser session, and only one session per user is
allowed. Use separate browsers or private windows, or log out between users.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── TaskFlowApplication.java
│   ├── config/SecurityConfig.java                    # Rules, role hierarchy, method security
│   ├── controller/TaskController.java                # Task endpoints
│   ├── service/TaskService.java                      # Task logic and @PreAuthorize checks
│   ├── model/Task.java                               # id, title, owner, completed
│   ├── security/
│   │   ├── CustomAuthenticationProvider.java         # Lockout, password and active checks
│   │   ├── CustomAuthenticationFailureHandler.java   # Records failed logins
│   │   ├── LoginAttemptService.java                  # In-memory attempt counter
│   │   ├── DemoUserDetailsService.java               # dev1, manager1, admin1
│   │   └── AppUserDetails.java                       # UserDetails with active flag and role
│   └── util/PasswordHashGenerator.java               # Prints an Argon2id hash for a password
└── resources/
    ├── application.yml
    └── static/demo-trigger.html
```

## Notes

- CSRF protection is disabled to keep the project simple. Do not do this in a
  real application that uses browser sessions.
- Users, credentials and the remember-me key are hardcoded for demo purposes only.
- `DefaultMethodSecurityExpressionHandler.setRoleHierarchy` is deprecated, but
  there is no full replacement for method-security role hierarchies yet
  (see spring-security#12783).
- Tasks and failed-login counts live in memory and reset on restart.
