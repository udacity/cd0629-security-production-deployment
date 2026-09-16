# Checkpoint: taskflow-module15-solution

Git tag: `module-15-solution`

This is Module 15's completed solution — the hardcoded secret is gone from application.yml, replaced with spring.config.import: vault:// pulling partner.api-key from a real HashiCorp Vault instance (dev mode, via docker-compose.yml). PartnerApiKeyProperties.java is byte-for-byte unchanged from the starter - that's the actual point. IMPORTANT: requires a one-time 'vault kv put' to actually store the secret, and VAULT_TOKEN must be exported as an environment variable before running the app - see README for exact commands.
