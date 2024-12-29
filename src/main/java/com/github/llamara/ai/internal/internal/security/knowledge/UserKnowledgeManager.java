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
import java.util.UUID;

import com.github.llamara.ai.internal.internal.knowledge.EmptyFileException;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeManager;
import com.github.llamara.ai.internal.internal.knowledge.storage.FileStorage;
import com.github.llamara.ai.internal.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import io.quarkus.security.ForbiddenException;

/**
 * {@link KnowledgeManager} that manages knowledge for a user identified by its {@link
 * io.quarkus.security.identity.SecurityIdentity}. Authentication itself is handled by the OIDC
 * provider, e.g. Keycloak.
 *
 * <p>Knowledge can only be managed by authenticated users.
 *
 * <p>Users must log in before any other operation can be performed. If the user is not logged in
 * and tries to perform an operation, the operation can fail with {@link ForbiddenException}.
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
}
