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
package com.github.llamara.ai.internal.internal;

import com.github.llamara.ai.internal.internal.knowledge.Knowledge;

/**
 * Constant class for metadata keys.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class MetadataKeys {
    private MetadataKeys() {}

    /**
     * The {@link java.util.UUID} as string of the {@link Knowledge}.
     *
     * <p>Only for embeddings.
     */
    public static final String KNOWLEDGE_ID = "knowledge_id";

    /** The checksum of the knowledge source. */
    public static final String CHECKSUM = "checksum";

    /**
     * The ingestion timestamp of the document as {@link java.time.Instant} as string.
     *
     * <p>Only for embeddings.
     */
    public static final String INGESTED_AT = "ingested_at";

    /** The content type of the document. */
    public static final String CONTENT_TYPE = "content_type";

    /**
     * The comma-separated list of usernames that have permission to access the knowledge.
     *
     * <p>Only for embeddings.
     */
    public static final String PERMISSION = "permission";
}
