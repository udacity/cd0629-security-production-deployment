# TaskFlow – Module 5 Solution

The completed Module 5 project. It builds on the Module 3 API (custom
`AuthenticationProvider`, Argon2id hashing) and replaces HTTP Basic with
**form-based login**, plus remember-me, a one-session-per-user cap and
account lockout after repeated failed logins.

## Solution added

| Feature | Starter (Module 3) | Solution (Module 5) |
|---------|--------------------|---------------------|
| Login | HTTP Basic popup, credentials sent on every request | Spring's form login page, then a session |
| Session | None | Session cookie (`JSESSIONID`) |
| Remember-me | No | Yes, via a remember-me cookie |
| Concurrent sessions | Not applicable | Max 1 per user |
| Brute-force protection | None | Lockout after 5 failed attempts |

### How each piece works

- **Form login:** `SecurityConfig` swaps `.httpBasic(...)` for `.formLogin(...)`.
  A successful login redirects to `/api/v1/tasks`.
- **Remember-me:** `.rememberMe(...)` keeps the user logged in on that browser
  after the session ends.
- **One session per user:** `.sessionManagement(session -> session.maximumSessions(1))`.
- **Lockout:**
  - `CustomAuthenticationFailureHandler` counts each failed login in
    `LoginAttemptService`.
  - `CustomAuthenticationProvider` checks `isBlocked(...)` first and throws
    `LockedException` once the limit is reached.
  - A successful login resets the counter.
- **Configurable limit:** `security.lockout.max-attempts` in `application.yml`
  (default `5`), so it can change per environment without code changes.

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

1. **Log in with the form.** Open <http://localhost:8080/login>, sign in, and
   you land on `/api/v1/tasks`. Tick **Remember me** to get the remember-me cookie.
2. **Create a task from the browser.** Open
   <http://localhost:8080/demo-trigger.html> and click **Create Task**. While
   logged out it sends you to the login form, and once logged in it returns
   `201 Created`.
3. **Trigger the lockout.** Submit a wrong password for `dev1` five times. The
   sixth attempt is rejected as locked, even with the correct password.
   Restart the app to clear the counter, since attempts are kept in memory.
4. **Check the one-session cap.** Log in as `dev1` in two browsers. The first
   session is expired when the second one is active.

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── TaskFlowApplication.java
│   ├── config/SecurityConfig.java                    # Form login, remember-me, session cap
│   ├── controller/TaskController.java                # GET/POST /api/v1/tasks
│   ├── model/Task.java
│   ├── security/
│   │   ├── CustomAuthenticationProvider.java         # Lockout + password + active checks
│   │   ├── CustomAuthenticationFailureHandler.java   # NEW: records failed logins
│   │   ├── LoginAttemptService.java                  # NEW: in-memory attempt counter
│   │   ├── DemoUserDetailsService.java               # One hardcoded user, dev1
│   │   └── AppUserDetails.java
│   └── util/PasswordHashGenerator.java
└── resources/
    ├── application.yml                               # security.lockout.max-attempts
    └── static/demo-trigger.html
```

## Notes

- CSRF protection is disabled to keep the demo simple. Do not do this in a real
  application that uses browser sessions.
- The remember-me key (`taskflow-remember-key`) and the demo credentials are
  hardcoded for demo purposes only.
- Failed-login counts live in memory, so they reset on restart and don't work
  across multiple instances. A real app would use a database or a shared cache
  such as Redis.
- Tasks are also stored in memory and lost on restart.

