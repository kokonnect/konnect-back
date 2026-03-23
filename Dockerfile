# build stage
FROM gradle:8-jdk17 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle build -x test || true

COPY . .
RUN gradle build -x test

# runtime stage
FROM eclipse-temurin:17-jdk-jammy
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]