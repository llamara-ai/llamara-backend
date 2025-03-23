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

import static com.github.llamara.ai.internal.ingestion.DocumentIngestionTestConstants.TEST_PDF_CONTENT;

import com.github.llamara.ai.internal.EmbeddingMetadataKeys;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Tests for {@link PdfDocumentSplitter}. */
@QuarkusTest
class PdfDocumentSplitterTest {
    private static final DocumentSplitter SUB_SPLITTER =
            DocumentSplitters.recursive(Integer.MAX_VALUE, 0);

    @Test
    void splitsTestContent() {
        // given
        DocumentSplitter splitter = new PdfDocumentSplitter(SUB_SPLITTER);

        // when
        List<TextSegment> segments = splitter.split(Document.from(TEST_PDF_CONTENT));

        // then
        assertEquals(3, segments.size());
    }

    @Test
    void addsPageMetadataWhenSplittingTestContent() {
        // given
        DocumentSplitter splitter = new PdfDocumentSplitter(SUB_SPLITTER);

        // when
        List<TextSegment> segments = splitter.split(Document.from(TEST_PDF_CONTENT));

        // then
        assertEquals(3, segments.size());
        assertEquals(1, segments.get(0).metadata().getInteger(EmbeddingMetadataKeys.PAGE));
        assertEquals(2, segments.get(1).metadata().getInteger(EmbeddingMetadataKeys.PAGE));
        assertEquals(3, segments.get(2).metadata().getInteger(EmbeddingMetadataKeys.PAGE));
    }
}
