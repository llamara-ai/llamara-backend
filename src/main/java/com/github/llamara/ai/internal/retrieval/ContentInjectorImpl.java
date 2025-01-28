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
import com.github.llamara.ai.internal.MetadataKeys;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;

/**
 * Implementation of the {@link ContentInjector}. It injects the content based on the prompt
 * template from the {@link RetrievalConfig}. See <a
 * href="https://docs.langchain4j.dev/tutorials/rag/#content-injector">LangChain4j Docs: RAG:
 * Content Injector</a>.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class ContentInjectorImpl implements ContentInjector {
    private static final String USER_MESSAGE_TEMPLATE = "{{userMessage}}";

    private final RetrievalConfig config;
    private final ContentInjector delegate;

    @Inject
    ContentInjectorImpl(RetrievalConfig config) {
        this.config = config;
        this.delegate =
                DefaultContentInjector.builder()
                        .metadataKeysToInclude(List.of(MetadataKeys.KNOWLEDGE_ID))
                        .promptTemplate(PromptTemplate.from(config.promptTemplate()))
                        .build();
    }

    @Override
    public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
        if (contents.isEmpty() && chatMessage instanceof UserMessage userMessage) {
            return new UserMessage(
                    config.missingKnowledgePromptTemplate()
                            .replace(USER_MESSAGE_TEMPLATE, userMessage.singleText()));
        }

        return delegate.inject(contents, chatMessage);
    }

    @Override
    public UserMessage inject(List<Content> contents, UserMessage userMessage) {
        if (contents.isEmpty()) {
            return new UserMessage(
                    config.missingKnowledgePromptTemplate()
                            .replace(USER_MESSAGE_TEMPLATE, userMessage.singleText()));
        }

        return delegate
                .inject( // NOSONAR: we need to use this method for implementation of interface
                        contents, userMessage);
    }
}
