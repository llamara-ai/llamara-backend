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

import com.github.llamara.ai.internal.internal.security.session.AuthenticatedUserSessionManagerImpl;
import com.github.llamara.ai.internal.internal.security.session.Session;
import com.github.llamara.ai.internal.internal.security.session.SessionNotFoundException;
import com.github.llamara.ai.internal.internal.security.session.UserAwareSessionRepository;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AuthenticatedUserManagerImpl}. */
@QuarkusTest
class AuthenticatedUserManagerTest {
    private static final String OWN_USERNAME = "test";
    private static final String OWN_DISPLAYNAME = "Test";

    @InjectSpy TestUserRepository userRepository;
    @InjectSpy UserAwareSessionRepository userAwareSessionRepository;
    @InjectMock AuthenticatedUserSessionManagerImpl sessionManager;

    @InjectMock SecurityIdentity identity;
    @InjectMock UserInfo userInfo;

    private AuthenticatedUserManagerImpl userManager;

    @Transactional
    @BeforeEach
    void setup() {
        userManager =
                new AuthenticatedUserManagerImpl(
                        userRepository, sessionManager, identity, userInfo);

        setupIdentity(OWN_USERNAME, OWN_DISPLAYNAME);

        userRepository.init();

        clearAllInvocations();

        assertEquals(1, userRepository.count());
        assertEquals(0, userAwareSessionRepository.count());
    }

    @Transactional
    @AfterEach
    void destroy() {
        userAwareSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    void clearAllInvocations() {
        clearInvocations(userRepository, userAwareSessionRepository);
    }

    /**
     * Set up the mock for the {@link SecurityIdentity} and {@link UserInfo} to return the given
     * username and display name.
     *
     * @param username the username
     * @param displayName the display name
     */
    void setupIdentity(String username, String displayName) {
        when(identity.getPrincipal()).thenReturn(() -> username);
        when(userInfo.getName()).thenReturn(displayName);
    }

    /**
     * Set up a user in the database for the current {@link SecurityIdentity} and {@link UserInfo}.
     */
    @Transactional
    void setupUser() {
        String username = identity.getPrincipal().getName();
        String displayName = userInfo.getName();

        User user = new User(username);
        user.setDisplayName(displayName);
        userRepository.persist(user);
        assertEquals(username, user.getUsername());
        assertEquals(displayName, user.getDisplayName());
        assertEquals(0, user.getSessions().size());

        clearAllInvocations();
    }

    /**
     * Set up a session for the user of the current {@link SecurityIdentity} and {@link UserInfo}.
     * Validates the session and enforces that the user has exactly one session.
     *
     * @return the ID of the created session
     */
    @Transactional
    UUID setupSession() {
        User user = userRepository.findByUsername(identity.getPrincipal().getName());
        Session session = new Session(user);
        user.addSession(session);
        userRepository.persist(user);
        assertEquals(identity.getPrincipal().getName(), session.getUser().getUsername());
        assertEquals(
                1,
                userRepository
                        .findByUsername(identity.getPrincipal().getName())
                        .getSessions()
                        .size());
        assertEquals(1, userAwareSessionRepository.count());

        clearAllInvocations();

        return session.getId();
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
