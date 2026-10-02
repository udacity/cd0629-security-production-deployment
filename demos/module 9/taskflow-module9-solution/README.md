# TaskFlow – Module 9 Solution

A Spring Boot REST API that acts as an **OAuth2 Resource Server**. TaskFlow no
longer manages users, passwords or sessions. Identity is delegated to
[Keycloak](https://www.keycloak.org/), an Authorization Server, and every
request carries a signed **JWT** access token.

Role-based authorization (role hierarchy, method security, ownership check)
works exactly as before. It only needs the roles to be on the `Authentication`
object, and doesn't care where they came from.

## How it works

| Concern | Handled by |
|---------|------------|
| Who is the user? | Keycloak issues a JWT after the user logs in |
| Is the token valid? | Spring validates the signature and issuer against `issuer-uri` |
| What can the user do? | `realm_access.roles` claim mapped to `ROLE_` authorities |
| Sessions | None. The API is stateless and every request carries its own token |

Key pieces in `SecurityConfig`:

- **`.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`** turns on JWT bearer
  token authentication.
- **Custom `JwtAuthenticationConverter`:**
  - Keycloak puts roles in `realm_access.roles`, not in the flat `scope` claim
    Spring expects by default. Without the mapping, every role check would fail.
  - `setPrincipalClaimName("preferred_username")` makes
    `authentication.getName()` return the username instead of Keycloak's
    internal subject UUID. The ownership check
    (`#task.owner == authentication.name`) depends on this.
- **`SessionCreationPolicy.STATELESS`** means the server never creates a session.
- **`RoleHierarchy` and `MethodSecurityExpressionHandler` beans:** `ADMIN` implies
  `MANAGER` implies `USER`, for both URL-level and method-level checks.
- **CSRF is disabled.** That is acceptable for a stateless, token-based API that
  doesn't use cookies for authentication.

## Endpoints

| Endpoint | Access |
|----------|--------|
| `GET /api/v1/tasks` | Open to everyone |
| `POST /api/v1/tasks` | Any authenticated user; the caller becomes the task's owner |
| `DELETE /api/v1/tasks/{id}` | `MANAGER` or `ADMIN` |
| `PATCH /api/v1/tasks/{id}/complete` | Authenticated, and must be the task's owner |

## Users

Create these in Keycloak (see setup below). All use the password `password123`.

| Username | Realm role | Effective roles |
|----------|------------|-----------------|
| `dev1` | `USER` | USER |
| `manager1` | `MANAGER` | MANAGER, USER |
| `admin1` | `ADMIN` | ADMIN, MANAGER, USER |

## Prerequisites

- Java 25
- Maven
- Docker (to run Keycloak)
- [Postman](https://www.postman.com/) (to obtain tokens and call the API)

## Keycloak setup (one time)

1. Start Keycloak from the project root:
   ```bash
   docker compose up -d
   ```
2. Open <http://localhost:8081> and log in as `admin` / `admin`.
3. Create a realm named **`taskflow`**.
4. Create a client:
   - **Client ID:** `taskflow-postman`
   - **Client authentication:** off (public client)
   - **Authentication flow:** Standard flow only (Authorization Code)
   - **Valid redirect URIs:** `https://oauth.pstmn.io/v1/callback`
   - **Advanced settings → Proof Key for Code Exchange:** `S256`
5. Create three **realm roles**: `USER`, `MANAGER`, `ADMIN`. They must be
   realm roles, not client roles.
6. Create three users (**Users → Add user**). For each, open the **Credentials**
   tab, set the password `password123`, and turn **Temporary** off. Then assign
   the realm role from the table above.

## Run the API

```bash
mvn spring-boot:run
```

The API starts on <http://localhost:8080>. Keycloak must already be running,
because the app contacts it at startup to discover the signing keys.

## Get a token in Postman

1. Create a new request and open the **Authorization** tab.
2. Set **Type** to **OAuth 2.0** and **Grant type** to **Authorization Code (with PKCE)**.
3. Fill in:
   - **Callback URL:** `https://oauth.pstmn.io/v1/callback`
   - **Auth URL:** `http://localhost:8081/realms/taskflow/protocol/openid-connect/auth`
   - **Access Token URL:** `http://localhost:8081/realms/taskflow/protocol/openid-connect/token`
   - **Client ID:** `taskflow-postman`
4. Click **Get New Access Token**, log in as the user you want to test, then
   click **Use Token**.

Postman sends the token as an `Authorization: Bearer <token>` header. Get a new
token each time you want to act as a different user.

## Try it

1. **Create a task** as `dev1` with `POST /api/v1/tasks` and body
   `{"title":"Test task"}`. You get `201 Created` and `"owner": "dev1"`.
2. **Delete it as `dev1`** with `DELETE /api/v1/tasks/1`. You get `403 Forbidden`.
3. **Delete it as `manager1`.** You get `204 No Content`.
4. **Delete it as `admin1`.** You also get `204`. `admin1` was never granted
   `MANAGER`, so this shows the role hierarchy at work.
5. **Complete your own task.** As `dev1`, create a task and call
   `PATCH /api/v1/tasks/{id}/complete`. You get `200`.
6. **Complete someone else's task.** As `manager1`, try the same task. You get
   `403`, even though `manager1` outranks `dev1`. Role and ownership are two
   independent checks.

You can also call the API with `curl` by passing a token you copied from Postman:

```bash
curl -i -X POST http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test task"}'
```

Access tokens are short-lived. A `401` on a request that worked earlier usually
means the token has expired, so get a new one.

## Project structure

```
.
├── docker-compose.yml                                # Keycloak on port 8081
├── pom.xml
└── src/main/
    ├── java/com/taskflow/
    │   ├── TaskFlowApplication.java
    │   ├── config/SecurityConfig.java                # JWT resource server, role mapping, hierarchy
    │   ├── controller/TaskController.java            # Task endpoints
    │   ├── service/TaskService.java                  # Task logic and @PreAuthorize checks
    │   ├── model/Task.java                           # id, title, owner, completed
    │   ├── security/                                 # Retired: kept for reference, not active
    │   │   ├── CustomAuthenticationProvider.java
    │   │   ├── CustomAuthenticationFailureHandler.java
    │   │   ├── DemoUserDetailsService.java
    │   │   ├── LoginAttemptService.java
    │   │   └── AppUserDetails.java
    │   └── util/PasswordHashGenerator.java
    └── resources/
        ├── application.yml                           # Keycloak issuer-uri
        └── static/demo-trigger.html
```

The classes in `security/` have their `@Component` annotation commented out.
They show what Keycloak now does for us: storing password hashes, checking
credentials and locking accounts after failed logins.

`TaskController` and `TaskService` contain no authentication code. They only
use `Authentication.getName()`, so replacing the whole login mechanism didn't
change them.

## Troubleshooting

**The app fails to start with a connection error to `localhost:8081`.**
Keycloak isn't running yet, or the `taskflow` realm doesn't exist. Run
`docker compose up -d`, wait a few seconds, and check
<http://localhost:8081/realms/taskflow>.

**Every request returns `403` even though the token is valid.**
The user probably has no realm role, or you assigned a client role instead of a
realm role. Check the user's **Role mapping** tab in Keycloak.

**`owner` shows a long UUID instead of the username.**
The token's principal claim isn't `preferred_username`. Check that
`setPrincipalClaimName("preferred_username")` is still in `SecurityConfig`.

**`401 Unauthorized` on protected endpoints.**
The `Authorization: Bearer ...` header is missing or the token has expired.

## Notes

- Keycloak runs in `start-dev` mode with the default `admin` / `admin` login.
  It is for local development only.
- `demo-trigger.html` relied on session cookies and no longer authenticates,
  because the API is stateless and expects a bearer token.
- Tasks live in memory and are lost on restart.
