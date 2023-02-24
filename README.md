# kareer-ms-user

Microservice for user management. JSON REST API written in Kotlin using the Spring Boot framework. API docs are automatically published on `/swagger`, generated from the OpenAPI 3 spec yaml available on `/openapi`.

Repository is split up in `ms-user` containing the actual application, and `lib-user` containing DTOs so other Kotlin/Java microservices that make requests to this one, can depend on this library to include the correct DTOs.

## Documentation

- [Microservice configuration (Confluence)](https://kommaboard.atlassian.net/wiki/spaces/KRR/pages/33296/Microservice+properties#ms-user)
- [OpenApi 3 spec (locally deployed)](http://kareer.internal:8006/api/openapi)
- [Swagger API docs (locally deployed)](http://kareer.internal:8006/api/swagger)

## Artifacts

- `lib-user` is published as a Java library to [maven.qrivi.dev](https://maven.qrivi.dev) (public Maven repository). Needs to be build and pushed manually by a developer.
- `ms-user` is published as a Docker image to [docker.qrivi.dev](https://docker.qrivi.dev) (private Docker registry). Is built and publisched automatically by GitHub Actions CI/CD.
