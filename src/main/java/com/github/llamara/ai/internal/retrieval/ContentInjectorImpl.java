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
package com.github.llamara.ai.internal.retrieval;

import com.github.llamara.ai.config.RetrievalConfig;
import com.github.llamara.ai.internal.EmbeddingMetadataKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static dev.langchain4j.internal.Utils.isNotNullOrBlank;
import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static java.util.stream.Collectors.joining;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.injector.ContentInjector;

/**
 * Implementation of the {@link ContentInjector}. It injects the content based on the prompt
 * template from the {@link RetrievalConfig}. See <a
 * href="https://docs.langchain4j.dev/tutorials/rag/#content-injector">LangChain4j Docs: RAG:
 * Content Injector</a>. <br>
 * <br>
 * Implementation is based on <a
 * href="https://github.com/langchain4j/langchain4j/blob/f1a41b23818edac7af7bf34a17af19a654f1d67b/langchain4j-core/src/main/java/dev/langchain4j/rag/content/injector/DefaultContentInjector.java">
 * <code>dev.langchain4j.rag.content.injector.DefaultContentInjector</code></a>.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class ContentInjectorImpl implements ContentInjector {
    private final PromptTemplate promptTemplate;
    private final PromptTemplate noContentsPromptTemplate;
    private final List<String> textSegmentMetadataToInclude;
    private final List<ContentMetadata> contentMetadataToInclude;

    @Inject
    ContentInjectorImpl(RetrievalConfig config) {
        this.promptTemplate = PromptTemplate.from(config.promptTemplate());
        this.noContentsPromptTemplate =
                PromptTemplate.from(config.missingKnowledgePromptTemplate());
        this.textSegmentMetadataToInclude = List.of(EmbeddingMetadataKeys.KNOWLEDGE_ID);
        this.contentMetadataToInclude = List.of(ContentMetadata.EMBEDDING_ID);
    }

    @Override
    public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
        if (!(chatMessage instanceof UserMessage userMessage)) {
            return chatMessage;
        }

        Prompt prompt = createPrompt(userMessage, contents);
        if (isNotNullOrBlank(userMessage.name())) {
            return prompt.toUserMessage(userMessage.name());
        }

        return prompt.toUserMessage();
    }

    /**
     * @deprecated use {@link #inject(List, ChatMessage)} instead.
     */
    @Override
    @Deprecated
    public UserMessage inject(List<Content> contents, UserMessage userMessage) {
        return (UserMessage) inject(contents, (ChatMessage) userMessage);
    }

    /**
     * Combines the original {@link UserMessage} and the retrieved {@link Content}s into the
     * resulting {@link UserMessage} based on the {@link PromptTemplate}s. <br>
     * <br>
     * It appends all given {@link Content}s to the end of the given {@link UserMessage} in their
     * order of iteration. It optionally includes a list of {@link TextSegment} and {@link Content}
     * metadata with each {@link Content} respective {@link Content#textSegment()}.
     *
     * @param userMessage
     * @param contents
     * @return
     */
    private Prompt createPrompt(UserMessage userMessage, List<Content> contents) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", userMessage.singleText());

        if (contents.isEmpty()) {
            return noContentsPromptTemplate.apply(variables);
        }

        variables.put("contents", format(contents));
        return promptTemplate.apply(variables);
    }

    /**
     * Formats the given list of {@link Content}s in their order of iteration.
     *
     * @param contents
     * @return
     */
    private String format(List<Content> contents) {
        return contents.stream().map(this::format).collect(joining("\n\n"));
    }

    /**
     * Formats the given {@link Content} and includes the specified {@link TextSegment} and {@link
     * ContentMetadata} keys.
     *
     * @param content
     * @return
     */
    private String format(Content content) {

        TextSegment segment = content.textSegment();

        if (isNullOrEmpty(textSegmentMetadataToInclude)
                && isNullOrEmpty(contentMetadataToInclude)) {
            return segment.text();
        }

        String segmentContent = segment.text();
        String segmentMetadata = format(segment.metadata());
        String contentMetadata = format(content.metadata());

        return format(segmentContent, segmentMetadata, contentMetadata);
    }

    /**
     * Formats the given metadata of a {@link TextSegment} and includes the specified {@link
     * Metadata} keys.
     *
     * @param metadata
     * @return
     */
    private String format(Metadata metadata) {
        StringBuilder formattedMetadata = new StringBuilder();
        for (String metadataKey : textSegmentMetadataToInclude) {
            String metadataValue = metadata.getString(metadataKey);
            if (metadataValue != null) {
                if (!formattedMetadata.isEmpty()) {
                    formattedMetadata.append("\n");
                }
                formattedMetadata.append(metadataKey).append(": ").append(metadataValue);
            }
        }
        return formattedMetadata.toString();
    }

    /**
     * Formats the given metadata of a {@link Content} and includes the specified {@link
     * ContentMetadata} keys.
     *
     * @param metadata
     * @return
     */
    private String format(Map<ContentMetadata, Object> metadata) {
        StringBuilder formattedMetadata = new StringBuilder();
        for (ContentMetadata metadataKey : contentMetadataToInclude) {
            Object metadataValue = metadata.get(metadataKey);
            if (metadataValue != null) {
                if (!formattedMetadata.isEmpty()) {
                    formattedMetadata.append("\n");
                }
                formattedMetadata
                        .append(metadataKey.toString().toLowerCase())
                        .append(": ")
                        .append(metadataValue);
            }
        }
        return formattedMetadata.toString();
    }

    /**
     * Formats the given segment content, segment metadata, and content metadata strings.
     *
     * @param segmentContent
     * @param segmentMetadata
     * @param contentMetadata
     * @return
     */
    private String format(String segmentContent, String segmentMetadata, String contentMetadata) {
        if (segmentMetadata.isEmpty()) {
            return contentMetadata.isEmpty()
                    ? segmentContent
                    : String.format("content: %s%n%s", segmentContent, contentMetadata);
        }

        if (contentMetadata.isEmpty()) {
            return String.format("content: %s%n%s", segmentContent, segmentMetadata);
        }

        return String.format(
                "content: %s%n%s%n%s", segmentContent, segmentMetadata, contentMetadata);
    }
}
