FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    apt-get update && apt-get install -y --no-install-recommends maven && \
    mvn -q -DskipTests package && \
    cp target/*.jar /build/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
