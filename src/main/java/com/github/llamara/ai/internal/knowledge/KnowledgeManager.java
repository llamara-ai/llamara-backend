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
package com.github.llamara.ai.internal.knowledge;

import com.github.llamara.ai.internal.MetadataKeys;
import com.github.llamara.ai.internal.ingestion.IngestionStatus;
import com.github.llamara.ai.internal.knowledge.storage.FileStorage;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.user.User;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Interface specifying the API for managing knowledge.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface KnowledgeManager {
    /**
     * Get all available knowledge.
     *
     * @return all available knowledge
     */
    Collection<Knowledge> getAllKnowledge();

    /**
     * Get single knowledge specified by its id.
     *
     * @param id persistent unique id of knowledge
     * @return the knowledge
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     */
    Knowledge getKnowledge(UUID id) throws KnowledgeNotFoundException;

    /**
     * Delete single knowledge specified by its id.
     *
     * <p>Also delete the file from the {@link FileStorage} if no other knowledge has the same file
     * source and the embeddings from the {@link dev.langchain4j.store.embedding.EmbeddingStore}
     *
     * @param id persistent unique id of knowledge
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     * @throws UnexpectedFileStorageFailureException if a {@link FileStorage} operation failed
     */
    void deleteKnowledge(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException;

    /**
     * Set the ingestion status of knowledge identified by its ID if it exists.
     *
     * @param id persistent unique id of knowledge
     * @param status the new ingestion status
     */
    default void setKnowledgeIngestionStatus(UUID id, IngestionStatus status) {
        throw new UnsupportedOperationException("Not supported by this KnowledgeManager.");
    }

    /**
     * Add a file source to the knowledge.
     *
     * <p>If the file is empty, throw {@link IOException}.
     *
     * <p>If the file is already source of another knowledge, create a new index entry but do not
     * store it again.
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
    UUID addSource(Path file, String fileName, String contentType)
            throws IOException, UnexpectedFileStorageFailureException;

    /**
     * {@link KnowledgeManager#addSource(Path, String, String)} with the additional functionality to
     * specify the owner of the added knowledge.
     *
     * <p>Implementations should set {@link Permission#OWNER} for the given {@link User} and add the
     * given user to the embedding's {@link MetadataKeys#PERMISSION} metadata.
     *
     * @param file
     * @param fileName
     * @param contentType
     * @param owner the owner of the knowledge
     * @return
     * @throws EmptyFileException
     * @throws IOException
     * @throws UnexpectedFileStorageFailureException
     */
    default UUID addSource(Path file, String fileName, String contentType, User owner)
            throws IOException, UnexpectedFileStorageFailureException {
        throw new UnsupportedOperationException("Not supported by this KnowledgeManager.");
    }

    /**
     * Update single file source based knowledge specified by its id.
     *
     * <p>If the old file is NO source for other knowledge, delete the old file from the {@link
     * FileStorage}, always delete the old embeddings for the knowledge.
     *
     * @param id persistent unique id of knowledge
     * @param file file specified by its {@link Path}
     * @param fileName name to use for the file
     * @param contentType content (MIME) type of the file
     * @throws EmptyFileException if the file is empty
     * @throws IOException if calculating the file checksum failed
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     * @throws UnexpectedFileStorageFailureException if a {@link FileStorage} operation failed
     *     unexpectedly
     */
    void updateSource(UUID id, Path file, String fileName, String contentType)
            throws IOException, KnowledgeNotFoundException, UnexpectedFileStorageFailureException;

    /**
     * Set the {@link Permission} for a {@link User} for a knowledge specified by its id.
     *
     * <p>Implementation should also update the embeddings' {@link MetadataKeys#PERMISSION}
     * metadata.
     *
     * <p>Implementations must not allow the following permission changes:
     *
     * <ul>
     *   <li>Explicitly setting {@link Permission#NONE}.
     *   <li>Adding {@link Permission#OWNER}. The owner must be added through {@link
     *       KnowledgeManager#addSource(Path, String, String, User)}.
     * </ul>
     *
     * @param id persistent unique id of knowledge
     * @param user the user to set the permission for
     * @param permission the permission to set
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     * @throws IllegalPermissionModificationException if the permission modification is illegal
     */
    void setPermission(UUID id, User user, Permission permission)
            throws KnowledgeNotFoundException, IllegalPermissionModificationException;

    /**
     * Remove the permission for a {@link User} for a knowledge specified by its id.
     *
     * <p>Implementation should also update the embeddings' {@link MetadataKeys#PERMISSION}
     * metadata.
     *
     * @param id persistent unique id of knowledge
     * @param user the user to remove the permission for
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     * @throws IllegalPermissionModificationException if the permission modification is illegal
     */
    void removePermission(UUID id, User user)
            throws KnowledgeNotFoundException, IllegalPermissionModificationException;

    /**
     * Add a tag to a knowledge specified by its id.
     *
     * @param id persistent unique id of knowledge
     * @param tag the tag to add
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     */
    void addTag(UUID id, String tag) throws KnowledgeNotFoundException;

    /**
     * Remove a tag from a knowledge specified by its id.
     *
     * @param id persistent unique id of knowledge
     * @param tag the tag to remove
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     */
    void removeTag(UUID id, String tag) throws KnowledgeNotFoundException;

    /**
     * Set the optional, user-defined label for a knowledge specified by its id.
     *
     * @param id persistent unique id of knowledge
     * @param label the label to set
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     */
    void setLabel(UUID id, String label) throws KnowledgeNotFoundException;

    /**
     * Get the source file of the file-based knowledge specified by its id.
     *
     * @param id persistent unique id of knowledge
     * @return the knowledge source file as {@link InputStream}
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     * @throws UnexpectedFileStorageFailureException if a {@link FileStorage} operation failed
     *     unexpectedly
     */
    NamedFileContainer getFile(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException;

    /**
     * Retry the failed ingestion of a knowledge specified by its id.
     *
     * <p>Implementations should do nothing if the current ingestion status is not {@link
     * IngestionStatus#FAILED}.
     *
     * @param id persistent unique id of knowledge
     * @throws KnowledgeNotFoundException if no knowledge with the given id was found
     * @throws UnexpectedFileStorageFailureException if a {@link FileStorage} operation failed
     *     unexpectedly
     */
    void retryFailedIngestion(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException;

    /**
     * Container for a named file, containing the file name, content and metadata.
     *
     * @param fileName the file name
     * @param content the file content as {@link InputStream}
     * @param metadata the metadata of the file, see {@link MetadataKeys} for possible keys
     */
    record NamedFileContainer(String fileName, InputStream content, Map<String, String> metadata) {}
}
