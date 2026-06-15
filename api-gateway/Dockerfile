# syntax=docker/dockerfile:1.7

FROM maven:3.9.12-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY api-gateway/pom.xml api-gateway/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -f api-gateway/pom.xml dependency:go-offline

COPY api-gateway/src api-gateway/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f api-gateway/pom.xml package -DskipTests \
    && cp api-gateway/target/api-gateway-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app
EXPOSE 8080 9090
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
