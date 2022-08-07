# kareer-ms-user

Microservice for user management. Spins up a web server exposing REST endpoints on port 32002. API docs are available on `/swagger` generated from the OpenAPI 3 spec yaml available on `/openapi`.

Repository is split up in `ms-user` containing the actual application, and `lib-user` containing DTOs so other microservices that make requests to this one, can depend on this library to include the correct DTOs.

## Configuration

Application configuration can be found in `src/main/resources/application.yml`. When run in Kubernetes, these properties can be overwritten by a ConfigMap that has the same name as the microservice (see `k8s/ms-user-configmap.yml`). Changing values in `application.yml` requires an application
restart, but in Kubernetes the application detects changes to the ConfigMap and applies them automatically.

| Key                  | Type   | Value                                                                                                                                                  |
|----------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `consumer-id`        | String | Value used for identification when communicating between services. Must be the same value across all microservices for authorization to be successful. |
| `salt`               | String | Salt used when hashing passwords before sending them to the database. Must be a valid BCrypt salt. See below.                                          |
| `salt`               | String | Salt used when hashing passwords before sending them to the database. Must be a valid BCrypt salt. See below.                                          |
| `admin-email`        | String | E-mail address for the default admin user that is created when no users exist in the database.                                                         |
| `admin-password`     | String | Password for the default admin user that is created when no users exist in the database.                                                               |
| `confirm-email-ttl`  | Int    | Time To Live for tickets of the `CONFIRM_EMAIL` type. Basically how long the link to confirm your e-mail address remains valid.                        |
| `reset-password-ttl` | Int    | Time To Live for tickets of the `RESET_PASSWORD` type. Basically how long the link to reset your password remains valid.                               |

This oneliner will generate a BCrypt salt of cost 12 based of the value "password". You can increase/decrease cost or change the value to your liking. More info [here](https://unix.stackexchange.com/a/419855).

```shell
echo $(htpasswd -bnBC 12 "" password | tr -d ':\n' | sed 's/$2y/$2a/')
```

## Deployment

### Run locally

Requires `http://kareer.local` to point to your local host. Expects a Postgres database `kareer` running on port 5432 and a user `admin` with password `admin` with full permissions on this database. Requires Java 17 to build the project. Gradle is not required as a wrapper is included.

```shell
cd ms-user
# Update hosts file if required
echo '127.0.0.1  kareer.local' | sudo tee -a /etc/hosts
# Run Postgres in Docker if required
docker compose run -d -p 5432:5432 kareer-postgres
# Build and run the microservice
./gradlew bootRun
```

### Run with Docker Compose

Included in the `ms-user` subdirectory is a `docker-compose.yml` file which will also build the Docker images if not found locally.

```shell
cd ms-user
# Perhaps first remove old containers (eg Postgres is shared, might give conflicts)
docker container prune -f
# Perhaps first remove old images (to make sure new images are build using latest code)
docker rmi $(docker images --format '{{.Repository}}:{{.Tag}}' | grep 'kareer')
# Let's get this bread
docker compose run -d -p 5432:5432 kareer-postgres
```

### Run with Skaffold

Included in the `ms-user` subdirectory is a `skaffold.yml` file to automatically watch, build and deploy the microservice and its related Kubernetes configuration. Requires the namespace and permission configuration from the `kareer-deployment` repository and assumes that repository is checked out
in the same parent directory as this one.

```shell
cd ms-user
# Start minikube unless you're using a different Kubernetes implementation
minikube start
# Link Docker CLI to Minikube's Docker daemon first
eval $(minikube -p minikube docker-env)
# Build and deploy, and watch for changes
skaffold dev --toot=true --tag "0.0.1"
```

