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
package com.github.llamara.ai.internal;

import com.github.llamara.ai.config.chat.ChatModelConfig;
import com.github.llamara.ai.config.chat.ModelConfigImpl;
import com.github.llamara.ai.config.embedding.EmbeddingModelConfig;
import com.github.llamara.ai.config.embedding.EmbeddingModelConfigImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Tests for {@link Utils}. */
class UtilsTest {
    private static final Path FILE = Path.of("src/test/resources/llamara.txt");
    private static final String FILE_CHECKSUM;

    static {
        try {
            FILE_CHECKSUM = Utils.generateChecksum(FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateChecksumGeneratesChecksum() {
        String checksum = assertDoesNotThrow(() -> Utils.generateChecksum(FILE));
        assertEquals(FILE_CHECKSUM, checksum);
    }

    @Test
    void generateChecksumThrowsIOExceptionIfFileDoesNotExist() {
        Path nonExistentFile = Path.of("src/test/resources/non-existent.txt");
        assertThrows(IOException.class, () -> Utils.generateChecksum(nonExistentFile));
    }

    @Test
    void buildAzureOpenAiEndpointBuildsEndpointForChatModelConfig() {
        ChatModelConfig.ModelConfig modelConfig = new AzureChatModelConfig();
        String endpoint = Utils.buildAzureOpenaiEndpoint(modelConfig);
        assertEquals(
                "https://azure-resource-name.openai.azure.com/openai/deployments/azure-model",
                endpoint);
    }

    @Test
    void buildAzureOpenAiEndpointThrowsIfResourceNameMissingForChatModelConfig() {
        ChatModelConfig.ModelConfig modelConfig = new EmptyChatModelConfig();
        assertThrows(
                IllegalArgumentException.class, () -> Utils.buildAzureOpenaiEndpoint(modelConfig));
    }

    @Test
    void buildAzureOpenAiEndpointBuildsEndpointForEmbeddingModelConfig() {
        EmbeddingModelConfig modelConfig = new AzureEmbeddingModelConfig();
        String endpoint = Utils.buildAzureOpenaiEndpoint(modelConfig);
        assertEquals(
                "https://azure-resource-name.openai.azure.com/openai/deployments/azure-model",
                endpoint);
    }

    @Test
    void buildAzureOpenAiEndpointThrowsIfResourceNameMissingForEmbeddingModelConfig() {
        EmbeddingModelConfig modelConfig = new EmptyEmbeddingModelConfig();
        assertThrows(
                IllegalArgumentException.class, () -> Utils.buildAzureOpenaiEndpoint(modelConfig));
    }

    static class AzureChatModelConfig extends ModelConfigImpl {
        @Override
        public Optional<String> resourceName() {
            return Optional.of("azure-resource-name");
        }

        @Override
        public String model() {
            return "azure-model";
        }
    }

    static class EmptyChatModelConfig extends ModelConfigImpl {}

    static class AzureEmbeddingModelConfig extends EmbeddingModelConfigImpl {
        @Override
        public EmbeddingModelProvider provider() {
            return EmbeddingModelProvider.AZURE;
        }

        @Override
        public Optional<String> resourceName() {
            return Optional.of("azure-resource-name");
        }

        @Override
        public String model() {
            return "azure-model";
        }
    }

    static class EmptyEmbeddingModelConfig extends EmbeddingModelConfigImpl {}
}
