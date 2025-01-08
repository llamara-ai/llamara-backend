# LLAMARA - Build & Publish

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Building the Docker image

You can optionally build a Docker container to run the application:

```shell script
./mvnw package
docker build -f src/main/docker/Dockerfile.jvm -t llamara-ai/llamara-backend-jvm --build-arg BUILD_DATE=$(date +"%Y-%m-%dT%H:%M:%SZ") --build-arg VCS_REF=$(git rev-parse HEAD) --build-arg LLAMARA_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout) .
```

Given the required configuration in `config/application.yaml` and the environment variables in `.env`, you can run the Docker container:

```shell script
docker run -i --rm -p 8080:8080 -v ./config:/config --env-file .env docker.io/llamara-ai/llamara-backend-jvm
```

## Publishing a new version

To publish a new version of the application, use the [Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/).

```shell script
./mvnw release:prepare -DpushChanges=false -DtagNameFormat=v@{project.version}
```

Push the changes to GitHub and the CI will automatically build and publish the Docker container.

After the release, clean up:

```shell script
./mvnw release:clean
```
