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
package com.github.llamara.ai.internal.internal.knowledge.embedding;

import com.github.llamara.ai.internal.internal.MetadataKeys;
import com.github.llamara.ai.internal.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.internal.security.PermissionMetadataMapper;

/**
 * Interface defining the API for managing {@link MetadataKeys#PERMISSION} metadata of embeddings.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface EmbeddingStorePermissionMetadataManager {
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
