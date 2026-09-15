# Solution: Apply Secret Management with Vault

## What changed

[`application.yml`](src/main/resources/application.yml):

```yaml
spring:
  config:
    import: "vault://secret/authdemo"
  cloud:
    vault:
      uri: http://127.0.0.1:18200
      token: test-token
      kv:
        enabled: true
        backend: secret
        default-context: authdemo
```

`db.username` / `db.password` are gone from this file entirely — they now
resolve from Vault's `secret/authdemo` KV entry at startup.

## A real gotcha hit while building this

Spring Cloud Vault's KV backend defaults to v1-style paths
(`GET /v1/secret/authdemo`) unless it successfully auto-detects a v2 mount
via a separate probe request first. I initially built `FakeVaultServer`
assuming KV v2 (the more common modern setup, with its nested
`data.data`/`data.metadata` response shape) and spent a while chasing
`spring.cloud.vault.kv.backend-version` and mount-detection responses
before just... implementing v1 instead, which is what the client actually
requests by default and is a simpler response shape besides. If you're
pointing this at a real Vault server that's mounted as KV v2 (Vault's own
default when you `vault secrets enable -version=2 kv`), you'll want to
account for that — the path and response shape genuinely differ.

## Verifying

```bash
./mvnw test -Dtest=VaultConfigTest
# 1 test, green
```

```bash
./mvnw spring-boot:run
curl http://localhost:8080/internal/db-status
# {"username":"vault_managed_user","passwordConfigured":true}
```
