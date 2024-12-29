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
package com.github.llamara.ai.internal.internal.ingestion.transformer.textsegment;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;

/**
 * A {@link TextSegmentTransformer} that provides a plug-able pipeline for transforming {@link
 * TextSegment}s.
 *
 * <p>Use this class to combine multiple {@link TextSegmentTransformer}s. See <a
 * href="https://docs.langchain4j.dev/tutorials/rag/#text-segment-transformer">LangChain4j: RAG:
 * Text Segment Transformer</a>.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
public class TextSegmentTransformerPipeline implements TextSegmentTransformer {
    @Override
    public TextSegment transform(TextSegment segment) {
        return segment;
    }
}
