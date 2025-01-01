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
package com.github.llamara.ai.internal.internal.security.session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Provides the correct {@link SessionManager} implementation depending on the {@link
 * SecurityIdentity} in a Jakarta REST request.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
public class SessionManagerProducer {
    private final SecurityIdentity identity;
    private final AuthenticatedUserSessionManagerImpl authenticatedUserSessionManager;
    private final AnonymousUserSessionManagerImpl anonymousUserSecurityManager;

    @Inject
    SessionManagerProducer(
            SecurityIdentity identity,
            AuthenticatedUserSessionManagerImpl authenticatedUserSessionManager,
            AnonymousUserSessionManagerImpl anonymousUserSecurityManager) {
        this.identity = identity;
        this.authenticatedUserSessionManager = authenticatedUserSessionManager;
        this.anonymousUserSecurityManager = anonymousUserSecurityManager;
    }

    @Produces
    @Default
    @RequestScoped // selected bean must be request scoped, as identity is request scoped
    SessionManager produceSecurityManager() {
        if (identity.isAnonymous()) {
            return anonymousUserSecurityManager;
        } else {
            return authenticatedUserSessionManager;
        }
    }
}
