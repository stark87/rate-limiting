FROM eclipse-temurin:25-jre
LABEL authors="STARK"

WORKDIR /app
COPY /target/*.jar /app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -Dspring.profiles.active=$SPRING_PROFILE -jar rate-limiting-0.0.1-SNAPSHOT.jar"]