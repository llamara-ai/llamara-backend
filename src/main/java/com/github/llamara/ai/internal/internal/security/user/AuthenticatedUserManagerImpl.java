package com.github.llamara.ai.internal.internal.security.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import com.github.llamara.ai.internal.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeManager;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.internal.security.Permission;
import com.github.llamara.ai.internal.internal.security.session.AuthenticatedUserSessionManagerImpl;
import com.github.llamara.ai.internal.internal.security.session.Session;
import com.github.llamara.ai.internal.internal.security.session.SessionNotFoundException;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * {@link UserManager} implementation for handling authenticated users.
 *
 * <p>Authenticated users are stored in the database.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(AuthenticatedUserManagerImpl.class)
@ApplicationScoped
public class AuthenticatedUserManagerImpl implements UserManager {
    private final UserRepository userRepository;
    private final AuthenticatedUserSessionManagerImpl authenticatedUserSessionManager;
    private final KnowledgeManager knowledgeManager;

    private final SecurityIdentity identity;
    private final UserInfo userInfo;

    @Inject
    AuthenticatedUserManagerImpl(
            UserRepository userRepository,
            AuthenticatedUserSessionManagerImpl authenticatedUserSessionManager,
            KnowledgeManager knowledgeManager,
            SecurityIdentity identity,
            UserInfo userInfo) {
        this.userRepository = userRepository;
        this.authenticatedUserSessionManager = authenticatedUserSessionManager;
        this.knowledgeManager = knowledgeManager;
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
            Log.infof("User '%s' not found in database, creating new entry.", user.getUsername());
            created = true;
        }
        Log.debugf("User '%s' found in database, updating user.", user.getUsername());
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
        User user = getUser();
        for (Session session : user.getSessions()) {
            try {
                authenticatedUserSessionManager.deleteSession(session.getId());
            } catch (SessionNotFoundException e) {
                Log.fatalf(
                        "Unexpectedly failed to delete session '%s' during deletion of user"
                                + " '%s'.",
                        session.getId(), user.getUsername(), e);
            }
        }
        for (Knowledge knowledge : user.getKnowledge()) {
            Permission permission = knowledge.getPermission(user);
            try {
                if (permission == Permission.OWNER) {
                    Log.infof(
                            "Deleting knowledge '%s' of user '%s' from database.",
                            knowledge.getId(), user.getUsername());
                    knowledgeManager.deleteKnowledge(knowledge.getId());
                } else {
                    Log.infof(
                            "Removing permission of user '%s' from knowledge '%s'.",
                            user.getUsername(), knowledge.getId());
                    knowledgeManager.removePermission(knowledge.getId(), user);
                }
            } catch (KnowledgeNotFoundException
                    | UnexpectedFileStorageFailureException
                    | IllegalPermissionModificationException e) {
                Log.fatalf(
                        "Unexpectedly failed to delete knowledge '%s' during deletion of user"
                                + " '%s'.",
                        knowledge.getId(), user.getUsername(), e);
            }
        }
        QuarkusTransaction.begin();
        userRepository.delete(user);
        QuarkusTransaction.commit();
        Log.infof("Deleted user '%s' from database.", user.getUsername());
    }

    @Override
    public User getUser() throws UserNotRegisteredException {
        User user = userRepository.findByUsername(identity.getPrincipal().getName());
        if (user == null) {
            throw new UserNotRegisteredException(identity.getPrincipal().getName());
        }
        return user;
    }
}
