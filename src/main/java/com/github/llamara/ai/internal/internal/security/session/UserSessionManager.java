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
package com.github.llamara.ai.internal.internal.security.session;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.github.llamara.ai.internal.internal.chat.history.ChatMessageRecord;
import com.github.llamara.ai.internal.internal.security.user.UserNotRegisteredException;
import io.smallrye.mutiny.Uni;

/**
 * Defines the interface for handling chat sessions for a user identified by its {@link
 * io.quarkus.security.identity.SecurityIdentity} and {@link io.quarkus.oidc.UserInfo}.
 * Authentication itself is handled by the OIDC provider, e.g. Keycloak.
 *
 * <p>Users must log in before any other operation can be performed. If the user is not logged in
 * and tries to perform an operation, the operation can fail with {@link
 * UserNotRegisteredException}.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface UserSessionManager {
    /**
     * Registers the user in, i.e. creates or updates the user in the database.
     *
     * @return {@code true} if the user was created, {@code false} if the user was updated
     */
    boolean register();

    /**
     * Enforces that the user is registered. If the user is not registered, a {@link
     * UserNotRegisteredException} is thrown.
     *
     * @throws UserNotRegisteredException
     */
    void enforceRegistered() throws UserNotRegisteredException;

    /** Deletes the current user and all his data. This includes removing all sessions. */
    void delete();

    /**
     * Check whether the given session is valid for the current user.
     *
     * @param sessionId the ID of the session to check
     * @return {@code true} if the session is valid, {@code false} otherwise
     */
    boolean checkSession(UUID sessionId);

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
}
