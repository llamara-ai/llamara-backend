package com.github.llamara.ai.internal.internal.security.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import com.github.llamara.ai.internal.internal.security.session.AuthenticatedUserSessionManagerImpl;
import com.github.llamara.ai.internal.internal.security.session.Session;
import com.github.llamara.ai.internal.internal.security.session.SessionNotFoundException;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;

@Typed(AuthenticatedUserManagerImpl.class)
@ApplicationScoped
public class AuthenticatedUserManagerImpl implements UserManager {
    private final UserRepository userRepository;
    private final AuthenticatedUserSessionManagerImpl authenticatedUserSessionManager;

    private final SecurityIdentity identity;
    private final UserInfo userInfo;

    @Inject
    AuthenticatedUserManagerImpl(
            UserRepository userRepository,
            AuthenticatedUserSessionManagerImpl authenticatedUserSessionManager,
            SecurityIdentity identity,
            UserInfo userInfo) {
        this.userRepository = userRepository;
        this.authenticatedUserSessionManager = authenticatedUserSessionManager;
        this.identity = identity;
        this.userInfo = userInfo;
    }

    @Override
    public boolean register() {
        boolean created = false;
        QuarkusTransaction.begin();
        User user = userRepository.findByUsername(identity.getPrincipal().getName());
        if (user == null) {
            user = new User(identity.getPrincipal().getName());
            Log.debug(
                    String.format(
                            "User '%s' not found in database, creating new user.",
                            user.getUsername()));
            created = true;
        }
        Log.debug(String.format("User '%s' found in database, updating user.", user.getUsername()));
        user.setDisplayName(userInfo.getName());
        userRepository.persist(user);
        QuarkusTransaction.commit();
        return created;
    }

    @Override
    public void enforceRegistered() {
        User user = userRepository.findByUsername(identity.getPrincipal().getName());
        if (user == null) {
            throw new UserNotRegisteredException(identity.getPrincipal().getName());
        }
    }

    @Override
    public void delete() {
        String username = identity.getPrincipal().getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UserNotRegisteredException(username);
        }
        for (Session session : user.getSessions()) {
            try {
                authenticatedUserSessionManager.deleteSession(session.getId());
            } catch (SessionNotFoundException e) {
                Log.fatal(
                        String.format(
                                "Unexpectedly failed to delete session '%s' during deletion of user"
                                        + " '%s'.",
                                session.getId(), user.getUsername()),
                        e);
            }
        }
        QuarkusTransaction.begin();
        userRepository.delete(user);
        QuarkusTransaction.commit();
        Log.debug(String.format("Deleted user '%s' from database.", user.getUsername()));
    }
}
