package com.github.llamara.ai.internal.internal.security.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;

import com.github.llamara.ai.internal.internal.security.Users;
import com.github.llamara.ai.internal.internal.security.session.AnonymousUserSessionManagerImpl;
import com.github.llamara.ai.internal.internal.security.session.SessionNotFoundException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Tests for {@link AnonymousUserManagerImpl}. */
@QuarkusTest
class AnonymousUserManagerTest {
    @InjectMock AnonymousUserSessionManagerImpl sessionManager;

    private AnonymousUserManagerImpl userManager;

    @BeforeEach
    void setup() {
        userManager = new AnonymousUserManagerImpl();
    }

    @Test
    void registerAlwaysClaimsToHaveCreatedUser() {
        assertTrue(userManager.register());
    }

    @Test
    void enforceRegisteredDoesNotThrowException() {
        assertDoesNotThrow(() -> userManager.enforceRegistered());
    }

    @Test
    void deleteDoesNothing() throws SessionNotFoundException {
        userManager.delete();
        Mockito.verify(sessionManager, never()).deleteSession(Mockito.any());
    }

    @Test
    void getUserReturnsUsersANY() {
        assertEquals(Users.ANY, userManager.getUser());
    }
}
