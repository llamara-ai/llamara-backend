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
package com.github.llamara.ai.internal.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.llamara.ai.config.SecurityConfig;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link AnonymousUserSecurityIdentityAugmentor}. */
@QuarkusTest
class AnonymousUserSecurityIdentityAugmentorTest {
    @InjectMock SecurityConfig config;

    private AnonymousUserSecurityIdentityAugmentor augmentor;

    @BeforeEach
    void setup() {
        augmentor = new AnonymousUserSecurityIdentityAugmentor(config);
    }

    @Test
    void addsRoleIfAnonymousUserEnabled() {
        // given
        when(config.anonymousUserEnabled()).thenReturn(true);
        SecurityIdentity given = QuarkusSecurityIdentity.builder().setAnonymous(true).build();

        // when
        Uni<SecurityIdentity> uni = augmentor.augment(given, null);

        // when
        UniAssertSubscriber<SecurityIdentity> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        SecurityIdentity result = subscriber.assertCompleted().getItem();
        assertTrue(result.hasRole(Roles.ANONYMOUS_USER));
    }

    @Test
    void doesNotAddRoleIfAnonymousUserDisabled() {
        // given
        when(config.anonymousUserEnabled()).thenReturn(false);
        SecurityIdentity given = QuarkusSecurityIdentity.builder().setAnonymous(true).build();

        // when
        Uni<SecurityIdentity> uni = augmentor.augment(given, null);

        // then
        UniAssertSubscriber<SecurityIdentity> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        SecurityIdentity result = subscriber.assertCompleted().getItem();
        assertFalse(result.hasRole(Roles.ANONYMOUS_USER));
    }

    @Test
    void doesNotAddRoleIfNotAnonymous() {
        // given
        when(config.anonymousUserEnabled()).thenReturn(false);
        SecurityIdentity given =
                QuarkusSecurityIdentity.builder()
                        .setAnonymous(false)
                        .setPrincipal(() -> "test")
                        .addRole(Roles.USER)
                        .build();

        // when
        Uni<SecurityIdentity> uni = augmentor.augment(given, null);

        // then
        UniAssertSubscriber<SecurityIdentity> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        SecurityIdentity result = subscriber.assertCompleted().getItem();
        assertFalse(result.hasRole(Roles.ANONYMOUS_USER));
    }

    @Test
    void doesNotModifyPrincipal() {
        // given
        String principal = "principal";
        SecurityIdentity given =
                QuarkusSecurityIdentity.builder()
                        .setAnonymous(false)
                        .setPrincipal(() -> principal)
                        .build();

        // when
        Uni<SecurityIdentity> uni = augmentor.augment(given, null);

        // then
        UniAssertSubscriber<SecurityIdentity> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        SecurityIdentity result = subscriber.assertCompleted().getItem();
        assertEquals(principal, result.getPrincipal().getName());
    }
}
