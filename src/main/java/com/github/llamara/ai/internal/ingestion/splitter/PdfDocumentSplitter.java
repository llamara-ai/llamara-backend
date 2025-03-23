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

import static com.github.llamara.ai.internal.CommonMetadataKeys.SEGMENTS;
import static com.github.llamara.ai.internal.EmbeddingMetadataKeys.INDEX;
import static com.github.llamara.ai.internal.EmbeddingMetadataKeys.PAGE;

import com.github.llamara.ai.internal.ingestion.parser.PdfDocumentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

/**
 * Splits a PDF document into its pages by the {@link PdfDocumentParser#PAGE_TAG}, then uses the
 * passed in {@link DocumentSplitter} to split the page.
 *
 * @author Florian Hotze - Initial contribution
 */
class PdfDocumentSplitter implements DocumentSplitter {
    private final DocumentSplitter subSplitter;

    PdfDocumentSplitter(DocumentSplitter subSplitter) {
        this.subSplitter = subSplitter;
    }

    @Override
    public List<TextSegment> split(Document document) {
        ensureNotNull(document, "document");

        List<TextSegment> segments = new ArrayList<>();

        String[] parts = document.text().split(PdfDocumentParser.PAGE_TAG);

        AtomicInteger index = new AtomicInteger(0);

        for (int page = 1; page < parts.length; page++) {
            String text = parts[page];
            if (text == null || text.isBlank()) continue;

            for (TextSegment segment : subSplitter.split(Document.from(text))) {
                segments.add(
                        createSegment(segment.text(), document, page, index.getAndIncrement()));
            }
        }

        document.metadata().put(SEGMENTS, index.get());

        return segments;
    }

    /**
     * Creates a new {@link TextSegment} from the provided text and document.
     *
     * <p>The segment inherits all metadata from the document. The segment also includes a "page"
     * metadata key representing the page number and an "index" metadata key representing the
     * segment position within the document.
     *
     * @param text text of the segment
     * @param document document to which the segment belongs
     * @param page page of the segment within the document
     * @param index index of the segment within the document
     */
    static TextSegment createSegment(String text, Document document, int page, int index) {
        Metadata metadata =
                document.metadata().copy().put(PAGE, page).put(INDEX, String.valueOf(index));
        return TextSegment.from(text, metadata);
    }
}
