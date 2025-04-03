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
package com.github.llamara.ai.internal.security.knowledge;

import com.github.llamara.ai.config.SecurityConfig;
import com.github.llamara.ai.internal.Utils;
import com.github.llamara.ai.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.knowledge.KnowledgeManager;
import com.github.llamara.ai.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.knowledge.persistence.Knowledge;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.Roles;
import com.github.llamara.ai.internal.security.user.User;
import com.github.llamara.ai.internal.security.user.UserManager;
import com.github.llamara.ai.internal.security.user.UserNotFoundException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * Implementation of {@link UserKnowledgeManager}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(UserKnowledgeManager.class)
@ApplicationScoped
public class UserKnowledgeManagerImpl implements UserKnowledgeManager {
    private final KnowledgeManager delegate;
    private final SecurityConfig config;
    private final UserAwareKnowledgeRepository userAwareRepository;
    private final UserManager userManager;

    private final SecurityIdentity identity;

    @Inject
    UserKnowledgeManagerImpl(
            KnowledgeManager delegate,
            SecurityConfig config,
            UserAwareKnowledgeRepository userAwareRepository,
            UserManager userManager,
            SecurityIdentity identity) {
        this.delegate = delegate;
        this.config = config;
        this.userAwareRepository = userAwareRepository;
        this.userManager = userManager;
        this.identity = identity;
    }

    @Override
    public Collection<Knowledge> getAllKnowledge() {
        String username = identity.getPrincipal().getName();
        userManager.enforceRegistered();

        // Admin user: Return all knowledge
        if (identity.hasRole(Roles.ADMIN)) {
            Log.debugf("Admin user '%s' requested knowledge, returning all knowledge.", username);
            return delegate.getAllKnowledge();
        }

        // Anonymous user: Return only public knowledge
        Set<Knowledge> publicKnowledge = new HashSet<>(userManager.getUserAny().getKnowledge());
        if (identity.isAnonymous()) {
            Log.debug("Anonymous user requested knowledge, returning only public knowledge.");
            return publicKnowledge;
        }

        // Authenticated, non-admin user: Return user knowledge and public knowledge
        Log.debugf(
                "Authenticated, non-admin user '%s' requested knowledge, returning user knowledge"
                        + " and public knowledge.",
                username);
        Set<Knowledge> userKnowledge = new HashSet<>(userManager.getCurrentUser().getKnowledge());
        userKnowledge.addAll(publicKnowledge);
        return userKnowledge;
    }

    @Override
    public Knowledge getKnowledge(UUID id) throws KnowledgeNotFoundException {
        userManager.enforceRegistered();
        Knowledge knowledge = userAwareRepository.findById(id);
        if (knowledge == null) {
            throw new KnowledgeNotFoundException(id);
        }
        return knowledge;
    }

    private void enforceAuthenticated() {
        if (identity.isAnonymous()) {
            throw new ForbiddenException();
        }
    }

    /**
     * Check whether the given knowledge is editable for the current user.
     *
     * <ul>
     *   <li>Admins always have read/write permission.
     * </ul>
     *
     * @param id the ID of the knowledge to check
     * @throws KnowledgeNotFoundException if the knowledge with the given ID does not exist or the
     *     user has no access to it
     * @throws ForbiddenException if the user is not allowed to edit the knowledge, but has read
     *     access
     */
    private void enforceKnowledgeEditable(UUID id)
            throws KnowledgeNotFoundException, ForbiddenException {
        enforceAuthenticated();
        userManager.enforceRegistered();
        if (identity.hasRole(Roles.ADMIN)) {
            return;
        }
        Knowledge knowledge = getKnowledge(id);
        Permission permission = knowledge.getPermission(identity.getPrincipal().getName());
        if (permission == Permission.READONLY) {
            throw new ForbiddenException();
        } else if (permission == Permission.NONE) {
            throw new KnowledgeNotFoundException(id);
        } else if (config.adminWriteOnlyEnabled()) {
            throw new ForbiddenException();
        }
    }

    @Override
    public void deleteKnowledge(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        enforceKnowledgeEditable(id);
        delegate.deleteKnowledge(id);
    }

    @Override
    public UUID addSource(Path file, String fileName, String contentType)
            throws IOException, UnexpectedFileStorageFailureException {
        enforceAuthenticated();
        userManager.enforceRegistered();
        if (!identity.hasRole(Roles.ADMIN) && config.adminWriteOnlyEnabled()) {
            throw new ForbiddenException();
        }

        String checksum = Utils.generateChecksum(file);
        Optional<Knowledge> existingKnowledge = userAwareRepository.existsChecksum(checksum);
        if (existingKnowledge.isPresent()) {
            return existingKnowledge.get().getId();
        }

        return delegate.addSource(
                file, fileName, contentType, new User(identity.getPrincipal().getName()));
    }

    @Override
    public void updateSource(UUID id, Path file, String fileName, String contentType)
            throws IOException, KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        enforceKnowledgeEditable(id);
        delegate.updateSource(id, file, fileName, contentType);
    }

    @Override
    public void setPermission(UUID id, User user, Permission permission)
            throws KnowledgeNotFoundException, IllegalPermissionModificationException {
        enforceKnowledgeEditable(id);
        delegate.setPermission(id, user, permission);
    }

    @Override
    public void setPermission(UUID id, String username, Permission permission)
            throws KnowledgeNotFoundException,
                    UserNotFoundException,
                    IllegalPermissionModificationException {
        User user = userManager.getUser(username);
        setPermission(id, user, permission);
    }

    @Override
    public void removePermission(UUID id, User user)
            throws KnowledgeNotFoundException, IllegalPermissionModificationException {
        enforceKnowledgeEditable(id);
        delegate.removePermission(id, user);
    }

    @Override
    public void removePermission(UUID id, String username)
            throws KnowledgeNotFoundException,
                    UserNotFoundException,
                    IllegalPermissionModificationException {
        User user = userManager.getUser(username);
        removePermission(id, user);
    }

    @Override
    public void addTag(UUID id, String tag) throws KnowledgeNotFoundException {
        enforceKnowledgeEditable(id);
        delegate.addTag(id, tag);
    }

    @Override
    public void removeTag(UUID id, String tag) throws KnowledgeNotFoundException {
        enforceKnowledgeEditable(id);
        delegate.removeTag(id, tag);
    }

    @Override
    public void setLabel(UUID id, String label) throws KnowledgeNotFoundException {
        enforceKnowledgeEditable(id);
        delegate.setLabel(id, label);
    }

    @Override
    public NamedFileContainer getFile(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        userManager.enforceRegistered();
        Knowledge knowledge = getKnowledge(id);
        return delegate.getFile(knowledge.getId());
    }

    @Override
    public void retryFailedIngestion(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        enforceKnowledgeEditable(id);
        delegate.retryFailedIngestion(id);
    }
}
