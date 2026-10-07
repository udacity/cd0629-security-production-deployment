# TaskFlow – Module 3 Solution

A small Spring Boot REST API for tasks with **custom authentication**: a lambda-style
`SecurityFilterChain`, HTTP Basic, Argon2id password hashing, a custom
`UserDetailsService`, and a custom `AuthenticationProvider` that adds its own
account-status rule.

## What's in place

| Feature | Where |
|---------|-------|
| Only task creation is protected | `SecurityConfig`: `POST /api/v1/tasks` needs authentication, everything else is open |
| HTTP Basic authentication | `.httpBasic(...)` in `SecurityConfig` |
| CSRF disabled | `.csrf(csrf -> csrf.disable())`, because this is a demo API used with `curl`, not a browser session |
| Argon2id password encoder | `SecurityConfig.passwordEncoder()` |
| Custom user store | `DemoUserDetailsService`, one hardcoded user, `dev1` |
| Custom authentication logic | `CustomAuthenticationProvider` |

### Lambda-style configuration

Spring Security 7 requires configuring each part by passing it a small block of code,
such as `csrf -> csrf.disable()`. The old style of chaining settings together with
`.and()` is gone. Every section in `SecurityConfig` is written this way.

### The custom provider

`CustomAuthenticationProvider` does the normal work, then adds a rule of its own:

1. Look up the user through `DemoUserDetailsService`.
2. Check the password against the stored Argon2id hash.
3. **Check that the account is active.** This is deliberately separate from Spring's
   built-in `UserDetails.isEnabled()`, so it's clear the rule belongs to us and not to
   the framework.

It logs `Custom provider checked <username>` each time it runs, so you can confirm in the
console that it's your code doing the work.

### Password hashing

Passwords are stored as **Argon2id** hashes, Spring Security's recommended encoder.
`Argon2PasswordEncoder` relies on Bouncy Castle for the actual hashing, which
`spring-boot-starter-security` doesn't include, so `pom.xml` adds it as a dependency.

## Tech stack

- Java 21 or later
- Spring Boot 4.0.0 (Spring Security 7)
- Bouncy Castle
- Maven

## Run it

```bash
mvn clean install
mvn spring-boot:run
```

The app starts on <http://localhost:8080>. **User:** `dev1` / `password123`

## Try it

**1. No credentials.** The protected endpoint refuses you:

```bash
curl -i -X POST http://localhost:8080/api/v1/tasks
```

You get `401 Unauthorized`.

**2. With credentials.**

```bash
curl -i -u dev1:password123 -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Test task"}'
```

You get `201 Created` with the task as JSON.

**3. An open endpoint.** `GET` needs no login:

```bash
curl http://localhost:8080/api/v1/tasks
```

**4. Confirm the custom provider ran.** Re-run step 2, then look at the **app's
console**, not curl's output, for:

```
Custom provider checked dev1
```

**5. Deactivate the account.** In `DemoUserDetailsService`, set:

```java
private static final boolean ACTIVE = false;
```

Restart and repeat step 2. You get `401` again. HTTP Basic reports every authentication
failure as 401, so the status code can't tell this apart from a wrong password. The
console shows the real reason:

```
Rejected dev1 — account is deactivated
```

Compare it with the `Custom provider checked dev1` line from the successful login: the
same provider, a different outcome, decided by one line of your code. **Set `ACTIVE` back
to `true` and restart** when you're done.

## Regenerate the password hash

`PASSWORD_HASH` in `DemoUserDetailsService` is an Argon2id hash of `password123`. To use a
different password, run `PasswordHashGenerator` from your IDE (optionally with the
password as a program argument), or:

```bash
mvn compile exec:java -Dexec.mainClass=com.taskflow.util.PasswordHashGenerator -Dexec.args="yourPasswordHere"
```

Then paste the printed hash into `DemoUserDetailsService.PASSWORD_HASH`.

## Troubleshooting

**`NoClassDefFoundError: org/bouncycastle/crypto/params/Argon2Parameters$Builder`**
Bouncy Castle isn't on the classpath. This project's `pom.xml` already includes it:

```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.83</version>
</dependency>
```

If you still hit this, make sure Maven re-resolved dependencies after any `pom.xml` edit
(`mvn clean install`).

## Project structure

```
src/main/
├── java/com/taskflow/
│   ├── TaskFlowApplication.java
│   ├── config/SecurityConfig.java                # Filter chain, HTTP Basic, Argon2id encoder
│   ├── controller/TaskController.java            # GET/POST /api/v1/tasks (in-memory list)
│   ├── model/Task.java
│   ├── security/
│   │   ├── CustomAuthenticationProvider.java     # Password check and active-account rule
│   │   ├── DemoUserDetailsService.java           # One hardcoded user, dev1
│   │   └── AppUserDetails.java                   # UserDetails with an "active" flag
│   └── util/PasswordHashGenerator.java           # Prints an Argon2id hash for a password
└── resources/
    └── application.yml
```

## Notes

- The demo user, password and hash are hardcoded for demonstration only. Never do this
  in a real application.
- CSRF is disabled because this API is used from `curl` and sends credentials on every
  request. A browser app with session login needs CSRF protection.
- Tasks live in memory and are lost on restart.
