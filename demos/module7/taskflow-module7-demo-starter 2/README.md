# TaskFlow – Module 7 Starter

A Spring Boot REST API with form-based login, remember-me, a one-session-per-user
cap and account lockout. Authentication is in place, but **authorization is not**:
there are no roles and no concept of who owns a task. One blanket rule decides
everything: a request is either authenticated or it isn't.

Use this project as the starting point for adding role-based authorization.

## Current behaviour

| Endpoint | Access |
|----------|--------|
| `GET /api/v1/tasks` | Open to everyone |
| `POST /api/v1/tasks` | Any authenticated user |
| Everything else | Open to everyone |

Every logged-in user can do exactly the same things, and tasks don't record who
created them.

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

**User:** `dev1` / `password123`

```bash
# Public: no login needed
curl http://localhost:8080/api/v1/tasks
```

To use the protected endpoints, log in at <http://localhost:8080/login>, then
use the buttons on <http://localhost:8080/demo-trigger.html>.

## What's already in place

- **Form login** with a custom `AuthenticationProvider` and Argon2id password hashing
- **Remember-me** cookie
- **Max 1 session per user**
- **Account lockout** after `security.lockout.max-attempts` failed logins
  (default `5`, set in `application.yml`)

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── TaskFlowApplication.java
│   ├── config/SecurityConfig.java                    # Filter chain and password encoder
│   ├── controller/TaskController.java                # GET/POST /api/v1/tasks (in-memory list)
│   ├── model/Task.java
│   ├── security/
│   │   ├── CustomAuthenticationProvider.java         # Lockout, password and active checks
│   │   ├── CustomAuthenticationFailureHandler.java   # Records failed logins
│   │   ├── LoginAttemptService.java                  # In-memory attempt counter
│   │   ├── DemoUserDetailsService.java               # One hardcoded user, dev1
│   │   └── AppUserDetails.java
│   └── util/PasswordHashGenerator.java               # Prints an Argon2id hash for a password
└── resources/
    ├── application.yml
    └── static/demo-trigger.html
```

## Notes

- CSRF protection is disabled to keep the project simple. Do not do this in a
  real application that uses browser sessions.
- Credentials and the remember-me key are hardcoded for demo purposes only.
- Tasks and failed-login counts live in memory and reset on restart.
