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
WORKDIR /kareer

# Update, upgrade and install jq for health check
RUN apt-get update && apt-get upgrade -y && apt-get install -y jq

# Create a user with limited permissions
RUN groupadd -r kareer && useradd -g kareer kareer
USER kareer:kareer

# Get all our stuff together
COPY --from=build /opt/build/libs/*.jar ./kareer-ms-user.jar


# Add some info to the image
EXPOSE 8006
LABEL maintainer="Komma Board <kareer@kommaboard.be>"
LABEL org.opencontainers.image.vendor="Komma Board <kareer@kommaboard.be>"
LABEL org.opencontainers.image.title="kareer-ms-user"
LABEL org.opencontainers.image.name="kareer-ms-user"
LABEL org.opencontainers.image.description="Kareer microservice for user management"

# Delayed run (to startup failures and log spam because of dependencies still starting up)
ENTRYPOINT [ "/bin/bash", \
  "-c", \
  "echo Startup in 10s…; sleep 10; java -XX:InitialRAMPercentage=60 -XX:MaxRAMPercentage=90 -XX:+HeapDumpOnOutOfMemoryError -jar kareer-ms-user.jar" ]

# Health check
HEALTHCHECK --start-period=30s --interval=30s --timeout=10s --retries=3 \
  CMD curl --silent --fail --request GET http://localhost:8006/api/actuator/health \
    | jq --exit-status -n 'inputs | if has("status") then .status=="UP" else false end' > /dev/null || exit 1

