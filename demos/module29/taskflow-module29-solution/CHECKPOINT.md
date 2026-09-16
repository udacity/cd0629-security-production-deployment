# Checkpoint: taskflow-module29-solution

Git tag: `module-29-solution`

This is Module 29's completed solution - real Jenkins CI/CD via docker-compose (opt-in 'containerized' profile), a genuine controller-agent pipeline (Jenkinsfile: Checkout -> Build -> Test -> Package -> Docker Build, using ephemeral maven:3.9-eclipse-temurin-25 agent containers per stage, matching Module 25's exact image), and the FIRST real tests in this entire project (TaskServiceTest.java - 3 passing, 1 DELIBERATELY FAILING for the demo's red-to-green moment). Jenkins boots with zero manual setup (admin/admin, no setup wizard) via a Groovy init script, same dev-mode pattern as Keycloak/Vault. IMPORTANT: requires manually creating the Jenkins Pipeline job pointing at file:///taskflow-source (one-time, doesn't self-configure) - see README's Module 29 section for the exact steps and full recording flow, including the intentional test failure and fix.
