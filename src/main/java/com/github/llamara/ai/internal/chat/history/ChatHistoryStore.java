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

import com.github.llamara.ai.config.chat.ChatHistoryConfig;

import java.util.List;

import dev.langchain4j.data.message.ChatMessageType;
import io.smallrye.mutiny.Uni;

/**
 * Interface defining the reactive API for storing chat history (not to be confused with chat
 * memory).
 *
 * <p>History keeps all messages between the user and AI intact. It represents what was actually
 * said. See <a href="https://docs.langchain4j.dev/tutorials/chat-memory#memory-vs-history">Memory
 * vs History</a> for more information.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface ChatHistoryStore {

    /**
     * Get the messages from the chat history for the given id. If there are no messages for the
     * given id, an empty list is returned.
     *
     * @param historyId the history id to get the messages from
     * @return the messages from the chat history, or an empty list if there are no messages for the
     *     given id
     */
    Uni<List<ChatMessageRecord>> getMessages(Object historyId);

    /**
     * Add a message to the chat history for the given id.
     *
     * <p>The implementation MUST ensure that the number of messages in the history does not exceed
     * the configured maximum in {@link ChatHistoryConfig}.
     *
     * @param historyId the history id to add the message to
     * @param type the type of the message
     * @param text the text of the message
     */
    Uni<Void> addMessage(Object historyId, ChatMessageType type, String text);

    /**
     * Delete all messages from the chat history for the given id. If no messages are present for
     * the given id, do nothing.
     *
     * @param memoryId the history id to delete the messages from
     */
    Uni<Void> deleteMessages(Object memoryId);
}
