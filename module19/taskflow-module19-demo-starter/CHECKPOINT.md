# Checkpoint: taskflow-module19-demo-starter

Git tag: `module-19-demo-starter`

This is Module 17's final solution state, plus a debug log line in PartnerController that leaks the raw partner API key in plain text on every request (a realistic 'added during debugging, never cleaned up' scenario). No structured logging, no correlation IDs yet. Same infrastructure needs as Module 17 (Keycloak + Vault via Docker if continuing from earlier modules; Config Server not required for this module's specific tests, though it can stay running if continuing straight through).
