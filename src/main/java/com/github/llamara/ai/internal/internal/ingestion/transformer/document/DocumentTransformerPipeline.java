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
package com.github.llamara.ai.internal.internal.ingestion.transformer.document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentTransformer;

/**
 * A {@link DocumentTransformer} that provides a plug-able pipeline for transforming {@link
 * Document}s.
 *
 * <p>Use this class to combine multiple {@link DocumentTransformer}s. See <a
 * href="https://docs.langchain4j.dev/tutorials/rag/#document-transformer">LangChain4j: RAG:
 * Document Transformer</a>.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
public class DocumentTransformerPipeline implements DocumentTransformer {
    private final CleaningByRegexDocumentTransformer cleaningByRegexDocumentTransformer;

    @Inject
    DocumentTransformerPipeline(
            CleaningByRegexDocumentTransformer cleaningByRegexDocumentTransformer) {
        this.cleaningByRegexDocumentTransformer = cleaningByRegexDocumentTransformer;
    }

    @Override
    public Document transform(Document document) {
        return cleaningByRegexDocumentTransformer.transform(document);
    }
}
