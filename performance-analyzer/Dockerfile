FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY generics/pom.xml generics/pom.xml
COPY generics/src generics/src
RUN mvn -f generics/pom.xml -DskipTests install
COPY performance-analyzer/pom.xml performance-analyzer/pom.xml
COPY performance-analyzer/src performance-analyzer/src
RUN mvn -f performance-analyzer/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/performance-analyzer/target/performance-analyzer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
