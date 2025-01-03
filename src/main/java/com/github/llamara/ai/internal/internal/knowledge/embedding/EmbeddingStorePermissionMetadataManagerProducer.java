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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

import com.github.llamara.ai.internal.config.embedding.EmbeddingStoreConfig;
import io.quarkus.runtime.Startup;

/**
 * CDI Bean Producer for {@link EmbeddingStorePermissionMetadataManager}. It produces the bean based
 * on the {@link EmbeddingStoreConfig}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Startup // initialize at startup to check connection
@ApplicationScoped
class EmbeddingStorePermissionMetadataManagerProducer {
    private final EmbeddingStoreConfig config;
    private final QdrantEmbeddingStorePermissionMetadataManagerImpl
            qdrantEmbeddingStorePermissionMetadataManager;

    @Inject
    EmbeddingStorePermissionMetadataManagerProducer(
            EmbeddingStoreConfig config,
            QdrantEmbeddingStorePermissionMetadataManagerImpl
                    qdrantEmbeddingStorePermissionMetadataManager) {
        this.config = config;
        this.qdrantEmbeddingStorePermissionMetadataManager =
                qdrantEmbeddingStorePermissionMetadataManager;

        produceEmbeddingStorePermissionMetadataManager().checkConnectionAndInit();
    }

    @Produces
    @Default
    @ApplicationScoped
    EmbeddingStorePermissionMetadataManager produceEmbeddingStorePermissionMetadataManager() {
        return switch (config.type()) {
            case QDRANT -> qdrantEmbeddingStorePermissionMetadataManager;
        };
    }
}
