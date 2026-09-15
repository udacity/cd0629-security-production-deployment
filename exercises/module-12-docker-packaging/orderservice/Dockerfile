# --- Stage 1: build the jar ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    apt-get update && apt-get install -y --no-install-recommends maven && \
    mvn -q -DskipTests package && \
    cp target/*.jar /build/app.jar

# --- Stage 2: split the jar into layers (dependencies change far less
# often than application code, so this is what makes rebuilds fast — only
# the "application" layer's Docker cache gets busted on a normal code
# change, not the ~200MB of third-party dependencies). ---
FROM eclipse-temurin:25-jdk AS extract
WORKDIR /extract
COPY --from=build /build/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# --- Stage 3: runtime image — no JDK, no Maven, no source, just a JRE
# and the four extracted layers, ordered least-to-most likely to change. ---
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./
EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
