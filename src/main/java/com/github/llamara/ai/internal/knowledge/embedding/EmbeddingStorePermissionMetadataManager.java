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
package com.github.llamara.ai.internal.knowledge.embedding;

import com.github.llamara.ai.internal.MetadataKeys;
import com.github.llamara.ai.internal.StartupException;
import com.github.llamara.ai.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.security.PermissionMetadataMapper;

/**
 * Interface defining the API for managing {@link MetadataKeys#PERMISSION} metadata of embeddings.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface EmbeddingStorePermissionMetadataManager {
    /**
     * Check the connection to the configured {@link dev.langchain4j.store.embedding.EmbeddingStore}
     * and initialize it if required. Throw a {@link StartupException} to abort application startup
     * if no connection can be established or initialization fails.
     *
     * <p>MUST be called during application startup for the configured {@link
     * dev.langchain4j.store.embedding.EmbeddingStore}.
     *
     * @throws StartupException if failed to connect or failed to initialize
     */
    void checkConnectionAndInit() throws StartupException;

    /**
     * Update the {@link MetadataKeys#PERMISSION} metadata of the embeddings of the given knowledge.
     *
     * <p>Implementations have to use {@link PermissionMetadataMapper#permissionsToMetadataEntry} to
     * convert the permissions to a metadata entry.
     *
     * @param knowledge
     */
    void updatePermissionMetadata(Knowledge knowledge);
}
