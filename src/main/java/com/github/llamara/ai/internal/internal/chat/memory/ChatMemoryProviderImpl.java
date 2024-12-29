/*
 * #%L
 * llamara-backend
 * %%
 * Copyright (C) 2024 Contributors to the LLAMARA project
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
package com.github.llamara.ai.internal.internal.chat.memory;

import java.util.UUID;
import java.util.concurrent.CompletionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.llamara.ai.internal.config.chat.ChatMemoryConfig;
import com.github.llamara.ai.internal.config.chat.ChatModelConfig;
import com.github.llamara.ai.internal.internal.StartupException;
import com.github.llamara.ai.internal.internal.chat.history.ChatHistoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkus.runtime.Startup;

/**
 * Implementation of the {@link ChatMemoryProvider} using the {@link ChatModelConfig} and {@link
 * ChatMemoryStore}.
 *
 * <p>Please note that this does not keep all messages. The {@link ChatHistoryStore} is responsible
 * for keeping all messages between the user and the AI. See <a
 * href="https://docs.langchain4j.dev/tutorials/chat-memory/#memory-vs-history">Memory vs
 * History</a> for more information.
 *
 * @author Florian Hotze - Initial contribution
 */
@Startup // initialize at startup to check connection and validate config
@ApplicationScoped
class ChatMemoryProviderImpl implements ChatMemoryProvider {
    private static final String INITIALIZATION_FAILURE_MESSAGE = "Failed to initialize chat memory";

    private final ChatMemoryProvider delegate;

    @Inject
    ChatMemoryProviderImpl(ChatMemoryConfig config, ChatMemoryStore store) {
        // Check connection
        try {
            store.getMessages(UUID.randomUUID());
        } catch (CompletionException e) {
            throw new StartupException("Failed to connect to Redis chat memory DB", e.getCause());
        }

        this.delegate =
                switch (config.window()) {
                    case MESSAGE -> memoryId -> {
                        if (config.maxMessages().isEmpty()) {
                            throw new StartupException(
                                    INITIALIZATION_FAILURE_MESSAGE,
                                    new IllegalStateException(
                                            "maxMessages config must be set for message window"));
                        }
                        return MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(config.maxMessages().get())
                                .chatMemoryStore(store)
                                .build();
                    };
                    case TOKEN -> memoryId -> {
                        if (config.maxTokens().isEmpty() || config.tokenizer().isEmpty()) {
                            throw new StartupException(
                                    INITIALIZATION_FAILURE_MESSAGE,
                                    new IllegalStateException(
                                            "maxTokens and tokenizer config must be set for token"
                                                    + " window"));
                        }
                        return TokenWindowChatMemory.builder()
                                .id(memoryId)
                                .maxTokens(
                                        config.maxTokens().get(),
                                        produceTokenizer(config.tokenizer().get()))
                                .chatMemoryStore(store)
                                .build();
                    };
                };
    }

    private Tokenizer produceTokenizer(ChatMemoryConfig.TokenizerConfig config) {
        return switch (config.provider()) {
            case OPENAI -> new OpenAiTokenizer(config.model());
        };
    }

    @Override
    public ChatMemory get(Object o) {
        return delegate.get(o);
    }
}
