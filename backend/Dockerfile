FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/spaceora-backend-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
