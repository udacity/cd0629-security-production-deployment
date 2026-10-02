# Module 25: multi-stage build, matching Module 24's exact pattern.
# Stage 1 compiles the app in a full JDK image (with Maven bundled in,
# so no wrapper script is needed — this project has only ever used a
# plain, locally-installed `mvn` all course). Stage 2 only copies the
# finished JAR into a slim JRE image — the compiler, Maven itself, and
# everything else from the build stage never makes it into what
# actually ships.

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy ONLY the file Maven needs to resolve dependencies first. This
# layer only invalidates when pom.xml itself changes — not every time a
# single Java file changes. That's the "fast way" from Module 24: the
# slow, large dependency download stays cached across almost every
# rebuild.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the actual source. Changing a Java file only invalidates
# this layer and everything after it — the dependency layer above is
# untouched.
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime stage ----
# JRE, not JDK — no compiler, no build tooling, nothing an attacker
# could use if they ever got a shell inside this container. Alpine
# specifically, per Module 24's minimal-base-image guidance: fewer
# system packages, smaller attack surface, smaller image.
FROM eclipse-temurin:25-jre-alpine

# Never run as root. Module 24 was explicit: if this app is ever
# compromised, root inside the container is a much worse outcome than
# a limited, unprivileged user.
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
USER taskflow

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
