# TaskFlow – Module 5 Demo Starter

A small Spring Boot REST API used as the **"before" picture** for Module 5. It
is the final state of Module 3: HTTP Basic authentication, a custom
`AuthenticationProvider`, and Argon2id password hashing.


## What the demo shows

| Behaviour | Where you see it |
|-----------|------------------|
| `GET /api/v1/tasks` is open to everyone | `curl` or browser, no login |
| `POST /api/v1/tasks` requires authentication | 401 without credentials |
| Browser shows a raw HTTP Basic popup | Click **Create Task** on the trigger page |
| No session: the browser resends credentials with every request | Network tab / server log |
| Passwords stored as Argon2id hashes | `DemoUserDetailsService` |

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

**Demo user:** `dev1` / `password123`

```bash
# Public: no credentials needed
curl http://localhost:8080/api/v1/tasks

# Protected: rejected with 401
curl -i -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Write README"}'

# Protected: succeeds with HTTP Basic credentials (201 Created)
curl -i -u dev1:password123 -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Write README"}'
```

For the browser view, open <http://localhost:8080/demo-trigger.html> and click
**Create Task**. The browser shows its built-in login popup.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── TaskFlowApplication.java            # Spring Boot entry point
│   ├── config/SecurityConfig.java          # Filter chain, HTTP Basic, Argon2id encoder
│   ├── controller/TaskController.java      # GET/POST /api/v1/tasks (in-memory list)
│   ├── model/Task.java                     # Task model
│   ├── security/
│   │   ├── CustomAuthenticationProvider.java  # Password check + active-account rule
│   │   ├── DemoUserDetailsService.java        # One hardcoded user, dev1
│   │   └── AppUserDetails.java                # UserDetails with an "active" flag
│   └── util/PasswordHashGenerator.java     # Prints an Argon2id hash for a password
└── resources/
    ├── application.yml
    └── static/demo-trigger.html            # "Create Task" button for the demo
```

## Useful tweaks

- **Deactivated account:** set `ACTIVE = false` in `DemoUserDetailsService`,
  restart, and repeat the authenticated `curl`. The custom provider rejects the
  login even though the password is correct.
- **Different password:** run `PasswordHashGenerator` from your IDE (optionally
  pass a password as an argument) and paste the printed hash into
  `DemoUserDetailsService.PASSWORD_HASH`.

## Notes

- Tasks live in memory and are lost on restart.
- Credentials are hardcoded for demo purposes only. Do not reuse this pattern
  in a real application.
