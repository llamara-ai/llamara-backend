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
package com.github.llamara.ai.internal.knowledge.storage;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.file.FileSystemException;

/**
 * Implementation of the {@link FileStorage} interface that stores files on the local file system.
 *
 * @author Florian Hotze - Initial contribution
 */
class FSFileStorageImpl implements FileStorage {
    private final io.vertx.core.file.FileSystem fileSystem;
    private final io.vertx.mutiny.core.file.FileSystem reactiveFileSystem;
    private final String storagePath;

    FSFileStorageImpl(
            String storagePath,
            io.vertx.core.Vertx vertx,
            io.vertx.mutiny.core.Vertx reactiveVertx) {
        this.fileSystem = vertx.fileSystem();
        this.reactiveFileSystem = reactiveVertx.fileSystem();
        this.storagePath = storagePath;

        if (!fileSystem.existsBlocking(storagePath)) {
            Log.infof("Creating storage directory '%s' ...", storagePath);
            fileSystem.mkdirsBlocking(storagePath);
        }
    }

    @Override
    public void storeFile(String checksum, Path file, Map<String, String> metadata)
            throws UnexpectedFileStorageFailureException {
        try {
            fileSystem.copyBlocking(file.toString(), Paths.get(storagePath, checksum).toString());
        } catch (FileSystemException e) {
            throw new UnexpectedFileStorageFailureException(
                    String.format("Failed to store file with checksum '%s'", checksum), e);
        }
        Log.infof("Stored file '%s' with checksum '%s'.", file, checksum);
    }

    @Override
    public FileContainer getFile(String checksum)
            throws FileNotFoundException, UnexpectedFileStorageFailureException {
        byte[] bytes;
        try {
            bytes =
                    fileSystem
                            .readFileBlocking(Paths.get(storagePath, checksum).toString())
                            .getBytes();
        } catch (FileSystemException e) {
            if (e.getCause() instanceof NoSuchFileException) {
                throw new FileNotFoundException(
                        String.format("File not found for checksum '%s'", checksum));
            }
            throw new UnexpectedFileStorageFailureException(
                    "Unexpected exception thrown while getting file", e);
        }
        InputStream content = new ByteArrayInputStream(bytes);
        return new FileContainer(content, Collections.emptyMap());
    }

    @Override
    public void deleteFile(String checksum) {
        reactiveFileSystem
                .delete(Paths.get(storagePath, checksum).toString())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .subscribe()
                .with(
                        item -> Log.infof("Deleted file with checksum '%s'.", checksum),
                        failure -> {});
    }

    @Override
    public void deleteAllFiles() {
        reactiveFileSystem
                .deleteRecursive(Paths.get(storagePath).toString(), true)
                .onItem()
                .transformToUni(uni -> reactiveFileSystem.mkdir(Paths.get(storagePath).toString()))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .subscribe()
                .with(
                        item -> Log.info("Deleted all files"),
                        failure -> Log.errorf("Failed to delete all files: %s", failure));
    }
}
