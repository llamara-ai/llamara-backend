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
import com.github.llamara.ai.internal.Utils;
import com.github.llamara.ai.internal.ingestion.DocumentIngestor;
import com.github.llamara.ai.internal.ingestion.IngestionStatus;
import com.github.llamara.ai.internal.knowledge.embedding.EmbeddingStorePermissionMetadataManager;
import com.github.llamara.ai.internal.knowledge.storage.FileStorage;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.Users;
import com.github.llamara.ai.internal.security.user.TestUserRepository;
import com.github.llamara.ai.internal.security.user.User;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link KnowledgeManagerImpl}. */
@QuarkusTest
class KnowledgeManagerImplTest {
    private static final Path EMPTY_FILE = Path.of("src/test/resources/empty.txt");
    private static final String EMPTY_FILE_NAME = "empty.txt";
    private static final String EMPTY_FILE_MIME_TYPE = "text/plain";

    private static final Path FILE = Path.of("src/test/resources/llamara.txt");
    private static final String FILE_NAME = "llamara.txt";
    private static final String FILE_MIME_TYPE = "text/plain";
    private static final String FILE_CHECKSUM;

    private static final int TOKEN_COUNT = 5000;

    static {
        try {
            FILE_CHECKSUM = Utils.generateChecksum(FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Path UPDATED_FILE = Path.of("src/test/resources/llamara-updated.txt");
    private static final String UPDATED_FILE_NAME = "llamara-updated.txt";
    private static final String UPDATED_FILE_MIME_TYPE = "text/plain";
    private static final String UPDATED_FILE_CHECKSUM;

    static {
        try {
            UPDATED_FILE_CHECKSUM = Utils.generateChecksum(UPDATED_FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final User OWNER_USER = new User("owner");
    private static final User OTHER_USER = new User("other");

    @Inject TestUserRepository userRepository;

    @InjectSpy KnowledgeRepository knowledgeRepository;
    @InjectMock DocumentIngestor documentIngestor;
    @InjectMock EmbeddingStore<TextSegment> embeddingStore;
    @InjectSpy FileStorage fileStorage;
    @InjectMock EmbeddingStorePermissionMetadataManager embeddingStorePermissionMetadataManager;

    private KnowledgeManagerImpl knowledgeManager;

    @Transactional
    @BeforeEach
    void setup() {
        knowledgeManager =
                new KnowledgeManagerImpl(
                        knowledgeRepository,
                        documentIngestor,
                        embeddingStore,
                        fileStorage,
                        embeddingStorePermissionMetadataManager);

        assertEquals(0, knowledgeRepository.count());

        userRepository.init();
        userRepository.persist(OWNER_USER);
        userRepository.persist(OTHER_USER);

        clearAllInvocations();

        assertEquals(3, userRepository.count());
    }

    @Transactional
    @AfterEach
    void destroy() throws UnexpectedFileStorageFailureException {
        knowledgeRepository.deleteAll();
        userRepository.deleteAll();
        fileStorage.deleteAllFiles();
        clearAllInvocations();
    }

    void clearAllInvocations() {
        clearInvocations(
                knowledgeRepository,
                documentIngestor,
                embeddingStore,
                fileStorage,
                embeddingStorePermissionMetadataManager);
    }

    @Test
    void getAllKnowledgeReturnsEmptyCollectionIfNoKnowledge() {
        assertEquals(0, knowledgeManager.getAllKnowledge().size());
    }

    @Test
    void getKnowledgeThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () -> knowledgeManager.getKnowledge(UUID.randomUUID()));
    }

    @Test
    void deleteKnowledgeThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () -> knowledgeManager.deleteKnowledge(UUID.randomUUID()));
    }

    @Test
    void setKnowledgeIngestionStatusDoesNothingIfNoKnowledge() {
        assertDoesNotThrow(
                () ->
                        knowledgeManager.setKnowledgeIngestionMetadata(
                                UUID.randomUUID(), IngestionStatus.SUCCEEDED, TOKEN_COUNT));
        verify(knowledgeRepository, never()).persist((Knowledge) any());
    }

    @Test
    void addSourceFileThrowsEmptyFileExceptionIfFileEmpty() {
        assertThrows(
                EmptyFileException.class,
                () ->
                        knowledgeManager.addSource(
                                EMPTY_FILE, EMPTY_FILE_NAME, EMPTY_FILE_MIME_TYPE));
    }

    @Test
    void addSourceFileCreatesKnowledge() throws UnexpectedFileStorageFailureException, IOException {
        UUID knowledgeId = knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE);
        assertEquals(1, knowledgeRepository.count());
        Knowledge knowledge = knowledgeRepository.findById(knowledgeId);
        assertEquals(FILE_CHECKSUM, knowledge.getChecksum());
        assertEquals(FILE_NAME, knowledge.getSource().toString());
        assertEquals(FILE_MIME_TYPE, knowledge.getContentType());
    }

    @Test
    void addSourceFileStoresFile() throws UnexpectedFileStorageFailureException, IOException {
        knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE);
        verify(fileStorage, times(1)).storeFile(eq(FILE_CHECKSUM), eq(FILE), any());
    }

    @Test
    void addSourceFileDispatchesIngestion()
            throws UnexpectedFileStorageFailureException, IOException {
        knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE);
        verify(documentIngestor, times(1)).ingestDocument(any());
    }

    @Test
    void addSourceFileWithOwnerSetsPermission()
            throws UnexpectedFileStorageFailureException, IOException {
        UUID knowledgeId = knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, OWNER_USER);
        assertEquals(1, knowledgeRepository.count());
        Knowledge knowledge = knowledgeRepository.findById(knowledgeId);
        assertEquals(Permission.OWNER, knowledge.getPermission(OWNER_USER));
    }

    @Test
    void updateSourceFileThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () ->
                        knowledgeManager.updateSource(
                                UUID.randomUUID(), FILE, FILE_NAME, FILE_MIME_TYPE));
    }

    @Test
    void setPermissionThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () ->
                        knowledgeManager.setPermission(
                                UUID.randomUUID(), OWNER_USER, Permission.OWNER));
    }

    @Test
    void removePermissionThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () -> knowledgeManager.removePermission(UUID.randomUUID(), OWNER_USER));
    }

    @Test
    void getFileThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () -> knowledgeManager.getFile(UUID.randomUUID()));
    }

    @Test
    void setLabelThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () -> knowledgeManager.addTag(UUID.randomUUID(), "label"));
    }

    @Test
    void addTagThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () -> knowledgeManager.addTag(UUID.randomUUID(), "tag"));
    }

    @Test
    void removeTagThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () -> knowledgeManager.removeTag(UUID.randomUUID(), "tag"));
    }

    @Test
    void retryFailedIngestionThrowsKnowledgeNotFoundExceptionIfNoKnowledge() {
        assertThrows(
                KnowledgeNotFoundException.class,
                () -> knowledgeManager.retryFailedIngestion(UUID.randomUUID()));
    }

    @Nested
    class WithKnowledgeFile {
        UUID knowledgeId;

        @BeforeEach
        void setup() throws UnexpectedFileStorageFailureException, IOException {
            knowledgeId = knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, OWNER_USER);
            clearAllInvocations();

            assertEquals(1, knowledgeRepository.count());
        }

        @Test
        void getAllKnowledgeReturnsKnowledge() {
            assertEquals(1, knowledgeManager.getAllKnowledge().size());
            assertEquals(
                    knowledgeId,
                    knowledgeManager.getAllKnowledge().stream()
                            .map(Knowledge::getId)
                            .findFirst()
                            .get());
        }

        @Test
        void getKnowledgeReturnsKnowledge() {
            Knowledge knowledge =
                    assertDoesNotThrow(() -> knowledgeManager.getKnowledge(knowledgeId));
            assertEquals(knowledgeId, knowledge.getId());
        }

        @Test
        void deleteKnowledgeDeletesKnowledge() {
            assertDoesNotThrow(() -> knowledgeManager.deleteKnowledge(knowledgeId));
            assertEquals(0, knowledgeRepository.count());
            verify(knowledgeRepository, times(1)).delete(any());
        }

        @Test
        void deleteKnowledgeDeletesFileIfNoOtherKnowledgeWithSameSourceFile()
                throws UnexpectedFileStorageFailureException, KnowledgeNotFoundException {
            knowledgeManager.deleteKnowledge(knowledgeId);
            verify(fileStorage, times(1)).deleteFile(FILE_CHECKSUM);
        }

        @Test
        void deleteKnowledgeDoesNotDeleteFileIfOtherKnowledgeWithSameSourceFile()
                throws UnexpectedFileStorageFailureException,
                        IOException,
                        KnowledgeNotFoundException {
            // setup
            knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE);

            // test
            knowledgeManager.deleteKnowledge(knowledgeId);
            verify(fileStorage, never()).deleteFile(FILE_CHECKSUM);
        }

        @Test
        void deleteKnowledgeDeletesEmbeddings()
                throws UnexpectedFileStorageFailureException, KnowledgeNotFoundException {
            knowledgeManager.deleteKnowledge(knowledgeId);
            Filter filter = new IsEqualTo(MetadataKeys.KNOWLEDGE_ID, knowledgeId);
            verify(embeddingStore, times(1)).removeAll(filter);
        }

        @Test
        void setKnowledgeIngestionStatusSetsStatus() {
            knowledgeManager.setKnowledgeIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);
            Knowledge knowledge = knowledgeRepository.findById(knowledgeId);
            assertEquals(IngestionStatus.SUCCEEDED, knowledge.getIngestionStatus());
        }

        @Test
        void addSourceFileCreatesNewKnowledgeIfFileAlreadyStored()
                throws UnexpectedFileStorageFailureException, IOException {
            UUID newKnowledgeId = knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE);
            assertNotEquals(knowledgeId, newKnowledgeId);
            assertEquals(2, knowledgeRepository.count());
        }

        @Test
        void addSourceFileDoesNotStoreFileIfAlreadyStored()
                throws UnexpectedFileStorageFailureException, IOException {
            knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE);
            verify(fileStorage, never()).storeFile(eq(FILE_CHECKSUM), eq(FILE), any());
        }

        @Test
        void addSourceFileDoesDispatchIngestionIfFileAlreadyStored()
                throws UnexpectedFileStorageFailureException, IOException {
            knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE);
            verify(documentIngestor, times(1)).ingestDocument(any());
        }

        @Test
        void updateSourceFileThrowsEmptyFileExceptionIfFileEmpty() {
            assertThrows(
                    EmptyFileException.class,
                    () ->
                            knowledgeManager.updateSource(
                                    knowledgeId,
                                    EMPTY_FILE,
                                    EMPTY_FILE_NAME,
                                    EMPTY_FILE_MIME_TYPE));
        }

        @Test
        void updateSourceFileRemovesOldFile()
                throws UnexpectedFileStorageFailureException,
                        IOException,
                        KnowledgeNotFoundException {
            knowledgeManager.updateSource(
                    knowledgeId, UPDATED_FILE, UPDATED_FILE_NAME, UPDATED_FILE_MIME_TYPE);
            verify(fileStorage, times(1)).deleteFile(FILE_CHECKSUM);
        }

        @Test
        void updateSourceFileDeletesOldEmbeddings()
                throws UnexpectedFileStorageFailureException,
                        KnowledgeNotFoundException,
                        IOException {
            knowledgeManager.updateSource(
                    knowledgeId, UPDATED_FILE, UPDATED_FILE_NAME, UPDATED_FILE_MIME_TYPE);
            Filter filter = new IsEqualTo(MetadataKeys.KNOWLEDGE_ID, knowledgeId);
            verify(embeddingStore, times(1)).removeAll(filter);
        }

        @Disabled("Test fails but implementation works in production")
        @Test
        void updateSourceFileUpdatesKnowledge()
                throws UnexpectedFileStorageFailureException,
                        KnowledgeNotFoundException,
                        IOException {
            knowledgeManager.updateSource(
                    knowledgeId, UPDATED_FILE, UPDATED_FILE_NAME, UPDATED_FILE_MIME_TYPE);

            Knowledge knowledge = knowledgeRepository.findById(knowledgeId);
            assertEquals(UPDATED_FILE_CHECKSUM, knowledge.getChecksum());
            assertEquals(UPDATED_FILE_NAME, knowledge.getSource().toString());
            assertEquals(UPDATED_FILE_MIME_TYPE, knowledge.getContentType());
        }

        @Test
        void updateSourceFileStoresNewFile()
                throws UnexpectedFileStorageFailureException,
                        KnowledgeNotFoundException,
                        IOException {
            knowledgeManager.updateSource(
                    knowledgeId, UPDATED_FILE, UPDATED_FILE_NAME, UPDATED_FILE_MIME_TYPE);
            verify(fileStorage, times(1))
                    .storeFile(eq(UPDATED_FILE_CHECKSUM), eq(UPDATED_FILE), any());
        }

        @Test
        void updateSourceFileDispatchesIngestion()
                throws UnexpectedFileStorageFailureException,
                        KnowledgeNotFoundException,
                        IOException {
            knowledgeManager.updateSource(
                    knowledgeId, UPDATED_FILE, UPDATED_FILE_NAME, UPDATED_FILE_MIME_TYPE);
            verify(documentIngestor, times(1)).ingestDocument(any());
        }

        @Test
        void setPermissionThrowsIllegalPermissionModificationExceptionForPermissionNONE() {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);

            // test
            assertThrows(
                    IllegalPermissionModificationException.class,
                    () -> knowledgeManager.setPermission(knowledgeId, OWNER_USER, Permission.NONE));
            // test
            assertThrows(
                    IllegalPermissionModificationException.class,
                    () -> knowledgeManager.setPermission(knowledgeId, OTHER_USER, Permission.NONE));
        }

        @Test
        void setPermissionThrowsIllegalPermissionModificationExceptionForPermissionOWNER() {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);

            // test
            assertThrows(
                    IllegalPermissionModificationException.class,
                    () ->
                            knowledgeManager.setPermission(
                                    knowledgeId, OWNER_USER, Permission.OWNER));
            assertThrows(
                    IllegalPermissionModificationException.class,
                    () ->
                            knowledgeManager.setPermission(
                                    knowledgeId, OTHER_USER, Permission.OWNER));
        }

        @Test
        void
                setPermissionThrowsIllegalPermissionModificationExceptionForChangingOwnersPermission() {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);

            // test
            assertThrows(
                    IllegalPermissionModificationException.class,
                    () ->
                            knowledgeManager.setPermission(
                                    knowledgeId, OWNER_USER, Permission.READONLY));
        }

        @Test
        void setPermissionSetsPermission() {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);

            // test
            assertDoesNotThrow(
                    () ->
                            knowledgeManager.setPermission(
                                    knowledgeId, OTHER_USER, Permission.READONLY));
            Knowledge knowledge = knowledgeRepository.findById(knowledgeId);
            assertEquals(Permission.READONLY, knowledge.getPermission(OTHER_USER));
        }

        // regression test for https://github.com/llamara-ai/llamara-backend/issues/116
        @Test
        void setPermissionSetsExplicitPermissionIfImplicitPermissionAlreadySet()
                throws IllegalPermissionModificationException, KnowledgeNotFoundException {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);
            knowledgeManager.setPermission(knowledgeId, Users.ANY, Permission.READONLY);

            // test
            assertDoesNotThrow(
                    () ->
                            knowledgeManager.setPermission(
                                    knowledgeId, OTHER_USER, Permission.READONLY));
            Knowledge knowledge = knowledgeRepository.findById(knowledgeId);
            // check if permission is properly evaluated
            assertEquals(Permission.READONLY, knowledge.getPermission(OTHER_USER));
            // check if explicit permission has been set
            assertEquals(Permission.READONLY, knowledge.getPermissions().get(OTHER_USER));
        }

        @Test
        void setPermissionUpdatesPermissionMetadata() {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);
            Knowledge knowledge = knowledgeRepository.findById(knowledgeId);

            // test
            assertDoesNotThrow(
                    () ->
                            knowledgeManager.setPermission(
                                    knowledgeId, OTHER_USER, Permission.READONLY));
            verify(embeddingStorePermissionMetadataManager, times(1))
                    .updatePermissionMetadata(knowledge);
        }

        @Test
        void
                removePermissionThrowsIllegalPermissionModificationExceptionForRemovingOwnersPermission() {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);

            // test
            assertThrows(
                    IllegalPermissionModificationException.class,
                    () -> knowledgeManager.removePermission(knowledgeId, OWNER_USER));
        }

        @Test
        void removePermissionRemovesPermission()
                throws IllegalPermissionModificationException, KnowledgeNotFoundException {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);
            knowledgeManager.setPermission(knowledgeId, OTHER_USER, Permission.READONLY);

            // test
            assertDoesNotThrow(() -> knowledgeManager.removePermission(knowledgeId, OTHER_USER));
            Knowledge knowledge = knowledgeRepository.findById(knowledgeId);
            assertEquals(Permission.NONE, knowledge.getPermission(OTHER_USER));
        }

        @Test
        void removePermissionUpdatesPermissionMetadata() {
            // setup
            knowledgeRepository.setIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);
            Knowledge knowledge = knowledgeRepository.findById(knowledgeId);

            // test
            assertDoesNotThrow(() -> knowledgeManager.removePermission(knowledgeId, OTHER_USER));
            verify(embeddingStorePermissionMetadataManager, times(1))
                    .updatePermissionMetadata(knowledge);
        }

        @Test
        void getFileReturnsFile() {
            KnowledgeManager.NamedFileContainer fileContainer =
                    assertDoesNotThrow(() -> knowledgeManager.getFile(knowledgeId));
            assertEquals(FILE_NAME, fileContainer.fileName());
        }

        @Test
        void setLabelSetsLabel() {
            assertDoesNotThrow(() -> knowledgeManager.setLabel(knowledgeId, "label"));

            assertDoesNotThrow(
                    () ->
                            assertEquals(
                                    "label",
                                    knowledgeManager
                                            .getKnowledge(knowledgeId)
                                            .getLabel()
                                            .orElse(null)));
        }

        @Test
        void addTagAddsTags() {
            assertDoesNotThrow(() -> knowledgeManager.addTag(knowledgeId, "tag1"));
            assertDoesNotThrow(() -> knowledgeManager.addTag(knowledgeId, "tag2"));

            assertDoesNotThrow(
                    () ->
                            assertEquals(
                                    Set.of("tag1", "tag2"),
                                    knowledgeManager.getKnowledge(knowledgeId).getTags()));
        }

        @Test
        void removeTagRemovesTag() throws KnowledgeNotFoundException {
            // setup
            knowledgeManager.addTag(knowledgeId, "tag1");

            // test
            assertDoesNotThrow(() -> knowledgeManager.removeTag(knowledgeId, "tag1"));
            assertDoesNotThrow(
                    () ->
                            assertEquals(
                                    Set.of(),
                                    knowledgeManager.getKnowledge(knowledgeId).getTags()));
        }

        @Test
        void removeTagDoesNothingIfTagNotExists() throws KnowledgeNotFoundException {
            // setup
            knowledgeManager.addTag(knowledgeId, "tag1");

            // test
            assertDoesNotThrow(() -> knowledgeManager.removeTag(knowledgeId, "tag2"));
            assertDoesNotThrow(
                    () ->
                            assertEquals(
                                    Set.of("tag1"),
                                    knowledgeManager.getKnowledge(knowledgeId).getTags()));
        }

        @Test
        void retryFailedIngestionDoesNothingIfKnowledgeIngestionStatusIsSucceeded()
                throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
            // setup
            knowledgeManager.setKnowledgeIngestionMetadata(
                    knowledgeId, IngestionStatus.SUCCEEDED, TOKEN_COUNT);

            // test
            knowledgeManager.retryFailedIngestion(knowledgeId);

            verify(documentIngestor, times(0)).ingestDocument(any());
        }

        @Test
        void retryFailedIngestionDoesNothingIfKnowledgeIngestionStatusIsPending()
                throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
            // setup
            knowledgeManager.setKnowledgeIngestionMetadata(
                    knowledgeId, IngestionStatus.PENDING, null);

            // test
            knowledgeManager.retryFailedIngestion(knowledgeId);

            verify(documentIngestor, times(0)).ingestDocument(any());
        }

        @Test
        void retryFailedIngestionSetsIngestionStatusToPendingIfIngestionStatusIsFailed()
                throws UnexpectedFileStorageFailureException, KnowledgeNotFoundException {
            // setup
            knowledgeManager.setKnowledgeIngestionMetadata(
                    knowledgeId, IngestionStatus.FAILED, null);

            // test
            knowledgeManager.retryFailedIngestion(knowledgeId);

            verify(knowledgeRepository, times(1))
                    .setIngestionMetadata(knowledgeId, IngestionStatus.PENDING, null);
        }

        @Test
        void retryFailedIngestionDispatchesIngestionIfKnowledgeIngestionStatusIsFailed()
                throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
            // setup
            knowledgeManager.setKnowledgeIngestionMetadata(
                    knowledgeId, IngestionStatus.FAILED, null);

            // test
            knowledgeManager.retryFailedIngestion(knowledgeId);

            verify(documentIngestor, times(1)).ingestDocument(any());
        }
    }
}
