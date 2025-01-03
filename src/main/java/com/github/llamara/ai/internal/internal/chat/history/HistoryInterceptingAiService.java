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
package com.github.llamara.ai.internal.internal.chat.history;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.github.llamara.ai.internal.internal.chat.aiservice.ChatModelAiService;
import com.github.llamara.ai.internal.internal.chat.aiservice.DelegatingChatModelAiService;
import com.github.llamara.ai.internal.internal.chat.aiservice.DelegatingTokenStream;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;

/**
 * Delegate allowing intercepting prompts and responses for a supplied {@link ChatModelAiService} to
 * store them in the {@link ChatHistoryStore}.
 *
 * @author Florian Hotze - Initial contribution
 */
public class HistoryInterceptingAiService extends DelegatingChatModelAiService {
    private final BiConsumer<UUID, String> promptConsumer;
    private final BiConsumer<UUID, AiMessage> responseConsumer;

    public HistoryInterceptingAiService(ChatModelAiService delegate, ChatHistoryStore store) {
        super(delegate);

        this.promptConsumer =
                (sessionId, prompt) ->
                        store.addMessage(sessionId, ChatMessageType.USER, prompt)
                                .subscribe()
                                .with(item -> {}, failure -> {});
        this.responseConsumer =
                (sessionId, response) ->
                        store.addMessage(sessionId, response.type(), response.text())
                                .subscribe()
                                .with(item -> {}, failure -> {});
    }

    @Override
    public String chat(UUID sessionId, boolean history, String prompt) {
        if (!history) {
            return super.chat(sessionId, false, prompt);
        }

        promptConsumer.accept(sessionId, prompt);
        String response = super.chat(sessionId, true, prompt);
        responseConsumer.accept(sessionId, new AiMessage(response));
        return response;
    }

    @Override
    public TokenStream chatAndStreamResponse(UUID sessionId, boolean history, String prompt) {
        if (!history) {
            return super.chatAndStreamResponse(sessionId, false, prompt);
        }

        promptConsumer.accept(sessionId, prompt);
        return new CompletionInterceptingTokenStream(
                super.chatAndStreamResponse(sessionId, true, prompt),
                response -> responseConsumer.accept(sessionId, response.aiMessage()));
    }

    /**
     * Delegate allowing intercepting {@link TokenStream#onComplete(Consumer)} for a supplied {@link
     * TokenStream}.
     */
    private static class CompletionInterceptingTokenStream extends DelegatingTokenStream {
        private final Consumer<ChatResponse> completionHandler;

        CompletionInterceptingTokenStream(
                TokenStream delegate, Consumer<ChatResponse> completionHandler) {
            super(delegate);
            this.completionHandler = completionHandler;
        }

        @Override
        public TokenStream onCompleteResponse(Consumer<ChatResponse> completionHandler) {
            super.onCompleteResponse(
                    response -> {
                        this.completionHandler.accept(response);
                        completionHandler.accept(response);
                    });
            return this;
        }
    }
}
