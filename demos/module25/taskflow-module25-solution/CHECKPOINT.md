# Checkpoint: taskflow-module25-solution

Git tag: `module-25-solution`

This is Module 25's completed solution - a real multi-stage Dockerfile (maven:3.9-eclipse-temurin-25 build stage, eclipse-temurin:25-jre-alpine runtime stage, non-root user, dependency-layer caching) plus TaskFlow added to docker-compose.yml as an opt-in 'containerized' profile service. IMPORTANT: two real, documented limitations - (1) Keycloak/Vault are reached via Docker service names (keycloak:8080, vault:8200) once TaskFlow itself is containerized, not localhost, and (2) a JWT obtained via Keycloak's host-facing URL will NOT validate against the containerized issuer-uri (a genuine Docker+Keycloak mismatch, not solved here - test with permitAll endpoints only). See README's Module 25 section for the full build/run/compose recording flow, including the concrete layer-caching proof (change a file, rebuild, watch the dependency layer stay CACHED).
