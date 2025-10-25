FROM gradle:9.1.0-jdk25 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./
RUN gradle dependencies --no-daemon || true
COPY src ./src
RUN gradle clean build --no-daemon -x test

FROM openjdk:25-jdk-slim AS final
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
