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

import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link AnonymousUserManagerImpl}. */
@QuarkusTest
class AnonymousUserManagerTest {
    @InjectSpy TestUserRepository userRepository;

    private AnonymousUserManagerImpl userManager;

    @Transactional
    @BeforeEach
    void setup() {
        userManager = new AnonymousUserManagerImpl(userRepository);

        userRepository.init();

        clearInvocations(userRepository);

        assertEquals(1, userRepository.count());
    }

    @Test
    void registerAlwaysClaimsToHaveCreatedUser() {
        assertTrue(userManager.register(any()));
    }

    @Test
    void enforceRegisteredDoesNotThrowException() {
        assertDoesNotThrow(() -> userManager.enforceRegistered());
    }

    @Test
    void deleteDoesNothing() {
        userManager.delete();
        verify(userRepository, never()).delete(any());
    }

    @Transactional
    protected User getUserAnyFromPersistence() {
        return userRepository.findByUsername(Users.ANY_USERNAME);
    }

    @Test
    void getCurrentUserReturnsUserAnyFromPersistence() {
        assertEquals(getUserAnyFromPersistence(), userManager.getCurrentUser());
    }

    @Test
    void getUserAnyReturnsUserAnyFromPersistence() {
        assertEquals(getUserAnyFromPersistence(), userManager.getUserAny());
    }
}
