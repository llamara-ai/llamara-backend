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
package com.github.llamara.ai.internal.security.user;

import com.github.llamara.ai.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.knowledge.KnowledgeManager;
import com.github.llamara.ai.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.BaseForAuthenticatedUserTests;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.Users;
import com.github.llamara.ai.internal.security.session.AuthenticatedUserSessionManagerImpl;
import com.github.llamara.ai.internal.security.session.SessionNotFoundException;

import java.util.UUID;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AuthenticatedUserManagerImpl}. */
@QuarkusTest
class AuthenticatedUserManagerTest extends BaseForAuthenticatedUserTests {
    @InjectMock AuthenticatedUserSessionManagerImpl sessionManager;
    @InjectMock KnowledgeManager knowledgeManager;

    private AuthenticatedUserManagerImpl userManager;

    @Transactional
    @BeforeEach
    @Override
    protected void setup() {
        userManager =
                new AuthenticatedUserManagerImpl(
                        userRepository, sessionManager, knowledgeManager, identity);

        clearAllInvocations();
        super.setup();
    }

    void clearAllInvocations() {
        clearInvocations(
                userRepository, userAwareSessionRepository, sessionManager, knowledgeManager);
    }

    @Test
    void registerCreatesUserIfNotExists() {
        assertTrue(userManager.register());
        verify(userRepository, times(1)).persist((User) any());
        User user = userRepository.findByUsername(OWN_USERNAME);
        assertEquals(OWN_USERNAME, user.getUsername());
        assertEquals(OWN_DISPLAYNAME, user.getDisplayName());
        assertEquals(0, user.getSessions().size());
    }

    @Test
    void enforceRegisteredThrowsIfNotRegistered() {
        assertThrows(UserNotRegisteredException.class, () -> userManager.enforceRegistered());
    }

    @Test
    void deleteThrowsAndDoesNothingIfNotExists()
            throws SessionNotFoundException,
                    UnexpectedFileStorageFailureException,
                    KnowledgeNotFoundException,
                    IllegalPermissionModificationException {
        assertThrows(UserNotRegisteredException.class, () -> userManager.delete());
        verify(userRepository, never()).delete(any());
        verify(sessionManager, never()).deleteSession(any());
        verify(knowledgeManager, never()).deleteKnowledge(any());
        verify(knowledgeManager, never()).removePermission(any(), any());
    }

    @Test
    void getCurrentUserThrowsIfNotRegistered() {
        assertThrows(UserNotRegisteredException.class, () -> userManager.getCurrentUser());
    }

    @Transactional
    protected User getUserAnyFromPersistence() {
        return userRepository.findByUsername(Users.ANY_USERNAME);
    }

    @Test
    void getUserAnyReturnsUserAnyFromPersistence() {
        assertEquals(getUserAnyFromPersistence(), userManager.getUserAny());
    }

    @Nested
    class WithUser {
        @BeforeEach
        void setup() {
            setupUser();
        }

        @Test
        void loginUpdatesUser() {
            String newDisplayName = "New Name";
            when(identity.getAttribute(Claims.full_name.name())).thenReturn(newDisplayName);

            assertFalse(userManager.register());
            verify(userRepository, times(1)).persist((User) any());
            User user = userRepository.findByUsername(OWN_USERNAME);
            assertEquals(OWN_USERNAME, user.getUsername());
            assertEquals(newDisplayName, user.getDisplayName());
            assertEquals(0, user.getSessions().size());
        }

        @Test
        void deleteDeletesUser() {
            userManager.delete();
            verify(userRepository, times(1)).delete(any());
            assertEquals(1, userRepository.count()); // Users#ANY still exists
        }

        @Test
        void enforceRegisteredDoesNotThrowIfLoggedIn() {
            assertDoesNotThrow(() -> userManager.enforceRegistered());
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
        void deleteDeletesSessions() throws SessionNotFoundException {
            userManager.delete();
            verify(sessionManager, times(1)).deleteSession(ownSessionId);
            verify(userRepository, times(1)).delete(any());
            assertEquals(0, userAwareSessionRepository.count());
        }
    }

    @Nested
    class WithUserAndOwnKnowledge {
        @BeforeEach
        void setup() {
            setupUser();
            setupKnowledgeWithPermission(Permission.OWNER);
        }

        @Test
        void deleteDeletesOwnKnowledge()
                throws UnexpectedFileStorageFailureException, KnowledgeNotFoundException {
            userManager.delete();
            verify(knowledgeManager, times(1)).deleteKnowledge(any());
        }
    }

    @Nested
    class WithUserAndSharedKnowledge {
        @BeforeEach
        void setup() {
            setupUser();
            setupKnowledgeWithPermission(Permission.READONLY);
        }

        @Test
        void deleteRemovesPermissionFromSharedKnowledge()
                throws KnowledgeNotFoundException, IllegalPermissionModificationException {
            userManager.delete();
            verify(knowledgeManager, times(1)).removePermission(any(), any());
        }
    }
}
