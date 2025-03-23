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

import java.io.IOException;
import java.io.InputStream;

import static dev.langchain4j.internal.Utils.isNullOrBlank;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Parses a PDF file into a {@link Document} using Apache PDFBox library. It adds the {@link
 * PdfDocumentParser#PAGE_TAG} to the beginning of each page's text.
 *
 * @author Florian Hotze - Initial contribution
 */
public class PdfDocumentParser implements DocumentParser {
    public static final String PAGE_TAG = "<begin-page/>";

    @Override
    public Document parse(InputStream inputStream) {
        try (PDDocument pdfDocument = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {
            int numberOfPages = pdfDocument.getNumberOfPages();

            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= numberOfPages; page++) {
                PDFTextStripper reader = new PDFTextStripper();
                reader.setStartPage(page);
                reader.setEndPage(page);
                String pageText = reader.getText(pdfDocument);
                sb.append(PAGE_TAG).append(System.lineSeparator()).append(pageText);
            }

            String text = sb.toString();

            if (isNullOrBlank(text.replaceAll(PAGE_TAG + System.lineSeparator(), ""))) {
                throw new BlankDocumentException();
            }
            return Document.from(text);
        } catch (IOException e) {
            throw new RuntimeException(
                    e); // NOSONAR: we don't expect an IOException here, so rethrow it as
            // RuntimeException
        }
    }
}
