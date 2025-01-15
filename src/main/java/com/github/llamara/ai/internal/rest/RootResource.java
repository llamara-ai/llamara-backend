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

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * REST resource for the root endpoint.
 *
 * @author Florian Hotze - Initial contribution
 */
@PermitAll
@Path("/rest")
class RootResource {
    private final OidcInfoDTO oidcInfo;

    @Inject
    RootResource(
            @ConfigProperty(name = "quarkus.oidc.auth-server-url") String oidcAuthServerUrl,
            @ConfigProperty(name = "quarkus.oidc.client-id") String oidcClientId,
            @ConfigProperty(name = "quarkus.oidc.authorization-path") String oidcAuthorizationPath,
            @ConfigProperty(name = "quarkus.oidc.logout-path") String oidcLogoutPath,
            @ConfigProperty(name = "quarkus.oidc.token-path") String oidcTokenPath) {
        oidcInfo = new OidcInfoDTO();
        oidcInfo.authServerUrl = oidcAuthServerUrl;
        oidcInfo.clientId = oidcClientId;
        oidcInfo.authorizationPath = oidcAuthorizationPath;
        oidcInfo.logoutPath = oidcLogoutPath;
        oidcInfo.tokenPath = oidcTokenPath;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public InfoDTO info() {
        InfoDTO infoDTO = new InfoDTO();
        infoDTO.oidc = oidcInfo;
        return infoDTO;
    }

    public static class InfoDTO {
        public OidcInfoDTO oidc; // NOSONAR: this is a DTO
    }

    public static class OidcInfoDTO {
        public String authServerUrl; // NOSONAR: this is a DTO
        public String clientId; // NOSONAR: this is a DTO
        public String authorizationPath; // NOSONAR: this is a DTO
        public String logoutPath; // NOSONAR: this is a DTO
        public String tokenPath; // NOSONAR: this is a DTO
    }
}
