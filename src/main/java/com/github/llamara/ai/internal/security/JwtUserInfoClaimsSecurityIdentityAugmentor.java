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

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.jwt.Claims;

/**
 * Augments the {@link SecurityIdentity} with user information claims from the JWT access token.
 * This allows to provide the username and full name through {@link Claims#preferred_username} and
 * {@link Claims#full_name} without needing a dependency on the {@link io.quarkus.oidc.UserInfo}. It
 * also uses {@link Claims#preferred_username} as principal if available.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class JwtUserInfoClaimsSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(
            final SecurityIdentity identity, final AuthenticationRequestContext context) {
        if (!(identity.getPrincipal() instanceof JWTCallerPrincipal principal)) {
            return Uni.createFrom().item(identity);
        }

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

        if (principal.containsClaim(Claims.preferred_username.name())) {
            String username = principal.getClaim(Claims.preferred_username.name());
            Log.tracef(
                    "Using preferred_username claim as principal for SecurityIdentity: %s",
                    username);
            builder.setPrincipal(() -> username);
        }
        if (principal.containsClaim(Claims.full_name.name())) {
            builder.addAttribute(
                    Claims.full_name.name(), principal.getClaim(Claims.full_name.name()));
        }

        return Uni.createFrom().item(builder.build());
    }
}
