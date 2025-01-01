/*
 * #%L
 * llamara-backend
 * %%
 * Copyright (C) 2024 Contributors to the LLAMARA project
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
package com.github.llamara.ai.internal.internal.security.knowledge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import com.github.llamara.ai.internal.internal.Utils;
import com.github.llamara.ai.internal.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeManager;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.internal.security.Permission;
import com.github.llamara.ai.internal.internal.security.Roles;
import com.github.llamara.ai.internal.internal.security.session.UserSessionManager;
import com.github.llamara.ai.internal.internal.security.user.User;
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
    private final UserAwareKnowledgeRepository userAwareRepository;
    private final UserSessionManager userSessionManager;

    private final SecurityIdentity identity;

    @Inject
    UserKnowledgeManagerImpl(
            KnowledgeManager delegate,
            UserAwareKnowledgeRepository userAwareRepository,
            UserSessionManager userSessionManager,
            SecurityIdentity identity) {
        this.delegate = delegate;
        this.userAwareRepository = userAwareRepository;
        this.userSessionManager = userSessionManager;
        this.identity = identity;
    }

    @Override
    public Collection<Knowledge> getAllKnowledge() {
        userSessionManager.enforceRegistered();
        return userAwareRepository.listAll();
    }

    @Override
    public Knowledge getKnowledge(UUID id) throws KnowledgeNotFoundException {
        userSessionManager.enforceRegistered();
        Knowledge knowledge = userAwareRepository.findById(id);
        if (knowledge == null) {
            throw new KnowledgeNotFoundException(id);
        }
        return knowledge;
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
        userSessionManager.enforceRegistered();
        if (identity.hasRole(Roles.ADMIN)) {
            return;
        }
        Knowledge knowledge = getKnowledge(id);
        Permission permission = knowledge.getPermission(identity.getPrincipal().getName());
        if (permission == Permission.READONLY) {
            throw new ForbiddenException();
        } else if (permission == Permission.NONE) {
            throw new KnowledgeNotFoundException(id);
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
        userSessionManager.enforceRegistered();

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
    public void removePermission(UUID id, User user)
            throws KnowledgeNotFoundException, IllegalPermissionModificationException {
        enforceKnowledgeEditable(id);
        delegate.removePermission(id, user);
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
        userSessionManager.enforceRegistered();
        Knowledge knowledge = getKnowledge(id);
        return delegate.getFile(knowledge.getId());
    }
}
