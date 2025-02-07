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
import com.github.llamara.ai.internal.chat.aiservice.ChatModelAiService;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Container for a chat model, containing information about the chat model and the {@link
 * ChatModelAiService}s.
 *
 * @param uid the UID of the chat model
 * @param label the label of the chat model
 * @param description the description of the chat model
 * @param provider the {@link ChatModelConfig.ChatModelProvider}
 * @param config the {@link ChatModelConfig.ModelConfig}
 * @param service the {@link ChatModelAiService} of the chat model
 * @author Florian Hotze - Initial contribution
 */
public record ChatModelContainer(
        String uid,
        String label,
        String description,
        ChatModelConfig.ChatModelProvider provider,
        @JsonIgnore ChatModelConfig.ModelConfig config,
        @JsonIgnore ChatModelAiService service) {}
