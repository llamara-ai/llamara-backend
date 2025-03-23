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
package com.github.llamara.ai.internal.ingestion.transformer.document;

import com.github.llamara.ai.internal.EmbeddingMetadataKeys;

import java.util.regex.Pattern;
import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentTransformer;
import io.quarkus.logging.Log;

/**
 * A {@link DocumentTransformer} that cleans the text of a document by removing certain patterns.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class CleaningByRegexDocumentTransformer implements DocumentTransformer {
    private static final Pattern PAGE_NUMBER_PATTERN =
            Pattern.compile("^ *((Seite|page) )?\\d* (von|of) \\d+ *$", Pattern.MULTILINE);
    private static final Pattern TOC_ENTRY_PATTERN =
            Pattern.compile(
                    "^([\\d.]+)?[\\wäöüß/—–’'\"? \\-]+( ?\\.\\.+) ?\\d+ *$", Pattern.MULTILINE);
    private static final Pattern REFERENCE_PATTERN =
            Pattern.compile(
                    "(\\d+ )?((vgl)|(siehe auch)|(Online[ \n]+verfügbar[ \n]+unter)).+$",
                    Pattern.MULTILINE);
    private static final Pattern WEBLINK_WITH_OPT_DATE_PATTERN =
            Pattern.compile(
                    "(https?://)([A-z]+.)?([A-z]+.)([A-z]+)([A-z./\\-?=&%:#~,0-9]*)("
                            + " \\(\\d{2,4}[.-]\\d{2}[.-]\\d{2,4}\\).?)?",
                    Pattern.MULTILINE);
    private static final Pattern COPYRIGHT_PATTERN =
            Pattern.compile("^[Cc]opyright.*$", Pattern.MULTILINE);
    private static final Pattern EMPTY_LINE_PATTERN =
            Pattern.compile("^\\n( )*\\n( )*\\n", Pattern.MULTILINE);

    @Override
    public Document transform(Document document) {
        String text = document.text();
        int size = text.length();
        text = PAGE_NUMBER_PATTERN.matcher(text).replaceAll("");
        text = TOC_ENTRY_PATTERN.matcher(text).replaceAll("");
        text = REFERENCE_PATTERN.matcher(text).replaceAll("");
        text = WEBLINK_WITH_OPT_DATE_PATTERN.matcher(text).replaceAll("");
        text = COPYRIGHT_PATTERN.matcher(text).replaceAll("");
        // finally, reduce the number of empty lines
        Log.debugf(
                "Reduced text size of '%s' from %d to %d characters.",
                document.metadata().getString(EmbeddingMetadataKeys.KNOWLEDGE_ID),
                size,
                text.length());
        return Document.from(text, document.metadata());
    }
}
