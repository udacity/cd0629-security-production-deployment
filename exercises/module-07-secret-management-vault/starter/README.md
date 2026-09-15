# Exercise: Apply Secret Management with Vault

**Estimated time:** 10–15 minutes

## The scenario

Udabank. A code-review comment from Priya: `application.yml` has a real
database password sitting in plaintext, committed to the repo. She wants
it pulled from Vault instead.

## Your task

One file: [`application.yml`](src/main/resources/application.yml). Wire
up Vault-backed config so `db.username` / `db.password` resolve from
Vault at startup instead of from this file. The TODO comment in that file
has the exact properties you need and what each one does.

No Docker/real Vault required — [`FakeVaultServer`](src/test/java/com/udabank/authdemo/FakeVaultServer.java)
(test sources) stands in for one, implementing just enough of Vault's real
HTTP API for the actual Spring Cloud Vault client to talk to.

## Running it

```bash
./mvnw spring-boot:run
curl http://localhost:8080/internal/db-status
# right now: {"username":"udabank_app","passwordConfigured":true} — hardcoded
```

## Verifying your work

```bash
./mvnw test -Dtest=VaultConfigTest
```

Fill in the TODO test assertion — once your `application.yml` fix is
correct, `/internal/db-status` should report `"username":"vault_managed_user"`
(from `FakeVaultServer`), not `"udabank_app"`.
