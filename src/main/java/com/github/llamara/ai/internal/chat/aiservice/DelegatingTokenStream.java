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

import java.util.List;
import java.util.function.Consumer;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;

/**
 * {@link TokenStream} implementation that delegates to a supplied {@link TokenStream} instance.
 * Allows overriding specific methods.
 *
 * @author Florian Hotze - Initial contribution
 */
public abstract class DelegatingTokenStream implements TokenStream {

    private final TokenStream delegate;

    protected DelegatingTokenStream(TokenStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public TokenStream onPartialResponse(Consumer<String> consumer) {
        delegate.onPartialResponse(consumer);
        return this;
    }

    @Override
    public TokenStream onNext(Consumer<String> tokenHandler) {
        delegate.onNext(tokenHandler);
        return this;
    }

    @Override
    public TokenStream onRetrieved(Consumer<List<Content>> contentHandler) {
        delegate.onRetrieved(contentHandler);
        return this;
    }

    @Override
    public TokenStream onToolExecuted(Consumer<ToolExecution> toolExecuteHandler) {
        delegate.onToolExecuted(toolExecuteHandler);
        return this;
    }

    @Override
    public TokenStream onCompleteResponse(Consumer<ChatResponse> consumer) {
        delegate.onCompleteResponse(consumer);
        return this;
    }

    @Override
    public TokenStream onComplete(Consumer<Response<AiMessage>> completionHandler) {
        delegate.onComplete(completionHandler);
        return this;
    }

    @Override
    public TokenStream onError(Consumer<Throwable> errorHandler) {
        delegate.onError(errorHandler);
        return this;
    }

    @Override
    public TokenStream ignoreErrors() {
        delegate.ignoreErrors();
        return this;
    }

    @Override
    public void start() {
        delegate.start();
    }
}
