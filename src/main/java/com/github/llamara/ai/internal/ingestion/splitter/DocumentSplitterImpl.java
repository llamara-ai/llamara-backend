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
package com.github.llamara.ai.internal.ingestion.splitter;

import com.github.llamara.ai.config.ingestion.DocumentSplitterConfig;
import com.github.llamara.ai.internal.CommonMetadataKeys;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

/**
 * Implementation of the {@link DocumentSplitter} using the configured {@link DocumentSplitter} from
 * the {@link DocumentSplitterConfig}. If the {@link Document} is a PDF file, use the {@link
 * PdfDocumentSplitter} to split it by pages and add page metadata, then use the configured {@link
 * DocumentSplitter} to split the page content. <br>
 * See <a href="https://docs.langchain4j.dev/tutorials/rag#document-splitter">LangChain4j Docs: RAG:
 * Document Splitter</a>.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class DocumentSplitterImpl implements DocumentSplitter {
    private final PdfDocumentSplitter pdfSplitter;
    private final DocumentSplitter configuredSplitter;

    @Inject
    DocumentSplitterImpl(DocumentSplitterConfig config) {
        this.configuredSplitter =
                switch (config.type()) {
                    case LINE ->
                            new DocumentByLineSplitter(
                                    config.maxSegmentSize(), config.maxOverlapSize());
                    case PARAGRAPH ->
                            new DocumentByParagraphSplitter(
                                    config.maxSegmentSize(), config.maxOverlapSize());
                    case RECURSIVE ->
                            DocumentSplitters.recursive(
                                    config.maxSegmentSize(), config.maxOverlapSize());
                };
        this.pdfSplitter = new PdfDocumentSplitter(configuredSplitter);
    }

    @Override
    public List<TextSegment> split(Document document) {
        if ("application/pdf"
                .equals(document.metadata().getString(CommonMetadataKeys.CONTENT_TYPE))) {
            return pdfSplitter.split(document);
        }

        return configuredSplitter.split(document);
    }
}
