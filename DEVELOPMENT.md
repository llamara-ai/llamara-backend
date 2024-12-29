# LLAMARA - Development

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

You can access the Quarkus Dev UI at <http://localhost:8080/q/dev/>.
You can access Swagger UI at <http://localhost:8080/q/swagger-ui>.

You can log in with Keycloak at <http://localhost:8080/q/dev-ui/io.quarkus.quarkus-oidc/keycloak-provider> using the following credentials:

- `alice` (admin & user role): password is `alice`
- `bob` (user role): password is `bob`

After logging in, click on "Test Your Service" -> "Swagger UI" to open the Swagger UI as an authenticated user.

You can also access the Keycloak admin console through the Dev UI, use `admin` as the username and `admin` as the password.

## Formatting the code

This project is using [Spotless Maven Plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven) for code formatting.
You can format your code using:

```shell script
./mvnw spotless:apply
```

## Adding the license header

This project is using [License Maven Plugin](https://www.mojohaus.org/license-maven-plugin/) for adding the license header to the source files.

You can add the license header to your source files using:

```shell script
./mvnw license:update-file-header
```

## Upgrading the application

To upgrade Quarkus execute the following command:

```shell script
./mvnw quarkus:update -N
```

Always check the `pom.xml` file afterward and sync related dependencies.

Other dependencies have to be upgraded manually in the `pom.xml` file.

## Configuring the application

LLAMARA comes with a default configuration for development that utilises [Quarkus Dev Services](https://quarkus.io/guides/dev-services) to provide all required external dependencies.
If you don't want to use the dev services, but rather provider your own configuration for the databases and object storage, you can do so by providing an `application.yaml` file in the [config](config) directory.
Use the `dev-external` profile:

```yaml
"%dev-external":
  quarkus:
    ...
```

## Documentation

### CDI

- [Introduction to Contexts and Dependency Injection](https://quarkus.io/guides/cdi)
- [Contexts and Dependency Injection](https://quarkus.io/guides/cdi-reference)

### Configuration

- [Configuration Reference](https://quarkus.io/guides/config-reference)
- [YAML Configuration](https://quarkus.io/guides/config-yaml)

### REST

- [Quarkus REST](https://quarkus.io/guides/rest)
- [Jackson JSON Processor](https://github.com/FasterXML/jackson-docs)

### Hibernate ORM/Reactive

- [Using Hibernate ORM](https://quarkus.io/guides/hibernate-orm)
- [Simplified Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [Using Hibernate Reactive](https://quarkus.io/guides/hibernate-reactive)
- [Simplified Hibernate Reactive with Panache](https://quarkus.io/guides/hibernate-reactive-panache)

### MinIO

- [Quarkus MinIO Client Extension](https://docs.quarkiverse.io/quarkus-minio/dev/index.html)
- [MinIO Java SDK](https://min.io/docs/minio/linux/developers/java/API.html)
- [Amazon S3 Operations](https://docs.aws.amazon.com/AmazonS3/latest/API/API_Operations_Amazon_Simple_Storage_Service.html)

### Redis

- [Redis Guide](https://quarkus.io/guides/redis)
- [Redis Reference](https://quarkus.io/guides/redis-reference)

### OIDC / Keycloak

- [Protect a service application by using OpenID Connect (OIDC) Bearer token authentication](https://quarkus.io/guides/security-oidc-bearer-token-authentication-tutorial)
- [Quarkus OpenID Connect (OIDC) Bearer token authentication](https://quarkus.io/guides/security-oidc-bearer-token-authentication)

### LangChain4j

- [LangChain4j](https://docs.langchain4j.dev/)
- [Quarkus LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)

### Reactive Programming

- [Getting Started with Reactive](https://quarkus.io/guides/getting-started-reactive)
- [Mutiny - Async for Mere Mortals](https://quarkus.io/guides/mutiny-primer)
- [SmallRye Mutiny: From Imperative to Reactive](https://smallrye.io/smallrye-mutiny/latest/guides/imperative-to-reactive)
