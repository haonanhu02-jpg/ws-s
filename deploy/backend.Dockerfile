FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
ARG MODULE
ARG MAVEN_CONNECT_TIMEOUT_MS=10000
ARG MAVEN_READ_TIMEOUT_MS=60000
# Keep batch transfer logs visible so a stalled dependency identifies its URL.
# requestTimeout limits network inactivity, not total build duration.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -e \
      -Daether.transport.http.connectTimeout=${MAVEN_CONNECT_TIMEOUT_MS} \
      -Daether.transport.http.requestTimeout=${MAVEN_READ_TIMEOUT_MS} \
      -Daether.transport.http.retryHandler.count=1 \
      -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
