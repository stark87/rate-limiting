FROM eclipse-temurin:25-jre
LABEL authors="STARK"

WORKDIR /app
COPY /target/*.jar /app

ENTRYPOINT ["java", "-jar", "rate-limiting-0.0.1-SNAPSHOT.jar"]