# Solution: Apply Custom Authentication Configuration

**Scenario recap:** Udabank needed `/app/**` locked down before a
compliance review, backed by the real `users` table, Argon2id password
hashing, and HTTP Basic as a stopgap until the frontend team ships a real
login page. See [`../starter/README.md`](../starter/README.md) for the full
story and task breakdown.

## What changed

- [`DbUserDetailsService.java`](src/main/java/com/udabank/authdemo/user/DbUserDetailsService.java) —
  loads a `User` via `UserRepository.findByUsername`, throws
  `UsernameNotFoundException` on a miss, and maps the entity to a Spring
  Security `UserDetails` with a single `ROLE_<role>` authority.
- [`SecurityConfig.java`](src/main/java/com/udabank/authdemo/config/SecurityConfig.java) —
  - `PasswordEncoder` bean using `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`
    (OWASP-recommended params: memory=19456 KB, iterations=2, parallelism=1).
  - `SecurityFilterChain` bean using the 7.x lambda-only DSL:
    `/public/**` → `permitAll()`, `/app/**` → `authenticated()`, everything
    else → `denyAll()` (fail closed instead of fail open), `httpBasic()`
    enabled, `formLogin()` disabled.
  - No explicit `.userDetailsService(...)` call needed — `DbUserDetailsService`
    is already a `@Service` bean, and Spring Security's auto-configuration
    picks up a single user-defined `UserDetailsService` bean automatically.

## Solution walkthrough: inspecting the resolved filter chain

Priya's next question in the standup will be "how do I know this is actually
wired correctly?" — you don't want to just eyeball the config class.

[`FilterChainInspector.java`](src/main/java/com/udabank/authdemo/config/FilterChainInspector.java)
is an `ApplicationRunner` that logs the concrete list of `Filter`s Spring
Security resolved at startup. Run the app and look for this in the logs:

```
Resolved SecurityFilterChain: DefaultSecurityFilterChain
  -> DisableEncodeUrlFilter
  -> WebAsyncManagerIntegrationFilter
  -> SecurityContextHolderFilter
  -> HeaderWriterFilter
  -> CsrfFilter
  -> LogoutFilter
  -> BasicAuthenticationFilter
  -> RequestCacheAwareFilter
  -> SecurityContextHolderAwareRequestFilter
  -> AnonymousAuthenticationFilter
  -> ExceptionTranslationFilter
  -> AuthorizationFilter
```

(Captured from an actual local run — see `target/surefire-reports` after `./mvnw test`.)

Three things worth confirming from that list, and why they matter:

- **`BasicAuthenticationFilter` is present, but no `UsernamePasswordAuthenticationFilter`
  (form login) shows up.** That confirms `formLogin(AbstractHttpConfigurer::disable)`
  actually took effect — a common mistake is disabling form login in one
  filter chain while a second, unintended filter chain still has it enabled.
- **`CsrfFilter` is still there.** We never called `.csrf(...)` in the config,
  so Spring Security's CSRF protection stays on by default — which is fine
  here since our exercise only exercises `GET` endpoints (CSRF only guards
  state-changing methods), but it's a reminder that HTTP Basic does *not*
  imply "stateless, so disable CSRF." That's a deliberate choice for a
  later module (the OWASP mitigations one), not this one.
- **`AuthorizationFilter` is last.** That's what enforces the
  `permitAll` / `authenticated` / `denyAll` rules from `authorizeHttpRequests`,
  and it needs to run after authentication has populated the security
  context — if you ever see it earlier in the list, something upstream was
  reordered.

This is also the fastest way to debug "why is my endpoint still 401" bugs:
if a filter you expected (or didn't expect) is missing from this list, the
DSL call that should have added/removed it isn't doing what you think.

## Verifying the solution

```bash
./mvnw spring-boot:run
```

```bash
curl -i http://localhost:8080/public/health
# HTTP/1.1 200

curl -i http://localhost:8080/app/dashboard
# HTTP/1.1 401

curl -i -u alice:VaultAdmin!2025 http://localhost:8080/app/dashboard
# HTTP/1.1 200
# {"message":"Ledger dashboard loaded.","authenticatedAs":"alice"}

curl -i -u alice:wrong-password http://localhost:8080/app/dashboard
# HTTP/1.1 401
```

```bash
./mvnw test -Dtest=SecurityConfigTest
# 4 tests, all green
```

## Common pitfalls (what Priya will actually ask about in review)

- **Forgetting `anyRequest().denyAll()`.** Without a catch-all rule, any path
  you didn't explicitly match falls through to Spring Security's default
  (authenticated), which *happens* to be safe here — but it's implicit and
  will silently change behavior if someone adds a matcher above it later.
  Being explicit makes the "fail closed" decision visible in the code.
- **Argon2id vs. BCrypt.** The brief only asks for Argon2id as the active
  encoder. If you later need to verify *existing* BCrypt hashes during a
  migration, that's what `PasswordEncoderFactories.createDelegatingPasswordEncoder()`
  combined with a custom `idForEncode` is for — out of scope for this
  exercise, but worth knowing before the next one.
- **`PathPatternRequestMatcher` vs. `AntPathRequestMatcher`.** Spring Security
  7 defaults to `PathPatternRequestMatcher`; mixing the two matcher types
  across rules in the same chain is a common source of "why doesn't my
  pattern match" bugs.
