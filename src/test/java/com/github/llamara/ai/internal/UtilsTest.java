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
import com.github.llamara.ai.config.embedding.EmbeddingModelConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for {@link Utils}. */
@QuarkusTest
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
        // given
        ChatModelConfig.ModelConfig modelConfig = mock(ChatModelConfig.ModelConfig.class);
        when(modelConfig.provider()).thenReturn(ChatModelConfig.ChatModelProvider.AZURE);
        when(modelConfig.resourceName()).thenReturn(Optional.of("azure-resource-name"));
        when(modelConfig.model()).thenReturn("azure-model");

        // when
        String endpoint = Utils.buildAzureOpenaiEndpoint(modelConfig);

        // then
        assertEquals(
                "https://azure-resource-name.openai.azure.com/openai/deployments/azure-model",
                endpoint);
    }

    @Test
    void buildAzureOpenAiEndpointThrowsIfResourceNameMissingForChatModelConfig() {
        // given
        ChatModelConfig.ModelConfig modelConfig = mock(ChatModelConfig.ModelConfig.class);

        // then
        assertThrows(
                IllegalArgumentException.class, () -> Utils.buildAzureOpenaiEndpoint(modelConfig));
    }

    @Test
    void buildAzureOpenAiEndpointBuildsEndpointForEmbeddingModelConfig() {
        // given
        EmbeddingModelConfig modelConfig = mock(EmbeddingModelConfig.class);
        when(modelConfig.provider()).thenReturn(EmbeddingModelConfig.EmbeddingModelProvider.AZURE);
        when(modelConfig.resourceName()).thenReturn(Optional.of("azure-resource-name"));
        when(modelConfig.model()).thenReturn("azure-model");

        // when
        String endpoint = Utils.buildAzureOpenaiEndpoint(modelConfig);

        // then
        assertEquals(
                "https://azure-resource-name.openai.azure.com/openai/deployments/azure-model",
                endpoint);
    }

    @Test
    void buildAzureOpenAiEndpointThrowsIfResourceNameMissingForEmbeddingModelConfig() {
        // given
        EmbeddingModelConfig modelConfig = mock(EmbeddingModelConfig.class);

        // then
        assertThrows(
                IllegalArgumentException.class, () -> Utils.buildAzureOpenaiEndpoint(modelConfig));
    }

    @ParameterizedTest
    @CsvSource({
        "AIX",
        "HP-UX",
        "Irix",
        "Linux",
        "Mac OS X",
        "Solaris",
        "SunOS",
        "FreeBSD",
        "OpenBSD",
        "NetBSD"
    })
    void isOsUnixReturnsTrueIfOnUnix(String osName) {
        assertTrue(Utils.isOsUnix(osName));
    }

    @ParameterizedTest
    @CsvSource({"Windows 10", "Windows 11", "OS/2", "OS/400", "z/OS"})
    void isOsUnixReturnsFalseIfNotOnUnix(String osName) {
        assertFalse(Utils.isOsUnix(osName));
    }
}
