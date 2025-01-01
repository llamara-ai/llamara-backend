/*
 * #%L
 * llamara-backend
 * %%
 * Copyright (C) 2024 Contributors to the LLAMARA project
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.github.llamara.ai.internal.internal.chat.history.ChatHistoryStore;
import com.github.llamara.ai.internal.internal.chat.history.ChatMessageRecord;
import com.github.llamara.ai.internal.internal.security.user.User;
import com.github.llamara.ai.internal.internal.security.user.UserNotRegisteredException;
import com.github.llamara.ai.internal.internal.security.user.UserRepository;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * {@link UserSessionManager} implementation for handling authenticated users.
 *
 * <p>Authenticated users can have multiple sessions, list existing sessions and have chat history.
 * Their sessions are stored in the database and are not automatically deleted.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(AuthenticatedUserSessionManagerImpl.class)
@ApplicationScoped
public class AuthenticatedUserSessionManagerImpl implements UserSessionManager {
    private final UserRepository userRepository;
    private final UserAwareSessionRepository userAwareSessionRepository;
    private final ChatMemoryStore chatMemoryStore;
    private final ChatHistoryStore chatHistoryStore;

    private final SecurityIdentity identity;
    private final UserInfo userInfo;

    @Inject
    AuthenticatedUserSessionManagerImpl(
            UserRepository userRepository,
            UserAwareSessionRepository userAwareSessionRepository,
            ChatMemoryStore chatMemoryStore,
            ChatHistoryStore chatHistoryStore,
            SecurityIdentity identity,
            UserInfo userInfo) {
        this.userRepository = userRepository;
        this.userAwareSessionRepository = userAwareSessionRepository;
        this.chatMemoryStore = chatMemoryStore;
        this.chatHistoryStore = chatHistoryStore;
        this.identity = identity;
        this.userInfo = userInfo;
    }

    @Override
    public boolean register() {
        boolean created = false;
        QuarkusTransaction.begin();
        User user = userRepository.findByUsername(identity.getPrincipal().getName());
        if (user == null) {
            user = new User(identity.getPrincipal().getName());
            Log.debug(
                    String.format(
                            "User '%s' not found in database, creating new user.",
                            user.getUsername()));
            created = true;
        }
        Log.debug(
                String.format(
                        "User '%s' found in database, updating user.", user.getUsername()));
        user.setDisplayName(userInfo.getName());
        userRepository.persist(user);
        QuarkusTransaction.commit();
        return created;
    }

    @Override
    public void enforceRegistered() {
        User user = userRepository.findByUsername(identity.getPrincipal().getName());
        if (user == null) {
            throw new UserNotRegisteredException(identity.getPrincipal().getName());
        }
    }

    @Override
    public void delete() {
        User user = getUser();
        for (Session session : user.getSessions()) {
            deleteSessionData(session.getId());
        }
        QuarkusTransaction.begin();
        userRepository.delete(user);
        QuarkusTransaction.commit();
        Log.debug(String.format("Deleted user '%s' from database.", user.getUsername()));
    }

    @Override
    public boolean checkSession(UUID sessionId) {
        try {
            userAwareSessionRepository.findById(sessionId);
        } catch (SessionNotFoundException e) {
            return false;
        }
        return true;
    }

    private User getUser() {
        String username = identity.getPrincipal().getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UserNotRegisteredException(username);
        }
        return user;
    }

    /**
     * Get all sessions for the current user.
     *
     * @return the IDs of all sessions
     */
    @Override
    public Collection<Session> getSessions() {
        return getUser().getSessions();
    }

    @Override
    public Session createSession() {
        QuarkusTransaction.begin();
        User user = getUser();
        Session session = new Session(user);
        user.addSession(session);
        userRepository.persist(user);
        QuarkusTransaction.commit(); // commit transaction to get the ID for the logging
        Log.debug(
                String.format(
                        "Created new session '%s' for user '%s'.",
                        session.getId(), session.getUser().getUsername()));
        return session;
    }

    private void deleteSessionData(UUID sessionId) {
        chatMemoryStore.deleteMessages(sessionId);
        chatHistoryStore.deleteMessages(sessionId).subscribe().with(item -> {}, failure -> {});
    }

    @Transactional
    @Override
    public void deleteSession(UUID sessionId) throws SessionNotFoundException {
        userAwareSessionRepository.deleteById(sessionId);
        deleteSessionData(sessionId);
        Log.debug(
                String.format(
                        "Deleted session '%s' for user '%s'.",
                        sessionId, identity.getPrincipal().getName()));
    }

    @Override
    public Uni<List<ChatMessageRecord>> getChatHistory(UUID sessionId)
            throws SessionNotFoundException {
        if (!checkSession(sessionId)) {
            throw new SessionNotFoundException(sessionId);
        }
        return chatHistoryStore.getMessages(sessionId);
    }
}
