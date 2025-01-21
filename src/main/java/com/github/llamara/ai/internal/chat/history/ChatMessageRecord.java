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
package com.github.llamara.ai.internal.chat.history;

import com.github.llamara.ai.config.chat.ChatModelConfig;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.langchain4j.data.message.ChatMessageType;

/**
 * Record used for storing a {@link dev.langchain4j.data.message.ChatMessage}.
 *
 * @param type the type of the message, e.g. AI or USER
 * @param text the text of the message
 * @param timestamp the timestamp of the message
 * @param modelProvider if the message is an AI message: the provider of the chat model, else <code>
 *     null</code>
 * @param modelName if the message is an AI message: the name of the chat model, else <code>null
 *     </code>
 * @author Florian Hotze - Initial contribution
 */
public record ChatMessageRecord(
        ChatMessageType type,
        String text,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL) ChatModelConfig.ChatModelProvider modelProvider,
        @JsonInclude(JsonInclude.Include.NON_NULL) String modelName) {}
