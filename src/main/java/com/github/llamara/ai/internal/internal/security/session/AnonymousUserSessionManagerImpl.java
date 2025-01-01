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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import com.github.llamara.ai.internal.config.SecurityConfig;
import com.github.llamara.ai.internal.internal.chat.history.ChatMessageRecord;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Shutdown;
import io.smallrye.mutiny.Uni;

/**
 * Implementation of {@link SessionManager} for handling anonymous users.
 *
 * <p>Anonymous users can only have one session, cannot list existing sessions and have no chat
 * history. The session is automatically deleted after a configurable timeout.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(AnonymousUserSessionManagerImpl.class)
@ApplicationScoped
public class AnonymousUserSessionManagerImpl implements SessionManager {
    private final SecurityConfig config;
    private final ChatMemoryStore chatMemoryStore;

    private final Set<UUID> anonymousSessions = Collections.synchronizedSet(new HashSet<>());
    private final ScheduledExecutorService deletionScheduler = Executors.newScheduledThreadPool(1);
    private final Map<UUID, ScheduledFuture<?>> scheduledDeletions =
            Collections.synchronizedMap(new HashMap<>());

    @Inject
    public AnonymousUserSessionManagerImpl(SecurityConfig config, ChatMemoryStore chatMemoryStore) {
        this.config = config;
        this.chatMemoryStore = chatMemoryStore;
    }

    @Shutdown
    public void shutdown() {
        Set<UUID> sessions = new HashSet<>(anonymousSessions); // prevent CME
        for (UUID sessionId : sessions) {
            try {
                deleteSession(sessionId);
            } catch (SessionNotFoundException e) {
                Log.fatal(
                        String.format(
                                "Unexpectedly failed to delete anonymous session '%s' during"
                                        + " shutdown.",
                                sessionId),
                        e);
            }
        }
    }

    @Override
    public boolean checkSession(UUID sessionId) {
        boolean valid = anonymousSessions.contains(sessionId);
        if (valid) {
            scheduleDeletion(sessionId);
        }
        return valid;
    }

    private void scheduleDeletion(UUID sessionId) {
        ScheduledFuture<?> future = scheduledDeletions.get(sessionId);
        if (future != null && !future.isDone()) {
            Log.debug(String.format("Rescheduling cleanup for anonymous session '%s'.", sessionId));
            future.cancel(false);
        } else {
            Log.debug(String.format("Scheduling cleanup for anonymous session '%s'.", sessionId));
        }
        future =
                deletionScheduler.schedule(
                        () -> {
                            Log.debug(
                                    String.format(
                                            "Cleaning up expired anonymous session '%s'.",
                                            sessionId));
                            chatMemoryStore.deleteMessages(sessionId);
                            anonymousSessions.remove(sessionId);
                            scheduledDeletions.remove(sessionId);
                        },
                        config.anonymousUserSessionTimeout(),
                        java.util.concurrent.TimeUnit.SECONDS);
        scheduledDeletions.put(sessionId, future);
    }

    @Override
    public Collection<Session> getSessions() {
        return Collections.emptySet();
    }

    @Override
    public Session createSession() {
        Session session = new AnonymousSession();
        anonymousSessions.add(session.getId());
        scheduleDeletion(session.getId());
        Log.debug(String.format("Created new anonymous session '%s'.", session.getId()));
        return session;
    }

    @Override
    public void deleteSession(UUID sessionId) throws SessionNotFoundException {
        if (!anonymousSessions.contains(sessionId)) {
            throw new SessionNotFoundException(sessionId);
        }
        scheduledDeletions.get(sessionId).cancel(false);
        scheduledDeletions.remove(sessionId);
        chatMemoryStore.deleteMessages(sessionId);
        anonymousSessions.remove(sessionId);
        Log.debug(String.format("Deleted anonymous session '%s'.", sessionId));
    }

    @Override
    public Uni<List<ChatMessageRecord>> getChatHistory(UUID sessionId) {
        return Uni.createFrom().item(Collections::emptyList);
    }
}
