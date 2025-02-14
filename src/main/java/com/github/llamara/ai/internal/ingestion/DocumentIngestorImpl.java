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
package com.github.llamara.ai.internal.ingestion;

import com.github.llamara.ai.internal.MetadataKeys;
import com.github.llamara.ai.internal.ingestion.transformer.document.DocumentTransformerPipeline;
import com.github.llamara.ai.internal.ingestion.transformer.textsegment.TextSegmentTransformerPipeline;
import com.github.llamara.ai.internal.knowledge.KnowledgeManager;

import java.time.Instant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Implementation of the {@link DocumentIngestor}.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class DocumentIngestorImpl implements DocumentIngestor {
    private final KnowledgeManager knowledgeManager;
    private final EmbeddingStoreIngestor ingestor;

    @Inject
    DocumentIngestorImpl(
            KnowledgeManager knowledgeManager,
            DocumentTransformerPipeline documentTransformer,
            DocumentSplitter documentSplitter,
            TextSegmentTransformerPipeline textSegmentTransformer,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        this.knowledgeManager = knowledgeManager;

        this.ingestor =
                EmbeddingStoreIngestor.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .documentTransformer(documentTransformer)
                        .documentSplitter(documentSplitter)
                        .textSegmentTransformer(textSegmentTransformer)
                        .build();
    }

    @Override
    public void ingestDocument(Document document) {
        // Set metadata that is independent of the document's source
        document.metadata().put(MetadataKeys.INGESTED_AT, Instant.now().toString());

        String knowledgeId = document.metadata().getString(MetadataKeys.KNOWLEDGE_ID);
        Uni.createFrom()
                // Specify blocking operation
                .item(
                        () -> {
                            Log.infof("Ingesting document '%s' ...", knowledgeId);
                            return ingestor.ingest(document);
                        })
                // Specify to run blocking operation on worker pool
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                // Specify handling of result
                .onItem()
                .invoke(
                        result -> {
                            Integer tokenCount =
                                    result.tokenUsage() != null
                                            ? result.tokenUsage().inputTokenCount()
                                            : null;
                            if (tokenCount != null) {
                                Log.infof(
                                        "Successfully ingested document '%s' using %d tokens.",
                                        knowledgeId, result.tokenUsage().inputTokenCount());
                            } else {
                                Log.infof("Successfully ingested document '%s'.", knowledgeId);
                            }
                            knowledgeManager.setKnowledgeIngestionMetadata(
                                    document.metadata().getUUID(MetadataKeys.KNOWLEDGE_ID),
                                    IngestionStatus.SUCCEEDED,
                                    tokenCount);
                        })
                // Specify handling of failure
                .onFailure()
                .invoke(
                        throwable -> {
                            Log.errorf("Failed to ingest document '%s'.", knowledgeId, throwable);
                            knowledgeManager.setKnowledgeIngestionMetadata(
                                    document.metadata().getUUID(MetadataKeys.KNOWLEDGE_ID),
                                    IngestionStatus.FAILED,
                                    null);
                        })
                // Subscribe to the Uni, i.e. start the operation
                .subscribe()
                .with(item -> {}, failure -> {});
    }
}
