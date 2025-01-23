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
package com.github.llamara.ai.internal.retrieval;

import com.github.llamara.ai.internal.MetadataKeys;
import com.github.llamara.ai.internal.security.PermissionMetadataMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * Implementation of the {@link RetrievalAugmentor} using the {@link EmbeddingStore} and {@link
 * EmbeddingModel} CDI beans. It augments the user message with the knowledge from the {@link
 * EmbeddingStore}. See <a
 * href="https://docs.langchain4j.dev/tutorials/rag/#retrieval-augmentor">LangChain4j Docs: RAG:
 * Retrieval Augmentor</a>.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class RetrievalAugmentorImpl implements RetrievalAugmentor {
    private final RetrievalAugmentor delegate;

    @Inject
    RetrievalAugmentorImpl(
            EmbeddingStore<TextSegment> store,
            EmbeddingModel model,
            ContentInjector contentInjector,
            SecurityIdentity identity) {
        // see https://docs.langchain4j.dev/tutorials/rag/#query-transformer
        // We may use a custom query transformer here to improve the quality of the response by
        // modifying or expanding the original query
        // see https://docs.langchain4j.dev/tutorials/rag/#content-retriever
        EmbeddingStoreContentRetriever contentRetriever =
                EmbeddingStoreContentRetriever.builder()
                        .embeddingModel(model)
                        .embeddingStore(store)
                        // Possible improvement: Return more results and use reranking
                        .maxResults(3)
                        // Filter the results to only include knowledge the user has access to
                        // Note: We cannot implement our own filter because the embedding store also
                        // needs to support it
                        // TODO: Enable the following filter as soon as contains filter is available
                        // .filter(metadataKey(MetadataKeys.PERMISSION).contains(PermissionMetadataMapper.usernameToMetadataQuery(identity)))
                        // problem: admins currently cannot use all knowledge in retrieval step
                        .filter(
                                metadataKey(MetadataKeys.PERMISSION)
                                        .isEqualTo(
                                                PermissionMetadataMapper.identityToMetadataQuery(
                                                        identity)))
                        .build();
        this.delegate =
                DefaultRetrievalAugmentor.builder()
                        .contentRetriever(contentRetriever)
                        .contentInjector(contentInjector)
                        .build();
    }

    @Override
    public AugmentationResult augment(AugmentationRequest augmentationRequest) {
        return delegate.augment(augmentationRequest);
    }

    @Override
    public UserMessage augment(UserMessage userMessage, Metadata metadata) {
        return delegate
                .augment( // NOSONAR: we need to use this method for implementation of interface
                        userMessage, metadata);
    }
}
