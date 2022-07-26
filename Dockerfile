FROM openjdk:17 AS build

RUN microdnf install findutils # includes xargs required for gradle
COPY . .
RUN ./gradlew bootJar


FROM eclipse-temurin:17-jre
WORKDIR /kareer/ms-user

RUN groupadd -r kareer && useradd -g kareer kareer
USER kareer:kareer
COPY --from=build ./build/libs/*.jar ./app.jar

ENTRYPOINT ["java","-jar","app.jar"]
