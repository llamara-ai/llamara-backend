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

import com.github.llamara.ai.config.ingestion.DocumentSplitterConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;

/**
 * CDI Bean Producer for {@link DocumentSplitter}. It produces the bean based on the {@link
 * DocumentSplitterConfig}. See <a
 * href="https://docs.langchain4j.dev/tutorials/rag/#document-splitter">LangChain4j: RAG: Document
 * Splitter</a>.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class DocumentSplitterProducer {
    DocumentSplitterConfig config;

    @Inject
    DocumentSplitterProducer(DocumentSplitterConfig config) {
        this.config = config;
    }

    @Produces
    @ApplicationScoped
    public DocumentSplitter produceDocumentSplitter() {
        return switch (config.type()) {
            case LINE -> new DocumentByLineSplitter(
                    config.maxSegmentSize(), config.maxOverlapSize());
            case PARAGRAPH -> new DocumentByParagraphSplitter(
                    config.maxSegmentSize(), config.maxOverlapSize());
            case RECURSIVE -> DocumentSplitters.recursive(
                    config.maxSegmentSize(), config.maxOverlapSize());
        };
    }
}
