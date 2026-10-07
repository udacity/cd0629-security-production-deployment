# TaskFlow – Module 3 Starter

A small Spring Boot REST API for tasks with **no security configuration at all**.
`spring-boot-starter-security` is on the classpath, so Spring Boot applies its default
behaviour, but nothing customizes it.

Use this project as the starting point for building custom authentication.

## What's here

| File | What it does |
|------|--------------|
| `TaskFlowApplication.java` | Application entry point |
| `controller/TaskController.java` | `GET` and `POST /api/v1/tasks`, stored in memory |
| `model/Task.java` | A plain task model |
| `application.yml` | Port 8080 and basic logging |

There is no `SecurityConfig`, no custom `UserDetailsService` and no
`AuthenticationProvider`.

## The problem

With Spring Security on the classpath and no configuration, Spring Boot protects every
endpoint with a generated login. It works, but it isn't practical:

- The password is **random and changes on every restart**.
- There is **no real user store**, only a single built-in user.
- There is **no place for business rules**, such as "is this account still active?"
- **Everything is locked down**, including endpoints you'd want to leave open.

## Tech stack

- Java 25
- Spring Boot 4.0.0 (Spring Security 7)
- Maven

## Run it

```bash
mvn clean install
mvn spring-boot:run
```

The app starts on <http://localhost:8080>.

## See the problem

1. Open <http://localhost:8080/api/v1/tasks> in a browser. You're redirected to Spring's
   default login page, which is unstyled because nothing overrides it.
2. Look at the console output from startup. You'll see a line like:
   ```
   Using generated security password: 8f14e045-fceb-4dc5-9e2c-4a3e5c8b9a11
   ```
   Your password will differ.
3. Log in with the username `user` and that generated password. It works, but a new
   random password on every restart is no basis for a real application.

## What you'll build next

- A real `SecurityFilterChain` that protects only the endpoints that need it
- A real user store
- Argon2id password hashing
- A custom `AuthenticationProvider` with your own business rule

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── TaskFlowApplication.java
│   ├── controller/TaskController.java
│   └── model/Task.java
└── resources/
    └── application.yml
```

## Notes

- Tasks live in memory and are lost on restart.
