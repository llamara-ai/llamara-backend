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

import java.util.Collection;

import com.github.llamara.ai.config.chat.ChatModelConfig;
import com.github.llamara.ai.internal.chat.aiservice.ChatModelAiService;

/**
 * The chat model provider provides access to the configured {@link ChatModelContainer}s.
 *
 * <p>It processes the {@link ChatModelConfig} and creates the {@link ChatModelAiService}s to
 * interface with the configured models.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface ChatModelProvider {

    /**
     * Get the available chat models.
     *
     * @return available chat models
     */
    Collection<ChatModelContainer> getModels();

    /**
     * Get the chat model specified by its UID.
     *
     * @param uid the UID of the chat model
     * @return the chat model
     * @throws ChatModelNotFoundException if no {@link ChatModelContainer} with the given UID was
     *     found
     */
    ChatModelContainer getModel(String uid) throws ChatModelNotFoundException;
}
