# Module 29: the CONTROLLER only. It schedules jobs and (for the final
# stage) builds the actual app image — it does NOT compile or test
# TaskFlow itself. That happens in ephemeral agent containers based on
# maven:3.9-eclipse-temurin-25 (Module 25's exact image), spun up by the
# docker-workflow plugin per stage. This mirrors Module 28's own
# controller-agent diagram directly: one controller, lightweight agents
# doing the actual compiling and testing.
#
# Staying on the official jenkins/jenkins:lts-jdk21 image deliberately
# — Jenkins' own JVM compatibility can lag behind the very latest JDK
# releases, so this project's JDK 25 stays confined to the build agents
# that actually need it, not baked into the thing running Jenkins itself.
FROM jenkins/jenkins:lts-jdk21

USER root

# Docker CLI only — no daemon. This controller talks to the HOST's
# Docker daemon via the mounted socket (docker-compose.yml), the same
# "Docker-outside-of-Docker" pattern, not Docker-in-Docker. Worth
# naming explicitly: mounting the host's Docker socket effectively
# gives this container root-equivalent access to the host. Fine for a
# local, throwaway CI demo; a real production Jenkins setup would use a
# more locked-down agent/executor model instead.
RUN apt-get update && apt-get install -y docker.io && rm -rf /var/lib/apt/lists/*

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt

COPY init.groovy.d/basic-security.groovy /usr/share/jenkins/ref/init.groovy.d/basic-security.groovy

ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true"

# Deliberately staying as root, not switching back to the jenkins user.
# The mounted Docker socket's group ownership varies across host OSes
# (Mac Docker Desktop, Linux, Windows all differ), and a uid/gid
# mismatch here is a common, confusing "permission denied
# /var/run/docker.sock" error. Since mounting that socket already grants
# root-equivalent host access regardless of which user owns the
# process, staying root doesn't meaningfully change the security
# posture — it just avoids a real, common setup failure. Not a choice
# to repeat for a real production Jenkins controller.
