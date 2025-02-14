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

import com.github.llamara.ai.config.EnvironmentVariables;
import com.github.llamara.ai.config.embedding.EmbeddingStoreConfig;
import com.github.llamara.ai.internal.MetadataKeys;
import com.github.llamara.ai.internal.StartupException;
import com.github.llamara.ai.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.security.PermissionMetadataMapper;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ValueFactory.value;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import io.quarkus.logging.Log;

/**
 * Implementation of {@link EmbeddingStorePermissionMetadataManager} for Qdrant.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(QdrantEmbeddingStorePermissionMetadataManagerImpl.class)
@ApplicationScoped
class QdrantEmbeddingStorePermissionMetadataManagerImpl
        implements EmbeddingStorePermissionMetadataManager {
    private final EmbeddingStoreConfig config;
    private final QdrantClient client;
    private final String collectionName;

    @Inject
    QdrantEmbeddingStorePermissionMetadataManagerImpl(
            EmbeddingStoreConfig config, EnvironmentVariables env) {
        this.config = config;

        QdrantGrpcClient.Builder grpcClientBuilder =
                QdrantGrpcClient.newBuilder(config.host(), config.port(), config.tls());

        Optional<String> apiKey = env.getQdrantApiKey();
        apiKey.ifPresent(grpcClientBuilder::withApiKey);

        this.client = new QdrantClient(grpcClientBuilder.build());
        this.collectionName = config.collectionName();
    }

    @Override
    public void checkConnectionAndInit() {
        boolean collectionExists;
        try {
            collectionExists = client.collectionExistsAsync(collectionName).get();
        } catch (InterruptedException // NOSONAR
                | ExecutionException // NOSONAR
                        e) { // we don't want to re-interrupt or rethrow as we abort startup
            throw new StartupException("Cannot connect to Qdrant embedding store.", e);
        }
        if (!collectionExists) {
            try {
                Log.infof("Creating missing Qdrant collection '%s' ...", collectionName);
                client.createCollectionAsync(
                                collectionName,
                                Collections.VectorParams.newBuilder()
                                        .setSize(config.vectorSize())
                                        .setDistance(Collections.Distance.Cosine)
                                        .build())
                        .get();
                Log.infof(
                        "Enabling %s payload index for Qdrant collection '%s' ...",
                        MetadataKeys.KNOWLEDGE_ID, collectionName);
                client.createPayloadIndexAsync(
                                collectionName,
                                MetadataKeys.KNOWLEDGE_ID,
                                Collections.PayloadSchemaType.Uuid,
                                null,
                                null,
                                null,
                                null)
                        .get();
            } catch (InterruptedException // NOSONAR
                    | ExecutionException // NOSONAR
                            e) { // we don't want to re-interrupt or rethrow as we abort startup
                throw new StartupException(
                        String.format(
                                "Failed to create missing Qdrant collection '%s'.",
                                collectionName));
            }
        }
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
        Log.infof("Updated permission metadata for knowledge '%s'.", knowledge.getId());
    }
}
