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

import com.github.llamara.ai.internal.chat.history.ChatHistoryStore;
import com.github.llamara.ai.internal.chat.history.ChatMessageRecord;
import com.github.llamara.ai.internal.security.BaseForAuthenticatedUserTests;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AuthenticatedUserSessionManagerImpl}. */
@QuarkusTest
class AuthenticatedUserSessionManagerImplTest extends BaseForAuthenticatedUserTests {
    private static final String FOREIGN_USERNAME = "foreign";
    private static final String FOREIGN_DISPLAYNAME = "Foreign";
    private static final List<ChatMessage> CHAT_HISTORY =
            List.of(new UserMessage("Hello, world!"), new AiMessage("Hi!"));

    @InjectMock ChatMemoryStore chatMemoryStore;
    @InjectMock ChatHistoryStore chatHistoryStore;

    private AuthenticatedUserSessionManagerImpl sessionManager;

    @Transactional
    @BeforeEach
    @Override
    protected void setup() {
        sessionManager =
                new AuthenticatedUserSessionManagerImpl(
                        userRepository,
                        userAwareSessionRepository,
                        chatMemoryStore,
                        chatHistoryStore,
                        identity);
        clearAllInvocations();
        super.setup();
    }

    @Transactional
    @AfterEach
    @Override
    protected void destroy() {
        for (Session session : userAwareSessionRepository.listAll()) {
            chatMemoryStore.deleteMessages(session.getId());
            chatHistoryStore.deleteMessages(session.getId()).await().indefinitely();
        }
        super.destroy();
    }

    void clearAllInvocations() {
        clearInvocations(
                userRepository, userAwareSessionRepository, chatMemoryStore, chatHistoryStore);
    }

    void verifyNothingDeleted() {
        verify(chatMemoryStore, never()).deleteMessages(any());
        verify(chatHistoryStore, never()).deleteMessages(any());
        verify(userAwareSessionRepository, never()).delete(any());
        verify(userRepository, never()).delete(any());
    }

    @Nested
    class WithUser {
        @BeforeEach
        void setup() {
            setupUser();
        }

        @Test
        void enforceSessionValidThrowsSessionNotFoundException() {
            assertThrows(
                    SessionNotFoundException.class,
                    () -> sessionManager.enforceSessionValid(UUID.randomUUID()));
        }

        @Test
        void getSessionsReturnsEmptyList() {
            assertEquals(0, sessionManager.getSessions().size());
        }

        @Test
        void createSessionCreatesNewSession() {
            Session session = sessionManager.createSession();
            assertEquals(OWN_USERNAME, session.getUser().getUsername());
            assertEquals(1, userRepository.findByUsername(OWN_USERNAME).getSessions().size());
            assertEquals(1, userAwareSessionRepository.count());
        }

        @Test
        void deleteSessionThrowsAndDoesNothing() {
            assertThrows(
                    SessionNotFoundException.class,
                    () -> sessionManager.deleteSession(UUID.randomUUID()));
            verifyNothingDeleted();
        }

        @Test
        void getChatHistoryThrowsAndDoesNothing() {
            assertThrows(
                    SessionNotFoundException.class,
                    () -> sessionManager.getChatHistory(UUID.randomUUID()).await().indefinitely());
            verify(chatHistoryStore, never()).getMessages(any());
        }
    }

    @Nested
    class WithUserAndForeignSession {
        UUID foreignSessionId;

        @BeforeEach
        void setup() {
            setupIdentity(FOREIGN_USERNAME, FOREIGN_DISPLAYNAME);
            setupUser();
            foreignSessionId = setupSession();
            setupIdentity(OWN_USERNAME, OWN_DISPLAYNAME);
            setupUser();
        }

        @Test
        void enforceSessionValidThrowsSessionNotFoundExceptionForForeignSession() {
            assertThrows(
                    SessionNotFoundException.class,
                    () -> sessionManager.enforceSessionValid(foreignSessionId));
        }

        @Test
        void getSessionsDoesNotReturnForeignSession() {
            assertEquals(0, sessionManager.getSessions().size());
        }

        @Test
        void deleteSessionThrowsAndDoesNothingForForeignSession() {
            assertThrows(
                    SessionNotFoundException.class,
                    () -> sessionManager.deleteSession(foreignSessionId));
            verifyNothingDeleted();
        }

        @Test
        void getChatHistoryThrowsAndDoesNothingForForeignSession() {
            assertThrows(
                    SessionNotFoundException.class,
                    () -> sessionManager.getChatHistory(foreignSessionId).await().indefinitely());
            verify(chatHistoryStore, never()).getMessages(any());
        }
    }

    @Nested
    class WithUserAndOwnSession {
        UUID ownSessionId;

        @BeforeEach
        void setup() {
            setupUser();
            ownSessionId = setupSession();
        }

        @Test
        void enforceSessionValidDoesNotThrowSessionNotFoundExceptionForOwnSession() {
            assertDoesNotThrow(() -> sessionManager.enforceSessionValid(ownSessionId));
        }

        @Test
        void getSessionsReturnsOwnSession() {
            assertEquals(
                    ownSessionId,
                    sessionManager.getSessions().stream()
                            .filter(session -> session.getId().equals(ownSessionId))
                            .findFirst()
                            .orElseThrow()
                            .getId());
            assertEquals(1, sessionManager.getSessions().size());
        }

        @Test
        void deleteSessionDeletesOwnSession() throws SessionNotFoundException {
            sessionManager.deleteSession(ownSessionId);
            verify(chatMemoryStore, times(1)).deleteMessages(ownSessionId);
            verify(chatHistoryStore, times(1)).deleteMessages(ownSessionId);
            verify(userAwareSessionRepository, times(1)).delete(any());
            assertEquals(0, userAwareSessionRepository.count());
        }

        @Test
        void getChatHistoryReturnsOwnChatHistory() {
            when(chatMemoryStore.getMessages(ownSessionId)).thenReturn(CHAT_HISTORY);
            Uni<List<ChatMessageRecord>> uni =
                    assertDoesNotThrow(() -> sessionManager.getChatHistory(ownSessionId));
            UniAssertSubscriber<Collection<ChatMessageRecord>> subscriber =
                    uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            subscriber.assertCompleted().assertItem(Collections.emptyList());
        }
    }
}
