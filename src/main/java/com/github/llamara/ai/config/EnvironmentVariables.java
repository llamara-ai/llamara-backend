/*
 * #%L
 * llamara-backend
 * %%
 * Copyright (C) 2024 - 2025 Contributors to the LLAMARA project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.llamara.ai.config;

import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Provides access to environment variables.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
public class EnvironmentVariables {
    private final Optional<String> azureApiKey;
    private final Optional<String> openaiApiKey;
    private final Optional<String> qdrantApiKey;
    private final Optional<String> minioAccessKey;
    private final Optional<String> minioSecretKey;

    @Inject
    EnvironmentVariables(
            @ConfigProperty(name = "AZURE_API_KEY") Optional<String> azureApiKey,
            @ConfigProperty(name = "OPENAI_API_KEY") Optional<String> openaiApiKey,
            @ConfigProperty(name = "QDRANT_API_KEY") Optional<String> qdrantApiKey,
            @ConfigProperty(name = "MINIO_ACCESS_KEY") Optional<String> minioAccessKey,
            @ConfigProperty(name = "MINIO_SECRET_KEY") Optional<String> minioSecretKey) {
        this.azureApiKey = azureApiKey;
        this.openaiApiKey = openaiApiKey;
        this.qdrantApiKey = qdrantApiKey;
        this.minioAccessKey = minioAccessKey;
        this.minioSecretKey = minioSecretKey;
    }

    /**
     * Returns the Azure API key, that is required for the Azure models.
     *
     * @return Azure API key
     */
    public String getAzureApiKey() {
        if (azureApiKey.isPresent()) {
            return azureApiKey.get();
        }
        throw new MissingEnvironmentVariableException("Azure API key is required but missing.");
    }

    /**
     * Returns the OpenAI API key, that is required for the OpenAI models.
     *
     * @return OpenAI API key
     */
    public String getOpenaiApiKey() {
        if (openaiApiKey.isPresent()) {
            return openaiApiKey.get();
        }
        throw new MissingEnvironmentVariableException("OpenAI API key is required but missing.");
    }

    /**
     * Returns the optional Qdrant API key..
     *
     * @return Qdrant API key
     */
    public Optional<String> getQdrantApiKey() {
        return qdrantApiKey;
    }

    /**
     * Returns the MinIO access key.
     *
     * @return MinIO access key
     */
    public String getMinioAccessKey() {
        if (minioAccessKey.isPresent()) {
            return minioAccessKey.get();
        }
        throw new MissingEnvironmentVariableException("MinIO access key is required but missing.");
    }

    /**
     * Returns the MinIO secret key.
     *
     * @return MinIO secret key
     */
    public String getMinioSecretKey() {
        if (minioSecretKey.isPresent()) {
            return minioSecretKey.get();
        }
        throw new MissingEnvironmentVariableException("MinIO secret key is required but missing.");
    }
}
