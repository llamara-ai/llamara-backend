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

import com.github.llamara.ai.internal.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link FSFileStorageImpl}. */
@QuarkusTest
class FSFileStorageImplTest {
    private static final Path FILE = Path.of("src/test/resources/llamara.txt");
    private static final String FILE_CHECKSUM;

    static {
        try {
            FILE_CHECKSUM = Utils.generateChecksum(FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject io.vertx.core.Vertx vertx;
    @Inject io.vertx.mutiny.core.Vertx reactiveVertx;

    String storagePath;
    FileStorage fileStorage;

    @BeforeEach
    void setup() {
        storagePath = "test_" + UUID.randomUUID() + "_knowledge";
        fileStorage = new FSFileStorageImpl(storagePath, vertx, reactiveVertx);
    }

    @AfterEach
    void destroy() {
        fileStorage = null;
        if (vertx.fileSystem().existsBlocking(storagePath)) {
            vertx.fileSystem().deleteRecursiveBlocking(storagePath, true);
        }
    }

    @Test
    void fileStorageCreatesStorageDirIfItDoesNotExist() {
        assertTrue(vertx.fileSystem().existsBlocking(storagePath));
    }

    @Test
    void storeFileStoresFile() {
        assertDoesNotThrow(
                () -> fileStorage.storeFile(FILE_CHECKSUM, FILE, Collections.emptyMap()));
        assertTrue(
                vertx.fileSystem().existsBlocking(Path.of(storagePath, FILE_CHECKSUM).toString()));
    }

    @Test
    void storeFileThrowsExceptionIfFileDoesNotExist() {
        assertThrows(
                UnexpectedFileStorageFailureException.class,
                () ->
                        fileStorage.storeFile(
                                FILE_CHECKSUM, Path.of("nonexistent"), Collections.emptyMap()));
    }

    @Test
    void storeFileThrowsExceptionIfStorageDirHasBeenRemoved() {
        // when
        vertx.fileSystem().deleteRecursiveBlocking(storagePath, true);

        // then
        assertThrows(
                UnexpectedFileStorageFailureException.class,
                () -> fileStorage.storeFile(FILE_CHECKSUM, FILE, Collections.emptyMap()));
    }

    @Test
    void getFileThrowsFileNotFoundExceptionIfFileDoesNotExist() {
        assertThrows(FileNotFoundException.class, () -> fileStorage.getFile("nonexistent"));
    }

    @Test
    void getFileReturnsFileContainer() throws UnexpectedFileStorageFailureException, IOException {
        // setup
        fileStorage.storeFile(FILE_CHECKSUM, FILE, Collections.emptyMap());

        // when
        FileContainer fileContainer = assertDoesNotThrow(() -> fileStorage.getFile(FILE_CHECKSUM));

        // when
        assertTrue(fileContainer.content().readAllBytes().length > 0);
    }

    @Test
    void deleteFileDeletesFile()
            throws UnexpectedFileStorageFailureException, InterruptedException {
        // setup
        fileStorage.storeFile(FILE_CHECKSUM, FILE, Collections.emptyMap());

        // when
        assertDoesNotThrow(() -> fileStorage.deleteFile(FILE_CHECKSUM));
        Thread.sleep(500); // NOSONAR: give the async delete some time to complete

        // then
        assertFalse(
                vertx.fileSystem().existsBlocking(Path.of(storagePath, FILE_CHECKSUM).toString()));
    }

    @Test
    void deleteAllFilesDeletesAllFiles()
            throws UnexpectedFileStorageFailureException, InterruptedException {
        // setup
        fileStorage.storeFile(FILE_CHECKSUM, FILE, Collections.emptyMap());

        // when
        assertDoesNotThrow(() -> fileStorage.deleteAllFiles());
        Thread.sleep(500); // NOSONAR: give the async delete some time to complete

        // then
        assertTrue(vertx.fileSystem().existsBlocking(storagePath));
        assertFalse(
                vertx.fileSystem().existsBlocking(Path.of(storagePath, FILE_CHECKSUM).toString()));
    }
}
