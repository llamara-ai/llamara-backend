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

import com.github.llamara.ai.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.knowledge.KnowledgeManager;
import com.github.llamara.ai.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.knowledge.persistence.Knowledge;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.Users;
import com.github.llamara.ai.internal.security.session.AuthenticatedUserSessionManagerImpl;
import com.github.llamara.ai.internal.security.session.Session;
import com.github.llamara.ai.internal.security.session.SessionNotFoundException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.Claims;

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

    @Inject
    AuthenticatedUserManagerImpl(
            UserRepository userRepository,
            AuthenticatedUserSessionManagerImpl authenticatedUserSessionManager,
            KnowledgeManager knowledgeManager,
            SecurityIdentity identity) {
        this.userRepository = userRepository;
        this.authenticatedUserSessionManager = authenticatedUserSessionManager;
        this.knowledgeManager = knowledgeManager;
        this.identity = identity;
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
        } else {
            Log.debugf("User '%s' found in database, updating user.", user.getUsername());
        }
        user.setDisplayName(identity.getAttribute(Claims.full_name.name()));
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
        User user = getCurrentUser();
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
    public User getCurrentUser() throws UserNotRegisteredException {
        User user = userRepository.findByUsername(identity.getPrincipal().getName());
        if (user == null) {
            throw new UserNotRegisteredException(identity.getPrincipal().getName());
        }
        return user;
    }

    @Override
    public User getUser(String username) throws UserNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException(username);
        }
        return user;
    }

    @Override
    public User getUserAny() {
        return userRepository.findByUsername(Users.ANY_USERNAME);
    }
}
