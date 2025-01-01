package com.github.llamara.ai.internal.internal.security.user;

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

import com.github.llamara.ai.internal.internal.security.AuthenticatedUserTest;
import com.github.llamara.ai.internal.internal.security.session.AuthenticatedUserSessionManagerImpl;
import com.github.llamara.ai.internal.internal.security.session.SessionNotFoundException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AuthenticatedUserManagerImpl}. */
@QuarkusTest
class AuthenticatedUserManagerTest extends AuthenticatedUserTest {
    @InjectMock AuthenticatedUserSessionManagerImpl sessionManager;

    private AuthenticatedUserManagerImpl userManager;

    @Transactional
    @BeforeEach
    @Override
    protected void setup() {
        userManager =
                new AuthenticatedUserManagerImpl(
                        userRepository, sessionManager, identity, userInfo);

        clearAllInvocations();
        super.setup();
    }

    void clearAllInvocations() {
        clearInvocations(userRepository, userAwareSessionRepository);
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
    void deleteThrowsAndDoesNothingIfNotExists() throws SessionNotFoundException {
        assertThrows(UserNotRegisteredException.class, () -> userManager.delete());
        verify(userRepository, never()).delete(any());
        verify(sessionManager, never()).deleteSession(any());
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
            when(userInfo.getName()).thenReturn(newDisplayName);

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
}
