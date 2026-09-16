# Checkpoint: taskflow-module23-demo-starter

Git tag: `module-23-demo-starter`

This is Module 21's final solution state, unchanged - /actuator/refresh is still implicitly wide open under anyRequest().permitAll() (a real gap that's existed since Module 17), no custom metrics beyond Spring Boot's automatic HTTP metrics, no Prometheus, no Grafana. The Demo's point is proving the refresh endpoint has zero auth right now, and that /actuator/health only gives a flat UP/DOWN with no real detail. Same infrastructure needs as Module 21 (Keycloak + Vault + Elasticsearch/Kibana via Docker if continuing from earlier modules).
