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
package com.github.llamara.ai.internal.internal.knowledge.storage;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Interface specifying the API for storing and retrieving files.
 *
 * @author Florian Hotze
 */
public interface FileStorage {

    /**
     * Store a given file with the given metadata identified by its checksum in the storage. If a
     * file with the same checksum already exists, it will be overwritten.
     *
     * @param checksum the checksum of the file, used to identify the file in the storage
     * @param file the file to store
     * @param metadata the metadata to store with the file
     * @throws UnexpectedFileStorageFailureException if the storage operation failed unexpectedly
     */
    void storeFile(String checksum, Path file, Map<String, String> metadata)
            throws UnexpectedFileStorageFailureException;

    /**
     * Get the file identified by its checksum from the storage.
     *
     * <p>The returned {@link InputStream} MUST be closed by the caller after use to release
     * resources.
     *
     * @param checksum the checksum of the file to get
     * @return the file as {@link InputStream}
     * @throws FileNotFoundException if no file with the checksum exists
     * @throws UnexpectedFileStorageFailureException if the storage operation failed unexpectedly
     */
    FileContainer getFile(String checksum)
            throws FileNotFoundException, UnexpectedFileStorageFailureException;

    /**
     * Delete the file identified by its checksum from the storage. If no file with the checksum
     * exists, do nothing.
     *
     * @param checksum the checksum of the file to delete
     * @throws UnexpectedFileStorageFailureException if the storage operation failed unexpectedly
     */
    void deleteFile(String checksum) throws UnexpectedFileStorageFailureException;
}
