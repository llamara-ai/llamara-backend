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
package com.github.llamara.ai.internal.rest;

import com.github.llamara.ai.config.SecurityConfig;
import com.github.llamara.ai.config.frontend.OidcConfig;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.common.annotation.NonBlocking;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * REST resource for the root endpoint.
 *
 * @author Florian Hotze - Initial contribution
 */
@PermitAll
@Path("/rest")
class RootResource {
    private final SecurityInfoDTO securityInfo;
    private final OidcInfoDTO oidcInfo;

    @Inject
    RootResource(
            SecurityConfig securityConfig,
            OidcConfig oidcConfig,
            @ConfigProperty(name = "quarkus.oidc.auth-server-url") String oidcAuthServerUrl) {
        securityInfo = new SecurityInfoDTO();
        securityInfo.anonymousUserEnabled = securityConfig.anonymousUserEnabled();
        securityInfo.anonymousUserSessionTimeout = securityConfig.anonymousUserSessionTimeout();
        oidcInfo = new OidcInfoDTO();
        oidcInfo.authServerUrl = oidcAuthServerUrl;
        oidcInfo.clientId = oidcConfig.clientId();
        oidcInfo.authorizationPath = oidcConfig.authorizationPath();
        oidcInfo.logoutPath = oidcConfig.logoutPath();
        oidcInfo.tokenPath = oidcConfig.tokenPath();
    }

    @NonBlocking
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "configuration",
            summary = "Get configuration required by the frontend.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = InfoDTO.class)))
    public InfoDTO configuration() {
        InfoDTO infoDTO = new InfoDTO();
        infoDTO.security = securityInfo;
        infoDTO.oidc = oidcInfo;
        return infoDTO;
    }

    public static class InfoDTO {
        public SecurityInfoDTO security; // NOSONAR: this is a DTO
        public OidcInfoDTO oidc; // NOSONAR: this is a DTO
    }

    public static class SecurityInfoDTO {
        public boolean anonymousUserEnabled; // NOSONAR: this is a DTO
        public int anonymousUserSessionTimeout; // NOSONAR: this is a DTO
    }

    public static class OidcInfoDTO {
        public String authServerUrl; // NOSONAR: this is a DTO
        public String clientId; // NOSONAR: this is a DTO
        public String authorizationPath; // NOSONAR: this is a DTO
        public String logoutPath; // NOSONAR: this is a DTO
        public String tokenPath; // NOSONAR: this is a DTO
    }
}
