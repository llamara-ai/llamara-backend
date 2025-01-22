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
package com.github.llamara.ai.internal.security.session;

import com.github.llamara.ai.internal.chat.history.ChatMessageRecord;
import com.github.llamara.ai.internal.security.user.User;
import com.github.llamara.ai.internal.security.user.UserNotRegisteredException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.smallrye.mutiny.Uni;

/**
 * Interface specifying the API for managing sessions. A session is identified by a UUID and is
 * owned by a {@link User}.
 *
 * <p>Users must register before any user-specific operation can be performed. If the user has not
 * registered and tries to perform an operation, the operation can fail with {@link
 * UserNotRegisteredException}.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface SessionManager {
    /**
     * Enforces that the session with the given ID is valid for the current user. If the session is
     * not valid, a {@link SessionNotFoundException} is thrown.
     *
     * @param sessionId the ID of the session to check
     * @throws SessionNotFoundException if no session with the given ID was found for the current
     */
    void enforceSessionValid(UUID sessionId) throws SessionNotFoundException;

    /**
     * Get all sessions for the current user.
     *
     * @return all sessions
     */
    Collection<Session> getSessions();

    /**
     * Creates a new session for the current user. The UUID must be generated on the server side to
     * prevent session hijacking attacks.
     *
     * @return the new session
     */
    Session createSession();

    /**
     * Delete the given session for the current user. This includes deleting all messages from the
     * chat memory and history.
     *
     * @param sessionId the ID of the session to remove
     * @throws SessionNotFoundException if no session with the given ID was found for the current
     *     user
     */
    void deleteSession(UUID sessionId) throws SessionNotFoundException;

    /**
     * Get the chat history for the given session.
     *
     * @param sessionId the ID of the session
     * @return the chat history
     * @throws SessionNotFoundException if no session with the given ID was found for the current
     *     user
     */
    Uni<List<ChatMessageRecord>> getChatHistory(UUID sessionId) throws SessionNotFoundException;

    /**
     * Sets the label of the given session.
     *
     * @param sessionId the ID of the session
     * @param label the new session label
     * @throws SessionNotFoundException if no session with the given ID was found for the current
     *     user
     */
    void setSessionLabel(UUID sessionId, String label) throws SessionNotFoundException;
}
