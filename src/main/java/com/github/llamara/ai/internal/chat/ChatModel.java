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
package com.github.llamara.ai.internal.chat;

import com.github.llamara.ai.config.chat.ChatModelConfig;
import com.github.llamara.ai.internal.MetadataKeys;
import com.github.llamara.ai.internal.chat.history.ChatHistoryStore;
import com.github.llamara.ai.internal.chat.history.ChatMessageRecord;
import com.github.llamara.ai.internal.chat.response.ChatResponseRecord;
import com.github.llamara.ai.internal.chat.response.RagSourceRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.service.Result;

/**
 * The {@link ChatModel} provides the interface to chat with the chat models. It takes care of
 * storing the chat history, if enabled, and applies the system prompt, if enabled.
 *
 * @author Florian Hotze - Initial contribution
 */
public class ChatModel {
    private final ChatModelConfig.ModelConfig config;
    private final AiService aiService;
    private final ChatHistoryStore historyStore;

    public ChatModel(
            ChatModelConfig.ModelConfig config,
            AiService aiService,
            ChatHistoryStore historyStore) {
        this.config = config;
        this.aiService = aiService;
        this.historyStore = historyStore;
    }

    /**
     * Send a prompt to a chat model and get the response.
     *
     * @param sessionId the session ID
     * @param history whether to save the conversation to the chat history
     * @param prompt the prompt to send to the chat model
     * @return the response from the chat model
     */
    public ChatResponseRecord chat(UUID sessionId, boolean history, String prompt) {
        if (history) {
            storePrompt(sessionId, prompt);
        }

        Result<String> result;
        if (config.systemPromptEnabled()) {
            result = aiService.chat(sessionId, prompt);
        } else {
            result = aiService.chatWithoutSystemMessage(sessionId, prompt);
        }
        ChatResponseRecord response =
                new ChatResponseRecord(
                        result.content(), getSourcesFromResult(result, result.content()));

        if (history) {
            storeResponse(sessionId, response);
        }

        return response;
    }

    /**
     * Get the sources from the result. Filters out sources that were not used by the chat model to
     * generate the response.
     *
     * @param result the {@link Result} from the chat model
     * @param text the text of the response
     * @return the sources used by the chat model to generate the response
     */
    private List<RagSourceRecord> getSourcesFromResult(Result<?> result, String text) {
        return result.sources().stream()
                .map(dev.langchain4j.rag.content.Content::textSegment)
                .filter(ts -> text.contains(ts.metadata().getString(MetadataKeys.KNOWLEDGE_ID)))
                .map(
                        ts ->
                                new RagSourceRecord(
                                        ts.metadata().getUUID(MetadataKeys.KNOWLEDGE_ID),
                                        ts.text()))
                .toList();
    }

    /**
     * Store the prompt in the chat history.
     *
     * @param sessionId the session ID
     * @param prompt the prompt to store
     */
    private void storePrompt(UUID sessionId, String prompt) {
        historyStore
                .addMessage(
                        sessionId,
                        new ChatMessageRecord(
                                ChatMessageType.USER, prompt, Instant.now(), null, null))
                .subscribe()
                .with(item -> {}, failure -> {});
    }

    /**
     * Store the response in the chat history.
     *
     * @param sessionId the session ID
     * @param response the response to store
     */
    private void storeResponse(UUID sessionId, ChatResponseRecord response) {
        historyStore
                .addMessage(
                        sessionId,
                        new ChatMessageRecord(
                                ChatMessageType.AI,
                                response.response(),
                                Instant.now(),
                                response.sources(),
                                config.uid()))
                .subscribe()
                .with(item -> {}, failure -> {});
    }
}
