FROM eclipse-temurin:17-jre
WORKDIR /kareer/ms-user

RUN groupadd -r kareer && useradd -g kareer kareer
USER kareer:kareer

COPY ./build/libs/*.jar ./app.jar

ENTRYPOINT ["java","-jar","app.jar"]
