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
package com.github.llamara.ai.internal.internal.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.llamara.ai.internal.config.SecurityConfig;
import com.github.llamara.ai.internal.internal.security.session.AnonymousUserSessionManagerImpl;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Augments the {@link SecurityIdentity} with the role {@link Roles#ANONYMOUS_USER} if {@link
 * SecurityConfig#anonymousUserEnabled()} is {@code true}. This allows configurable anonymous access
 * to the application, which is then handled by the {@link AnonymousUserSessionManagerImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
public class AnonymousUserSecurityIdentityAugmentor implements SecurityIdentityAugmentor {
    private final SecurityConfig config;

    @Inject
    AnonymousUserSecurityIdentityAugmentor(SecurityConfig config) {
        this.config = config;
    }

    @Override
    public Uni<SecurityIdentity> augment(
            SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous() && config.anonymousUserEnabled()) {
            return Uni.createFrom()
                    .item(
                            QuarkusSecurityIdentity.builder(identity)
                                    .addRole(Roles.ANONYMOUS_USER)
                                    .build());
        } else {
            return Uni.createFrom().item(identity);
        }
    }
}
