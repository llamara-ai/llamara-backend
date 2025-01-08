# LLAMARA - Large Language Assistant for Model-Augmented Retrieval and Analysis

LLAMARA is an LLM-based assistant for information retrieval from a provided knowledge base.
It aims at supporting researchers working with scientific papers, whitepapers and documentation,
as well as possibly serving research findings in an accessible way to the public.

> **NOTE:** This repository contains the LLAMARA backend only.

## Introduction

This project uses [Quarkus](https://quarkus.io), the Supersonic Subatomic Java Framework, and [LangChain4j](https://docs.langchain4j.dev/).
It is build with Java 21 and Maven 3.9.9.

If you need to have a look at its REST API, check out Swagger UI on <http://localhost:8080/q/swagger-ui>.

## Configuration

LLAMARA comes with a default configuration that can be overridden by providing an `application.yaml` file in the [config](config) directory.
Refer to [config/README.md](config/README.md) for more information.

## Running LLAMARA

The easiest way to run LLAMARA is to use the provided Docker container with Docker Compose.

Please refer to [llamara-docker](https://github.com/llamara-ai/llamara-docker) for more information.

## Dependencies

> For development, you only need to set up the [OpenAI API](#openai-api) dependency, as the other dependencies are provided by Quarkus Dev Services.
> See [DEVELOPMENT.md](DEVELOPMENT.md) for more information.

### Authentication

This application requires an OIDC authentication provider to be set up.
The OIDC provider requires the `auth-server-url` and `client-id` to be set in the `application.yaml` file and the `QUARKUS_OIDC_CREDENTIALS_SECRET` environment variables.
For Keycloak, you need to add the `microprofile-jwt` and `profile` scopes for the Quarkus client, see [Keycloak Server Documentation](https://www.keycloak.org/docs/latest/server_admin/#protocol).

### OpenAI API

This application is using Large-Language-Models for chat and embedding models for RAG from OpenAI.
You therefore need to provide an OpenAI API key through the `OPENAI_API_KEY` environment variable, e.g. through an `.env` file.

### Databases & Object Storage

#### PostgreSQL

The application requires a [PostgreSQL](https://www.postgresql.org/) database on `localhost:5432` (default).
If needed, specify a password through the `QUARKUS_DATASOURCE_PASSWORD` environment variable.

The application requires its tables to be available in the configured JDBC database.

#### MinIO

The application requires a [MinIO](https://min.io) object storage on `localhost:9000` (default).
You need to set up access and secret key through the MinIO web interface and provide them through the `QUARKUS_MINIO_ACCESS_KEY` and `QUARKUS_MINIO_SECRET_KEY` environment variables.

#### Redis

This application requires a [Redis](https://redis.io/json) server on `localhost:6379` (default).
It uses database 1 (default) for chat memory and database 2 (default) for chat history.
If needed, specify passwords through the `QUARKUS_REDIS_CHAT_MEMORY_PASSWORD` and `QUARKUS_REDIS_CHAT_HISTORY_PASSWORD` environment variables.

#### Qdrant

This application requires a [Qdrant Vector Database](https://qdrant.tech/qdrant-vector-database/) on `localhost:6334` (gRPC) (default).
If needed, specify an API key through the `QDRANT_API_KEY` environment variable.

Before using Qdrant, you need to create the required collection:

1. Visit <http://localhost:6333/dashboard#/tutorial/quickstart>
1. Create a collection named `text-embedding-3-large` with the vector size matching the used embedding model
1. Enable payload index for the `knowledge_id` payload key by executing the following in <http://localhost:6333/dashboard#/console>:
   ```
   PUT /collections/text-embedding-3-large/index
     {
       "field_name": "knowledge_id",
       "field_schema": "uuid"
     }
   ```

| Provider | Embedding Model          | Vector Size | Distance Calculation | Refs                                                       |
|----------|--------------------------|-------------|----------------------|------------------------------------------------------------|
| OpenAI   | `text-embedding-3-small` | 1536        | Dot Product          | [Docs](https://platform.openai.com/docs/guides/embeddings) |
| OpenAI   | `text-embedding-3-large` | 3072        | Dot Product          | [Docs](https://platform.openai.com/docs/guides/embeddings) |
| Ollama   | `nomic-embed-text`       | 768         | Dot Product          | [Ollama](https://ollama.com/library/nomic-embed-text)      |

## Known Issues

- Filtering embeddings by permissions in the retrieval step only works if knowledge has only a single permission set.
- Administrators can manage all knowledge, but unfortunately they cannot use all knowledge in the retrieval step.
