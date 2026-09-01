FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared/pom.xml shared/pom.xml
COPY survey/pom.xml survey/pom.xml
COPY restore/pom.xml restore/pom.xml
COPY shared/src shared/src
COPY survey/src survey/src
COPY restore/src restore/src
RUN mvn --batch-mode --no-transfer-progress -pl survey -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/survey/target/survey-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]