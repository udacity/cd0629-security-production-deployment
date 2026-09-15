# TaskFlow API — Demo 3 STARTER project

This is the "before" state for Module 3, "Demo: Apply Custom Authentication
Configuration." It's deliberately bare: a working Task API with **no
security configuration at all**. This is what makes Spring Boot's default
auto-login behavior show up genuinely, no need to fake it by hiding code.

The completed version lives in the separate `taskflow-solution` project —
don't peek at it until after recording the Demo.

## What's here

- `TaskFlowApplication.java` — entry point
- `TaskController.java` — `GET`/`POST /api/v1/tasks`, fully open right now
- `Task.java` — plain model
- `application.yml` — port 8080, basic logging

No `SecurityConfig`, no custom `UserDetailsService`, no
`AuthenticationProvider`. `spring-boot-starter-security` is on the
classpath (so Spring Boot's default behavior kicks in), but nothing
customizes it yet.

## Recording flow

1. `mvn clean install`, then `mvn spring-boot:run`.
2. In the browser, go to `http://localhost:8080/api/v1/tasks`. You'll be
   redirected to Spring's default login page — genuinely unstyled,
   because nothing here overrides it.
3. Check the console output from startup. You'll see a real line like:
   ```
   Using generated security password: 8f14e045-fceb-4dc5-9e2c-4a3e5c8b9a11
   ```
   This is real this time — no custom `UserDetailsService` exists yet to
   suppress it.
4. Log in with username `user` and that generated password. Point out:
   it works, but it's completely impractical — a new random password
   every restart, no real user store, no way to add business rules like
   "is this account still active."
5. Lay out the requirements for what you're about to build in the
   Solution video: a real `SecurityFilterChain`, a real user store,
   Argon2id password hashing, and a custom `AuthenticationProvider`.

That's the full Demo. Nothing to build here — the Solution project is a
separate, complete implementation.
