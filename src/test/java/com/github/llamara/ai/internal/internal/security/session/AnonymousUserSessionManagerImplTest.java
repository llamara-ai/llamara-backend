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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.llamara.ai.internal.config.UserSecurityConfig;
import com.github.llamara.ai.internal.internal.chat.history.ChatMessageRecord;
import com.github.llamara.ai.internal.internal.security.user.UserNotLoggedInException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Tests for {@link AnonymousUserSessionManagerImpl}. */
@QuarkusTest
class AnonymousUserSessionManagerImplTest {
    private static final List<ChatMessage> CHAT_HISTORY =
            List.of(new UserMessage("Hello, world!"), new AiMessage("Hi!"));

    @InjectMock UserSecurityConfig securityConfig;
    @InjectMock ChatMemoryStore chatMemoryStore;

    private AnonymousUserSessionManagerImpl userSecurityManager;

    @BeforeEach
    void setup() {
        when(securityConfig.anonymousUserEnabled()).thenReturn(true);
        when(securityConfig.anonymousUserSessionTimeout())
                .thenReturn(60); // ensure tests have enough time to run without scheduled deletion
        // kicking in
        userSecurityManager = new AnonymousUserSessionManagerImpl(securityConfig, chatMemoryStore);
    }

    @AfterEach
    void destroy() {
        userSecurityManager.shutdown();
        userSecurityManager = null;
    }

    @Test
    void shutdownDeletesAllSessions() {
        List<UUID> sessionIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            sessionIds.add(userSecurityManager.createSession().getId());
        }

        userSecurityManager.shutdown();
        verify(chatMemoryStore, times(1)).deleteMessages(sessionIds.get(0));
        verify(chatMemoryStore, times(1)).deleteMessages(sessionIds.get(1));
        verify(chatMemoryStore, times(1)).deleteMessages(sessionIds.get(2));
    }

    @Test
    void loginAlwaysClaimsToHaveCreatedUser() {
        assertFalse(userSecurityManager.login());
    }

    @Test
    void enforceUserLoggedInAlwaysThrowsException() {
        assertThrows(UserNotLoggedInException.class, () -> userSecurityManager.enforceLoggedIn());
    }

    @Test
    void deleteDoesNothing() {
        userSecurityManager.delete();
        Mockito.verify(chatMemoryStore, never()).deleteMessages(Mockito.any());
    }

    @Test
    void checkSessionReturnsFalseForNonExistingSession() {
        assertFalse(userSecurityManager.checkSession(UUID.randomUUID()));
    }

    @Test
    void checkSessionReturnsTrueForExistingSession() {
        UUID sessionId = userSecurityManager.createSession().getId();
        assertTrue(userSecurityManager.checkSession(sessionId));
    }

    @Test
    void checkSessionPostponesDeletionForExistingSession() throws InterruptedException {
        when(securityConfig.anonymousUserSessionTimeout()).thenReturn(1);

        UUID sessionId = userSecurityManager.createSession().getId();
        verify(chatMemoryStore, never()).deleteMessages(sessionId);

        when(securityConfig.anonymousUserSessionTimeout()).thenReturn(2);
        userSecurityManager.checkSession(sessionId);
        verify(chatMemoryStore, never()).deleteMessages(sessionId);
        Thread.sleep(2500); // NOSONAR: sleep is necessary to wait for scheduled deletion
        verify(chatMemoryStore, times(1)).deleteMessages(sessionId);
    }

    @Test
    void getSessionsAlwaysReturnsEmptyList() {
        userSecurityManager.createSession();
        userSecurityManager.createSession();

        assertEquals(0, userSecurityManager.getSessions().size());
    }

    @Test
    void createSessionSchedulesDeletionForExistingSession() throws InterruptedException {
        when(securityConfig.anonymousUserSessionTimeout()).thenReturn(1);

        UUID sessionId = userSecurityManager.createSession().getId();
        verify(chatMemoryStore, never()).deleteMessages(sessionId);
        Thread.sleep(1500); // NOSONAR: sleep is necessary to wait for scheduled deletion
        verify(chatMemoryStore, times(1)).deleteMessages(sessionId);
    }

    @Test
    void deleteSessionThrowsExceptionForNonExistingSession() {
        assertThrows(
                SessionNotFoundException.class,
                () -> userSecurityManager.deleteSession(UUID.randomUUID()));
    }

    @Test
    void deleteSessionDeletesMessagesFromChatMemoryStore() {
        UUID sessionId = userSecurityManager.createSession().getId();

        assertDoesNotThrow(() -> userSecurityManager.deleteSession(sessionId));
        Mockito.verify(chatMemoryStore, times(1)).deleteMessages(sessionId);
    }

    @Test
    void getChatHistoryAlwaysReturnsEmptyList() {
        UUID sessionId = UUID.randomUUID();
        when(chatMemoryStore.getMessages(sessionId)).thenReturn(CHAT_HISTORY);

        Uni<List<ChatMessageRecord>> uni =
                assertDoesNotThrow(() -> userSecurityManager.getChatHistory(sessionId));
        UniAssertSubscriber<Collection<ChatMessageRecord>> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(Collections.emptyList());
    }
}
