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

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link JwtUserInfoClaimsSecurityIdentityAugmentor}. */
@QuarkusTest
class JWTUserInfoClaimsSecurityIdentityAugmentorTest {
    private static final Supplier<QuarkusSecurityIdentity.Builder> IDENTITY_BUILDER =
            () -> QuarkusSecurityIdentity.builder().setAnonymous(false).addRole("user");

    private JwtUserInfoClaimsSecurityIdentityAugmentor augmentor;

    @BeforeEach
    void setup() {
        augmentor = new JwtUserInfoClaimsSecurityIdentityAugmentor();
    }

    @Test
    void doesNothingIfNotJwtPrincipal() {
        // given
        SecurityIdentity identity = IDENTITY_BUILDER.get().setPrincipal(() -> "test").build();

        // when
        Uni<SecurityIdentity> uni = augmentor.augment(identity, null);

        // then
        UniAssertSubscriber<SecurityIdentity> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        SecurityIdentity result = subscriber.assertCompleted().getItem();
        assertEquals(identity, result);
    }

    @Test
    void usesPreferredUsernameClaimAsPrincipal() throws InvalidJwtException {
        // given
        String upn = "upn";
        String sub = "sub";
        String preferredUsername = "preferred_username";
        SecurityIdentity identity =
                IDENTITY_BUILDER
                        .get()
                        .setPrincipal(
                                new DefaultJWTCallerPrincipal(
                                        null,
                                        JwtClaims.parse(
                                                String.format(
                                                        "{\"upn\":\"%s\",\"sub\":\"%s\",\"preferred_username\":\"%s\"}",
                                                        upn, sub, preferredUsername))))
                        .build();
        assertEquals(upn, identity.getPrincipal().getName());

        // when
        Uni<SecurityIdentity> uni = augmentor.augment(identity, null);

        // then
        UniAssertSubscriber<SecurityIdentity> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        SecurityIdentity result = subscriber.assertCompleted().getItem();
        assertEquals(preferredUsername, result.getPrincipal().getName());
    }

    @Test
    void addsFullNameClaimAsAttribute() throws InvalidJwtException {
        // given
        String fullName = "full_name";
        SecurityIdentity identity =
                IDENTITY_BUILDER
                        .get()
                        .setPrincipal(
                                new DefaultJWTCallerPrincipal(
                                        null,
                                        JwtClaims.parse(
                                                String.format("{\"full_name\":\"%s\"}", fullName))))
                        .build();

        // when
        Uni<SecurityIdentity> uni = augmentor.augment(identity, null);

        // then
        UniAssertSubscriber<SecurityIdentity> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        SecurityIdentity result = subscriber.assertCompleted().getItem();
        assertEquals(fullName, result.getAttribute(Claims.full_name.name()));
    }

    @Test
    void fallsBackToNameClaimForFullNameAttribute() throws InvalidJwtException {
        // given
        String fullName = "given_name";
        SecurityIdentity identity =
                IDENTITY_BUILDER
                        .get()
                        .setPrincipal(
                                new DefaultJWTCallerPrincipal(
                                        null,
                                        JwtClaims.parse(
                                                String.format("{\"name\":\"%s\"}", fullName))))
                        .build();

        // when
        Uni<SecurityIdentity> uni = augmentor.augment(identity, null);

        // then
        UniAssertSubscriber<SecurityIdentity> subscriber =
                uni.subscribe().withSubscriber(UniAssertSubscriber.create());
        SecurityIdentity result = subscriber.assertCompleted().getItem();
        assertEquals(fullName, result.getAttribute(Claims.full_name.name()));
    }
}
