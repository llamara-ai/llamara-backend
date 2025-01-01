package com.github.llamara.ai.internal.internal.security.user;

/**
 * Interface specifying the API for managing users. A user is identified by its {@link
 * io.quarkus.security.identity.SecurityIdentity} and {@link io.quarkus.oidc.UserInfo}.
 * Authentication itself is handled by the OIDC provider, e.g. Keycloak.
 *
 * <p>Users must register before any user-specific operation can be performed. If the user has not
 * registered and tries to perform an operation, the operation can fail with {@link
 * UserNotRegisteredException}.
 */
public interface UserManager {
    /**
     * Registers the user in, i.e. creates or updates the user in the database.
     *
     * @return {@code true} if the user was created, {@code false} if the user was updated
     */
    boolean register();

    /**
     * Enforces that the user is registered. If the user is not registered, a {@link
     * UserNotRegisteredException} is thrown.
     *
     * @throws UserNotRegisteredException
     */
    void enforceRegistered() throws UserNotRegisteredException;

    /** Deletes the current user and all his data. This includes removing all sessions. */
    void delete();
}
