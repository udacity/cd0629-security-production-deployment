# Checkpoint: taskflow-module23-solution

Git tag: `module-23-solution`

This is Module 23's completed solution - Prometheus and Grafana added via docker-compose.yml (Grafana on host port 3001, NOT the default 3000, to avoid colliding with Module 11's cors-frontend), a real Counter metric in PartnerController tagged by outcome, percentile histograms enabled for http.server.requests, liveness/readiness health probes enabled, and a genuine security fix: /actuator/refresh now requires MANAGER role (closing a gap open since Module 17). Includes alerts.yml, a real Prometheus alert rule matching Module 22's 9-second threshold example. IMPORTANT: prometheus.yml scrapes TaskFlow via host.docker.internal since TaskFlow runs natively, not in Docker - see README's Module 23 section for the full setup and recording flow.
