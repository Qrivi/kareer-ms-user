# Build step
FROM gradle:7.5.0-jdk17 AS build
WORKDIR /opt

# Download and install dependencies
COPY ./build.gradle.kts ./settings.gradle.kts ./
RUN gradle clean build > /dev/null 2>&1 || true

# Build and package the application
COPY . ./
RUN gradle bootJar

# Deploy step
FROM eclipse-temurin:17-jre
WORKDIR /kareer/ms-user

# Update, upgrade and install jq for health check
RUN apt-get update && apt-get upgrade -y && apt-get install -y jq

# Create a user with limited permissions
RUN groupadd -r kareer && useradd -g kareer kareer
USER kareer:kareer

# Get all our stuff together
COPY --from=build /opt/build/libs/*.jar ./app.jar


# Run
EXPOSE 8006
LABEL maintainer="Komma Board <kareer@kommaboard.be>"
ENTRYPOINT ["java", "-XX:InitialRAMPercentage=60", "-XX:MaxRAMPercentage=90", "-XX:+HeapDumpOnOutOfMemoryError", "-jar", "app.jar"]

# Health check
HEALTHCHECK --start-period=30s --interval=30s --timeout=10s --retries=3 \
  CMD curl --silent --fail --request GET http://localhost:8006/api/actuator/health \
    | jq --exit-status -n 'inputs | if has("status") then .status=="UP" else false end' > /dev/null || exit 1

