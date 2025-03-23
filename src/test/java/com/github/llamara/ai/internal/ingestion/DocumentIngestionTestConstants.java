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

import com.github.llamara.ai.internal.ingestion.parser.PdfDocumentParser;

public final class DocumentIngestionTestConstants {
    private DocumentIngestionTestConstants() {}

    public static final String BLANK_PDF = "blank.pdf";
    public static final String TEST_PDF = "test.pdf";
    public static final String TEST_PDF_CONTENT =
            String.format(
                    "%s%nThis is the text of page 1.%n%s%nThis is the text of page 2.%n%s%nThis is"
                            + " the text of page 3.%n",
                    PdfDocumentParser.PAGE_TAG,
                    PdfDocumentParser.PAGE_TAG,
                    PdfDocumentParser.PAGE_TAG);
}
