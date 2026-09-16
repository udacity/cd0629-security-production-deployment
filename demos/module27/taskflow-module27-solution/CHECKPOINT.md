# Checkpoint: taskflow-module27-solution

Git tag: `module-27-solution`

This is Module 27's completed solution - a k8s/ directory with ConfigMap (non-sensitive Vault URI), Secret (base64-encoded Vault token, with an explicit note that base64 is NOT encryption), Deployment (replicas: 2, imagePullPolicy: Never for the locally-built image, health probes wired to Module 23's existing /actuator/health/liveness and /actuator/health/readiness endpoints), and a ClusterIP Service. Uses Docker Desktop's built-in Kubernetes - no minikube/kind needed. IMPORTANT: requires Docker Desktop's Kubernetes enabled (Settings > Kubernetes > Enable Kubernetes, takes several minutes first time, do ahead of recording) and the image built locally first. Same Keycloak issuer-mismatch limitation as Module 25 applies here too - test with permitAll endpoints. See README's Module 27 section for the full apply/port-forward/self-healing-proof recording flow.
