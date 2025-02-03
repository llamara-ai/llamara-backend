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

/**
 * Interface specifying the API for managing users. A user is identified by its {@link
 * io.quarkus.security.identity.SecurityIdentity} and {@link io.quarkus.oidc.UserInfo}.
 * Authentication itself is handled by the OIDC provider, e.g. Keycloak.
 *
 * <p>Users must register before any user-specific operation can be performed. If the user has not
 * registered and tries to perform an operation, the operation can fail with {@link
 * UserNotRegisteredException}.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface UserManager {
    /**
     * Register current the user in, i.e. create or update the user in the database.
     *
     * @return {@code true} if the user was created, {@code false} if the user was updated
     */
    boolean register();

    /**
     * Enforce that the user is registered. If the user is not registered, a {@link
     * UserNotRegisteredException} is thrown.
     *
     * @throws UserNotRegisteredException if the user is not registered
     */
    void enforceRegistered() throws UserNotRegisteredException;

    /**
     * Delete the current user and all his data. This includes removing all sessions.
     *
     * @throws UserNotRegisteredException if the user is not registered
     */
    void delete() throws UserNotRegisteredException;

    /**
     * Get the current user.
     *
     * @return the user
     * @throws UserNotRegisteredException if the user is not registered
     */
    User getCurrentUser() throws UserNotRegisteredException;

    /**
     * Get the user with the given username.
     *
     * @param username name of the user
     * @return the user
     * @throws UserNotFoundException if no user with the given username was found
     */
    User getUser(String username) throws UserNotFoundException;

    /**
     * Get {@link com.github.llamara.ai.internal.security.Users#ANY} from persistence.
     *
     * @return the "any" user
     */
    User getUserAny();
}
