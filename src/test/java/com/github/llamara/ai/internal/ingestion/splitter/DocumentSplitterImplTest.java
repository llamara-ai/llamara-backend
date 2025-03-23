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

import com.github.llamara.ai.config.ingestion.DocumentSplitterConfig;
import com.github.llamara.ai.internal.CommonMetadataKeys;
import com.github.llamara.ai.internal.EmbeddingMetadataKeys;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link DocumentSplitterImpl}. */
@QuarkusTest
class DocumentSplitterImplTest {
    private final DocumentSplitterConfig config = mock(DocumentSplitterConfig.class);

    DocumentSplitter splitter;

    @BeforeEach
    void setup() {
        when(config.type()).thenReturn(DocumentSplitterConfig.DocumentSplitterType.RECURSIVE);
        when(config.maxSegmentSize()).thenReturn(Integer.MAX_VALUE);
        when(config.maxOverlapSize()).thenReturn(0);

        splitter = new DocumentSplitterImpl(config);
    }

    @Test
    void usesPdfDocumentSplitterForPdfDocuments() {
        // given
        Document document =
                Document.from(
                        TEST_PDF_CONTENT,
                        Metadata.from(Map.of(CommonMetadataKeys.CONTENT_TYPE, "application/pdf")));

        // when
        List<TextSegment> segments = splitter.split(document);

        // then
        assertEquals(3, segments.size());
        assertEquals(1, segments.get(0).metadata().getInteger(EmbeddingMetadataKeys.PAGE));
    }

    @Test
    void usesConfiguredDocumentSplitterForNonPdfDocuments() {
        // given
        Document document =
                Document.from(
                        "This is a test document",
                        Metadata.metadata(CommonMetadataKeys.CONTENT_TYPE, "text/plain"));

        // when
        List<TextSegment> segments = splitter.split(document);

        // then
        assertNull(segments.get(0).metadata().getInteger(EmbeddingMetadataKeys.PAGE));
    }
}
