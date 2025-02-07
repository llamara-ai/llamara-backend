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
package com.github.llamara.ai.internal.chat.aiservice;

import com.github.llamara.ai.config.chat.ChatModelConfig;

import java.util.UUID;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;

/**
 * {@link ChatModelAiService} implementation that delegates to a supplied {@link ChatModelAiService}
 * instance and stores its {@link ChatModelConfig.ModelConfig}. Allows overriding specific methods
 * and accessing the chat model configuration.
 *
 * @author Florian Hotze - Initial contribution
 */
public abstract class DelegatingChatModelAiService implements ChatModelAiService {
    private final ChatModelAiService delegate;
    private final ChatModelConfig.ModelConfig config;

    protected DelegatingChatModelAiService(
            ChatModelAiService delegate, ChatModelConfig.ModelConfig config) {
        this.delegate = delegate;
        this.config = config;
    }

    /**
     * Get the model configuration of this {@link ChatModelAiService}.
     *
     * @return configuration of the chat model
     */
    public ChatModelConfig.ModelConfig config() {
        return config;
    }

    @Override
    public Result<String> chat(UUID sessionId, boolean history, String prompt) {
        return delegate.chat(sessionId, history, prompt);
    }

    @Override
    public Result<String> chatWithoutSystemMessage(UUID sessionId, boolean history, String prompt) {
        return delegate.chatWithoutSystemMessage(sessionId, history, prompt);
    }

    @Override
    public TokenStream chatAndStreamResponse(UUID sessionId, boolean history, String prompt) {
        return delegate.chatAndStreamResponse(sessionId, history, prompt);
    }

    @Override
    public String clean(String text) {
        return delegate.clean(text);
    }
}
