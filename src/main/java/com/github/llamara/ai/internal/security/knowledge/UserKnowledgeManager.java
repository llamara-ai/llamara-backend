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

import com.github.llamara.ai.internal.knowledge.EmptyFileException;
import com.github.llamara.ai.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.knowledge.KnowledgeManager;
import com.github.llamara.ai.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.knowledge.storage.FileStorage;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.user.User;
import com.github.llamara.ai.internal.security.user.UserNotFoundException;
import com.github.llamara.ai.internal.security.user.UserNotRegisteredException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import io.quarkus.security.ForbiddenException;

/**
 * {@link KnowledgeManager} that manages knowledge for a user identified by its {@link
 * io.quarkus.security.identity.SecurityIdentity}. Authentication itself is handled by the OIDC
 * provider, e.g. Keycloak.
 *
 * <p>Knowledge can only be managed by authenticated users, but anonymous users are allowed to get
 * public knowledge. If a anonymous users tries to perform an operation that requires
 * authentication, the operation must fail with {@link ForbiddenException}.
 *
 * <p>Users must register before any operation can be performed. If the user is not registered and
 * tries to perform an operation, the operation can fail with {@link UserNotRegisteredException}.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface UserKnowledgeManager extends KnowledgeManager {
    /**
     * Add a file source to the knowledge.
     *
     * <p>If the file is empty, throw {@link IOException}.
     *
     * <p>If the file is already source of another knowledge, return the id of the existing
     * knowledge.
     *
     * @param file file specified by its {@link Path}
     * @param fileName name to use for the file
     * @param contentType content (MIME) type of the file
     * @return the persisted unique id of the newly created knowledge or <code>Optional.empty()
     * </code> if the file is empty
     * @throws EmptyFileException if the file is empty
     * @throws IOException if calculating the file checksum failed
     * @throws UnexpectedFileStorageFailureException if a {@link FileStorage} operation failed
     *     unexpectedly
     */
    @Override
    UUID addSource(Path file, String fileName, String contentType)
            throws IOException, UnexpectedFileStorageFailureException;

    /**
     * Set the {@link Permission} for a {@link User} for a knowledge specified by its id.
     *
     * <p>Implementation should delegate to {@link UserKnowledgeManager#setPermission(UUID, User,
     * Permission)}.
     *
     * @param id persistent unique id of knowledge
     * @param username the name of the user to set the permission for
     * @param permission the permission to set
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     * @throws UserNotFoundException if no user with the given username was found
     * @throws IllegalPermissionModificationException if the permission modification is illegal
     */
    void setPermission(UUID id, String username, Permission permission)
            throws KnowledgeNotFoundException,
                    UserNotFoundException,
                    IllegalPermissionModificationException;

    /**
     * Remove the permission for a {@link User} for a knowledge specified by its id.
     *
     * <p>Implementation should delegate to {@link UserKnowledgeManager#setPermission(UUID, User,
     * Permission)}.
     *
     * @param id persistent unique id of knowledge
     * @param username the name of the user to remove the permission for
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     * @throws UserNotFoundException if no user with the given username was found
     * @throws IllegalPermissionModificationException if the permission modification is illegal
     */
    void removePermission(UUID id, String username)
            throws KnowledgeNotFoundException,
                    UserNotFoundException,
                    IllegalPermissionModificationException;
}
