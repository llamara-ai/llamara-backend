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

import com.github.llamara.ai.internal.security.Roles;
import com.github.llamara.ai.internal.security.user.UserManager;

import java.util.Set;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Blocking;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * REST resource for the user endpoint.
 *
 * @author Florian Hotze - Initial contribution
 */
@Path("/rest/user")
class UserResource {

    private final UserManager userManager;
    private final SecurityIdentity identity;
    private final UserInfo userInfo;

    @Inject
    UserResource(UserManager userManager, SecurityIdentity identity, UserInfo userInfo) {
        this.userManager = userManager;
        this.identity = identity;
        this.userInfo = userInfo;
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER, Roles.ANONYMOUS_USER})
    @Blocking
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "login",
            summary = "Login and get the user information based on the OIDC token.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = UserInfoDTO.class)))
    public UserInfoDTO login() {
        userManager.register();
        return new UserInfoDTO(identity, userInfo);
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @DELETE
    @ResponseStatus(200)
    @Operation(operationId = "deleteUserData", summary = "Delete all data for the user.")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(
            responseCode = "400",
            description =
                    "Bad Request. Returned when an operation is requested before the user is logged"
                            + " in.")
    public void delete() {
        userManager.delete();
    }

    public static class UserInfoDTO {
        public String username; // NOSONAR: this is a DTO
        public Set<String> roles; // NOSONAR: this is a DTO
        public boolean anonymous; // NOSONAR: this is a DTO
        public String name; // NOSONAR: this is a DTO

        UserInfoDTO(SecurityIdentity identity, UserInfo userInfo) {
            this.username = identity.getPrincipal().getName();
            if (this.username.isBlank()) {
                this.username = null;
            }
            this.roles = identity.getRoles();
            this.anonymous = identity.isAnonymous();
            this.name = userInfo.getName();
        }
    }
}
