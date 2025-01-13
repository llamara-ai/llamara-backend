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
package com.github.llamara.ai.internal.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

import com.github.llamara.ai.internal.config.EnvironmentVariables;
import com.github.llamara.ai.internal.config.embedding.EmbeddingStoreConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.quarkus.logging.Log;

/**
 * CDI Bean Producer for {@link EmbeddingStore}. It produces the bean based on the {@link
 * EmbeddingStoreConfig}.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class EmbeddingStoreProducer {
    private final EmbeddingStoreConfig config;
    private final EnvironmentVariables env;

    @Inject
    EmbeddingStoreProducer(EmbeddingStoreConfig config, EnvironmentVariables env) {
        this.config = config;
        this.env = env;
    }

    @Produces
    @ApplicationScoped
    EmbeddingStore<TextSegment> produceEmbeddingStore() {
        Log.infof("Creating embedding store of type '%s' ...", config.type());

        return switch (config.type()) {
            case QDRANT -> produceQdrantEmbeddingStore();
        };
    }

    private QdrantEmbeddingStore produceQdrantEmbeddingStore() {
        return new QdrantEmbeddingStore(
                config.collectionName(),
                config.host(),
                config.port(),
                config.tls(),
                "text_segment",
                env.getQdrantApiKey().orElse(null));
    }
}
