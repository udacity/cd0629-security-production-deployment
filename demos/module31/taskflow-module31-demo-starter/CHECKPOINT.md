# Checkpoint: taskflow-module31-demo-starter

Git tag: `module-31-demo-starter`

This is Module 29's corrected final state (deliberately-failing test fixed, matching what the recording itself does) - Hibernate manages schema via create-drop, one single Kubernetes Deployment/Service, no Flyway, no Blue-Green concept. The Demo's point: showing both real gaps explicitly - a database that resets on every restart, and no clean way to instantly roll back a bad deployment.
