# TaskFlow API — Demo 3 SOLUTION project

This is the completed "after" state for Module 3, "Solution: Apply
Custom Authentication Configuration." It picks up right where the Demo
(starter) project left off: same `TaskController`, same `Task` model,
but now with a full custom authentication setup layered on top.

Unlike the Demo project, this one is built forward in one pass — no
commenting/uncommenting files needed. Each step below adds one real
piece and leaves the app in a working state before moving to the next.

## Before you record

**Not compiled in the sandbox that generated it** (no Maven Central
access there) — do a local `mvn clean install` first.

1. Open the project in your IDE.
2. Run `com.taskflow.util.PasswordHashGenerator` (right-click → Run). It
   prints an Argon2id hash for the password `password123`.
3. Copy that hash into `DemoUserDetailsService.PASSWORD_HASH`, replacing
   the placeholder string.
4. Confirm `DemoUserDetailsService.ACTIVE` is set to `true` before you
   start recording — you'll flip it to `false` live, near the end.

## How the project maps to the script

| Script beat | What's already in the code |
|---|---|
| `SecurityFilterChain` bean, lambda DSL | `SecurityConfig.filterChain(...)` |
| Protect only task creation | `.requestMatchers(HttpMethod.POST, "/api/v1/tasks").authenticated()` |
| HTTP Basic + CSRF off (curl, not a browser session) | `.httpBasic(...)`, `.csrf(csrf -> csrf.disable())` |
| Argon2id password encoder | `SecurityConfig.passwordEncoder()`, backed by `PasswordHashGenerator` |
| Custom `UserDetailsService` | `DemoUserDetailsService` — one hardcoded user, `dev1` |
| Custom `AuthenticationProvider` | `CustomAuthenticationProvider` — validates credentials, then checks `active` |
| "Prove the custom provider is running" | `log.info("Custom provider checked {}", username)` inside `authenticate()` |
| "Deactivate the account, show it fail" | Flip `DemoUserDetailsService.ACTIVE` to `false`, restart, re-run the same request |

## Commands to run on camera

Start the app:
```bash
mvn spring-boot:run
```

**Protect the endpoint, test with no credentials.**
```bash
curl -i -X POST http://localhost:8080/api/v1/tasks
```
Expect `401 Unauthorized`.

**Same request, with credentials.**
```bash
curl -i -u dev1:password123 -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Test task"}'
```
Expect `201 Created` with the task echoed back as JSON.

**Prove the custom provider fired.**
Re-run the authenticated `POST` above, then check the **app's console**
(not curl's output — curl only shows the HTTP response) for:
```
Custom provider checked dev1
```

**Deactivate the account.**
In `DemoUserDetailsService`, set:
```java
private static final boolean ACTIVE = false;
```
Restart, re-run the same authenticated `POST`. Expect `401` again — HTTP
Basic maps every authentication failure to 401, so the status code alone
won't distinguish this from a bad password. The proof is in the app
console instead:
```
Rejected dev1 — account is deactivated
```
Contrast that against the plain `Custom provider checked dev1` line from
the earlier successful login — same provider, different outcome, and
you can point at exactly which line in the code decided that.

**Reset before wrapping up.** Flip `ACTIVE` back to `true`, restart, and
re-confirm `201` so the project is left clean.

## Troubleshooting

**`NoClassDefFoundError: org/bouncycastle/crypto/params/Argon2Parameters$Builder`**
`Argon2PasswordEncoder` delegates to Bouncy Castle for the actual Argon2
hashing, and `spring-boot-starter-security` doesn't pull that in by
default. This project's `pom.xml` already includes it:
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.83</version>
</dependency>
```
If you still hit this, confirm Maven actually re-resolved dependencies
after any `pom.xml` edit (`mvn clean install`).

## Regenerating the password hash

If you want to demo with a different plaintext password, run:
```bash
mvn compile exec:java -Dexec.mainClass=com.taskflow.util.PasswordHashGenerator -Dexec.args="yourPasswordHere"
```
(or just run `PasswordHashGenerator.main()` from the IDE with a program
argument) and paste the printed hash into `DemoUserDetailsService`.
