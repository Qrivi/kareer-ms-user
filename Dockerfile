# Build step
FROM gradle:7.5.0-jdk17 AS build
WORKDIR /opt

# Download and install dependencies
COPY ./build.gradle.kts ./settings.gradle.kts ./
RUN gradle clean build --no-daemon > /dev/null 2>&1 || true

# Build and package the application
COPY . ./
RUN gradle bootJar

# Deploy step
FROM eclipse-temurin:17-jre
WORKDIR /kareer/ms-user

# Create a user with limited permissions
RUN groupadd -r kareer && useradd -g kareer kareer
USER kareer:kareer

# Get the app and mark it as entrypoint
COPY --from=build ./build/libs/*.jar ./app.jar
ENTRYPOINT ["java","-jar","app.jar"]
