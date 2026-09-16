# Checkpoint: taskflow-module17-solution

Git tag: `module-17-solution`

This is Module 17's completed solution - TaskFlow is now a Spring Cloud Config client, pulling partner.request.timeout-seconds from a real git-backed Config Server instead of a local file, with @RefreshScope enabling live updates via POST /actuator/refresh, no restart needed. REQUIRES the separate config-server and taskflow-config-repo projects (delivered separately) to be running on port 8888 - see README's Module 17 section for the full three-terminal setup (TaskFlow, Config Server, and Keycloak/Vault if continuing from earlier modules).
