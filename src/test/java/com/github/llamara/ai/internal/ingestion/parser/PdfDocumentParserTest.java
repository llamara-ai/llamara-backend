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
package com.github.llamara.ai.internal.ingestion.parser;

import static com.github.llamara.ai.internal.ingestion.DocumentIngestionTestConstants.BLANK_PDF;
import static com.github.llamara.ai.internal.ingestion.DocumentIngestionTestConstants.TEST_PDF;
import static com.github.llamara.ai.internal.ingestion.DocumentIngestionTestConstants.TEST_PDF_CONTENT;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Tests for {@link PdfDocumentParser}. */
@QuarkusTest
class PdfDocumentParserTest {
    @Test
    void throwsBlankDocumentExceptionIfPdfIsBlank() {
        // given
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(BLANK_PDF);
        assertNotNull(inputStream);
        DocumentParser parser = new PdfDocumentParser();

        // then
        assertThrows(BlankDocumentException.class, () -> parser.parse(inputStream));
    }

    @Test
    void parsesTestPdf() {
        // given
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TEST_PDF);
        assertNotNull(inputStream);
        PdfDocumentParser parser = new PdfDocumentParser();

        // when
        Document document = assertDoesNotThrow(() -> parser.parse(inputStream));

        // then
        assertEquals(TEST_PDF_CONTENT, document.text());
    }
}
