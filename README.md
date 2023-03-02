# kareer-ms-user

Microservice for user management. JSON REST API written in Kotlin using the Spring Boot framework. API docs are automatically published on `/swagger`, generated from the OpenAPI 3 spec yaml available on `/openapi`.

Repository is split up in `ms-user` containing the actual application, and `lib-user` containing DTOs so other Kotlin/Java microservices that make requests to this one, can depend on this library to include the correct DTOs.

## Documentation

- [Microservice configuration (Confluence)](https://kommaboard.atlassian.net/wiki/spaces/KRR/pages/33296/Microservice+properties#ms-user)
- [OpenApi 3 spec (locally deployed)](http://kareer.internal:8006/api/openapi)
- [Swagger API docs (locally deployed)](http://kareer.internal:8006/api/swagger)
