/*
 * #%L
 * llamara-backend
 * %%
 * Copyright (C) 2024 Contributors to the LLAMARA project
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

import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ValueFactory.value;

import com.github.llamara.ai.internal.config.EnvironmentVariables;
import com.github.llamara.ai.internal.config.embedding.EmbeddingStoreConfig;
import com.github.llamara.ai.internal.internal.MetadataKeys;
import com.github.llamara.ai.internal.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.internal.security.PermissionMetadataMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Points;

/**
 * Implementation of {@link EmbeddingStorePermissionMetadataManager} for Qdrant.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(QdrantEmbeddingStorePermissionMetadataManagerImpl.class)
@ApplicationScoped
class QdrantEmbeddingStorePermissionMetadataManagerImpl
        implements EmbeddingStorePermissionMetadataManager {
    private final QdrantClient client;
    private final String collectionName;

    @Inject
    QdrantEmbeddingStorePermissionMetadataManagerImpl(
            EmbeddingStoreConfig config, EnvironmentVariables env) {
        QdrantGrpcClient.Builder grpcClientBuilder =
                QdrantGrpcClient.newBuilder(config.host(), config.port(), config.tls());

        String apiKey = env.getQdrantApiKey().orElse(null);
        if (apiKey != null) {
            grpcClientBuilder.withApiKey(apiKey);
        }

        this.client = new QdrantClient(grpcClientBuilder.build());
        this.collectionName = config.collectionName();
    }

    @Override
    public void updatePermissionMetadata(Knowledge knowledge) {
        Points.Filter filter =
                Points.Filter.newBuilder()
                        .addMust(
                                matchKeyword(
                                        MetadataKeys.KNOWLEDGE_ID, knowledge.getId().toString()))
                        .build();
        client.setPayloadAsync(
                collectionName,
                Map.of(
                        MetadataKeys.PERMISSION,
                        value(
                                PermissionMetadataMapper.permissionsToMetadataEntry(
                                        knowledge.getPermissions()))),
                filter,
                null,
                null,
                null);
    }
}
