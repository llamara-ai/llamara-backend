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
    private final Optional<String> googleGeminiApiKey;
    private final Optional<String> minioAccessKey;
    private final Optional<String> minioSecretKey;
    private final Optional<String> mistralApiKey;
    private final Optional<String> openaiApiKey;
    private final Optional<String> qdrantApiKey;

    @Inject
    EnvironmentVariables(
            @ConfigProperty(name = "AZURE_API_KEY") Optional<String> azureApiKey,
            @ConfigProperty(name = "GOOGLE_GEMINI_API_KEY") Optional<String> googleGeminiApiKey,
            @ConfigProperty(name = "MINIO_ACCESS_KEY") Optional<String> minioAccessKey,
            @ConfigProperty(name = "MINIO_SECRET_KEY") Optional<String> minioSecretKey,
            @ConfigProperty(name = "MISTRAL_API_KEY") Optional<String> mistralApiKey,
            @ConfigProperty(name = "OPENAI_API_KEY") Optional<String> openaiApiKey,
            @ConfigProperty(name = "QDRANT_API_KEY") Optional<String> qdrantApiKey) {
        this.azureApiKey = azureApiKey;
        this.googleGeminiApiKey = googleGeminiApiKey;
        this.minioAccessKey = minioAccessKey;
        this.minioSecretKey = minioSecretKey;
        this.mistralApiKey = mistralApiKey;
        this.openaiApiKey = openaiApiKey;
        this.qdrantApiKey = qdrantApiKey;
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
     * Returns the Google Gemini API key, that is required for the Google Gemini models.
     *
     * @return Google Gemini API key
     */
    public String getGoogleGeminiApiKey() {
        if (googleGeminiApiKey.isPresent()) {
            return googleGeminiApiKey.get();
        }
        throw new MissingEnvironmentVariableException(
                "Google Gemini API key is required but missing.");
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

    /**
     * Returns the Mistral API key, that is required for the Mistral models.
     *
     * @return Mistral API key
     */
    public String getMistralApiKey() {
        if (mistralApiKey.isPresent()) {
            return mistralApiKey.get();
        }
        throw new MissingEnvironmentVariableException("Mistral API key is required but missing.");
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
}
