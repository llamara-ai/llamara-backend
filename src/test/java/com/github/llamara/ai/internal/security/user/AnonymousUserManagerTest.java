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

import com.github.llamara.ai.internal.security.Users;
import com.github.llamara.ai.internal.security.session.AnonymousUserSessionManagerImpl;
import com.github.llamara.ai.internal.security.session.SessionNotFoundException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;

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
