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
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Shared utilities and constants.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class Utils {

    /** The API version to use for Quarkus LangChain4j Azure OpenAI. */
    public static final String AZURE_OPENAI_API_VERSION = "2023-05-15";

    private Utils() {}

    /**
     * Generates a MD5 checksum for the given file.
     *
     * @param file the file to generate the checksum for
     * @return the MD5 checksum
     * @throws IOException if an I/O error occurs reading the file
     */
    public static String generateChecksum(Path file) throws IOException {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException( // NOSONAR: MD5 is a standard algorithm, this should never
                    // be thrown
                    e);
        }
        byte[] data = Files.readAllBytes(file);
        md5.update(data);
        return String.format("%032x", new BigInteger(1, md5.digest()));
    }

    /**
     * Builds the Azure OpenAI endpoint in the format <code>
     * https://{resource-name}.openai.azure.com/openai/deployments/{deployment-name}</code>.
     *
     * @param config the config of the chat model
     * @return the Azure OpenAI endpoint for Quarkus LangChain4j Azure
     * @throws IllegalArgumentException if the endpoint is empty
     */
    public static String buildAzureOpenaiEndpoint(ChatModelConfig.ModelConfig config)
            throws IllegalArgumentException {
        if (config.resourceName().isEmpty()) {
            throw new IllegalArgumentException("Endpoint is required for Azure chat model.");
        }
        return "https://"
                + config.resourceName().get() // NOSONAR: We have checked for empty
                + ".openai.azure.com/openai/deployments/"
                + config.model();
    }

    /**
     * Builds the Azure OpenAI endpoint in the format <code>
     * https://{resource-name}.openai.azure.com/openai/deployments/{deployment-name}</code>.
     *
     * @param config the config of the chat model
     * @return the Azure OpenAI endpoint for Quarkus LangChain4j Azure
     * @throws IllegalArgumentException if the endpoint is empty
     */
    public static String buildAzureOpenaiEndpoint(EmbeddingModelConfig config)
            throws IllegalArgumentException {
        if (config.resourceName().isEmpty()) {
            throw new IllegalArgumentException("Endpoint is required for Azure embedding model.");
        }
        return "https://"
                + config.resourceName().get() // NOSONAR: We have checked for empty
                + ".openai.azure.com/openai/deployments/"
                + config.model();
    }
}
