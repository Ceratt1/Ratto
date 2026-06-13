# syntax=docker/dockerfile:1.7

FROM maven:3.9.12-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY generics/pom.xml generics/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f generics/pom.xml dependency:go-offline

COPY generics/src generics/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f generics/pom.xml install -DskipTests

COPY producer/pom.xml producer/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f producer/pom.xml dependency:go-offline

COPY producer/src producer/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f producer/pom.xml package -DskipTests \
    && cp producer/target/producer-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build --chown=app:app /workspace/app.jar app.jar

USER app
EXPOSE 8070 9090

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
