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
package com.github.llamara.ai.internal.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

import static com.github.llamara.ai.internal.internal.Utils.AZURE_OPENAI_API_VERSION;
import static com.github.llamara.ai.internal.internal.Utils.buildAzureOpenaiEndpoint;

import com.github.llamara.ai.internal.config.EnvironmentVariables;
import com.github.llamara.ai.internal.config.embedding.EmbeddingModelConfig;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import io.quarkiverse.langchain4j.azure.openai.AzureOpenAiEmbeddingModel;
import io.quarkiverse.langchain4j.ollama.OllamaEmbeddingModel;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;

/**
 * CDI Bean Producer for {@link dev.langchain4j.model.embedding.EmbeddingModel}. It produces the
 * bean based on the {@link EmbeddingModelConfig}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Startup // initialize at startup to validate config
@ApplicationScoped
public class EmbeddingModelProducer {
    private final EmbeddingModelConfig config;
    private final EnvironmentVariables env;

    @Inject
    EmbeddingModelProducer(EmbeddingModelConfig config, EnvironmentVariables env) {
        this.config = config;
        this.env = env;

        produceEmbeddingModel();
    }

    @Produces
    @ApplicationScoped
    public EmbeddingModel produceEmbeddingModel() {
        Log.infof(
                "Creating embedding model '%s' of provider '%s' ...",
                config.model(), config.provider());

        return switch (config.provider()) {
            case OPENAI -> OpenAiEmbeddingModel.builder()
                    .baseUrl(config.baseUrl().orElse(null))
                    .apiKey(env.getOpenaiApiKey())
                    .modelName(config.model())
                    .build();
            case OLLAMA -> {
                if (config.baseUrl().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Base URL is required for Ollama embedding model.");
                }
                yield OllamaEmbeddingModel.builder()
                        .baseUrl(config.baseUrl().get()) // NOSONAR: we have checked for empty
                        .model(config.model())
                        .build();
            }
            case AZURE -> AzureOpenAiEmbeddingModel.builder()
                    .endpoint(buildAzureOpenaiEndpoint(config))
                    .apiKey(env.getAzureApiKey())
                    .apiVersion(AZURE_OPENAI_API_VERSION)
                    .maxRetries(3)
                    .build();
        };
    }
}
