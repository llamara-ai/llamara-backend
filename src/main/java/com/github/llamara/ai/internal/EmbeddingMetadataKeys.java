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

import com.github.llamara.ai.internal.knowledge.persistence.Knowledge;

/**
 * Constant class for {@link dev.langchain4j.data.embedding.Embedding} metadata keys.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class EmbeddingMetadataKeys {
    private EmbeddingMetadataKeys() {}

    /** The {@link java.util.UUID} as string of the {@link Knowledge}. */
    public static final String KNOWLEDGE_ID = "knowledge_id";

    /** The ingestion timestamp of the document as {@link java.time.Instant} as string. */
    public static final String INGESTED_AT = "ingested_at";

    /** The comma-separated list of usernames that have permission to access the knowledge. */
    public static final String PERMISSION = "permission";

    /**
     * The page number of the {@link dev.langchain4j.data.segment.TextSegment} within the document.
     */
    public static final String PAGE = "page";

    /**
     * The index representing the {@link dev.langchain4j.data.segment.TextSegment} position within
     * the document.
     */
    public static final String INDEX = "index";
}
