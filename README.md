# LLAMARA - Large Language Assistant for Model-Augmented Retrieval and Analysis

LLAMARA is an LLM-based assistant for information retrieval from a provided knowledge base.
It aims at supporting researchers working with scientific papers, whitepapers and documentation,
as well as possibly serving research findings in an accessible way to the public.

> **NOTE:** This repository contains the LLAMARA backend only.

## Features

- **Retrieval-Augmented-Generation (RAG)** chat functionality with provided knowledge
- Support uploading the following file types as knowledge source:
  - PDF
  - Microsoft Word DOCX
  - Markdown
  - TXT
  - and everything else supported by [Apache Tika](https://tika.apache.org/)
- Provide **reference to the source** of knowledge in the generated response with the ability to download the file-based source
- **Interchangeable LLM** (chat model) for each request
- Extensive **management of knowledge** (including the ability to set label and tags)
- **Multi-User support** with Single-Sign-On (SSO) support through external OIDC providers (we recommend [Keycloak](https://www.keycloak.org/)):
  - Admins can manage all knowledge added to LLAMARA.
  - Users can add individual knowledge and share it with other users of LLAMARA through fine-grained permissions.
  - Anonymous access can be enabled to allow anyone to make us of the publicly shared knowledge.
- **Multiple sessions** per user with server-side chat history
- Integration with the following LLM (chat model) providers:
  - [OpenAI](https://platform.openai.com/docs/models#models-overview)
  - [Microsoft Azure OpenAI](https://azure.microsoft.com/en-us/products/ai-services/openai-service/)
  - [Ollama](https://ollama.com/)
- Integration with the following embedding model providers:
  - [OpenAI](https://platform.openai.com/docs/models#embeddings)
  - [Microsoft Azure OpenAI](https://azure.microsoft.com/en-us/products/ai-services/openai-service/)
  - [Ollama](https://ollama.com/)
- Integration with the following embedding stores:
  - [Qdrant](https://qdrant.tech/)
- Build with [Quarkus](https://quarkus.io/), the Supersonic Subatomic Java Framework
- Uses [LangChain4j](https://docs.langchain4j.dev/), the versatile LLM integration library
- Relies on battle-tested open-source software such as [PostgreSQL](https://www.postgresql.org/), [MinIO](https://min.io/) & [Redis](https://redis.io/)

## Configuration

LLAMARA comes with a default configuration that can be overridden by providing an `application.yaml` file in the [config](config) directory.
Refer to [config/README.md](config/README.md) for more information.

## Running LLAMARA

The easiest way to run LLAMARA is to use the provided Docker container with Docker Compose.

Please refer to [llamara-docker](https://github.com/llamara-ai/llamara-docker) for more information.

## Dependencies

> For development, you only need to set up the [AI Model Provider](#ai-model-provider) dependency, as the other dependencies are provided by Quarkus Dev Services.
> See [DEVELOPMENT.md](DEVELOPMENT.md) for more information.

### Authentication

This application requires an OIDC authentication provider to be set up.
The OIDC provider requires the `auth-server-url` and `client-id` to be set in the `application.yaml` file and the `QUARKUS_OIDC_CREDENTIALS_SECRET` environment variables.
For Keycloak, you need to add the `microprofile-jwt` and `profile` scopes for the Quarkus client, see [Keycloak Server Documentation](https://www.keycloak.org/docs/latest/server_admin/#protocol).

### AI Model Provider

With the default configuration, LLAMARA relies on GPT-4o mini and `text-embeddding-3-large` from OpenAI.
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

LLAMARA will create the required collection according to the configured collection name and vector size,
and enable payload index for the `knowledge_id` payload key.

<details>

<summary>How to do that manually</summary>

Before using Qdrant, you need to create the required collection:

1. Visit <http://localhost:6333/dashboard#/tutorial/quickstart>
1. Create a collection `${COLLECTION_NAME}` with the vector size matching the used embedding model
1. Enable payload index for the `knowledge_id` payload key by executing the following in <http://localhost:6333/dashboard#/console>:
   ```
   PUT /collections/${COLLECTION_NAME}/index
     {
       "field_name": "knowledge_id",
       "field_schema": "uuid"
     }
   ```

`${COLLECTION_NAME}` is the configured collection name.

</details>

Common embedding models and their vector size and recommended distance calculation are:

| Provider | Embedding Model          | Vector Size | Distance Calculation | Refs                                                       |
|----------|--------------------------|-------------|----------------------|------------------------------------------------------------|
| OpenAI   | `text-embedding-3-small` | 1536        | Dot Product          | [Docs](https://platform.openai.com/docs/guides/embeddings) |
| OpenAI   | `text-embedding-3-large` | 3072        | Dot Product          | [Docs](https://platform.openai.com/docs/guides/embeddings) |
| Ollama   | `nomic-embed-text`       | 768         | Dot Product          | [Ollama](https://ollama.com/library/nomic-embed-text)      |

## Endpoints

### REST API

LLAMARA backend provides a REST API to interface with it.
You can explore it through Swagger UI on <http://localhost:8080/q/swagger-ui>,
or use the OpenAPI YAML or JSON produces as build artifact in [CI](https://github.com/llamara-ai/llamara-backend/actions/workflows/ci-build.yaml).

### Info Endpoint

The <http://localhost:8080/q/info> endpoint provides information about LLAMARA backend, including Git, Java, OS and build details.

### Health Endpoint

LLAMARA backend exposes four REST endpoints according to the [Eclipse Microprofile specification](https://github.com/eclipse/microprofile-health/):

- `/q/health/live`: The application is up and running.
- `/q/health/ready`: The application is ready to serve requests.
- `/q/health/started`: The application is started.
- `/q/health`: Accumulating all health check procedures in the application.

## Known Issues

- Filtering embeddings by permissions in the retrieval step only works if knowledge has only a single permission set.
