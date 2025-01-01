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
package com.github.llamara.ai.internal.internal.security.knowledge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.llamara.ai.internal.internal.Utils;
import com.github.llamara.ai.internal.internal.ingestion.DocumentIngestor;
import com.github.llamara.ai.internal.internal.ingestion.IngestionStatus;
import com.github.llamara.ai.internal.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeRepository;
import com.github.llamara.ai.internal.internal.knowledge.TestKnowledgeManagerImpl;
import com.github.llamara.ai.internal.internal.knowledge.embedding.EmbeddingStorePermissionMetadataManager;
import com.github.llamara.ai.internal.internal.knowledge.storage.FileStorage;
import com.github.llamara.ai.internal.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.internal.security.Permission;
import com.github.llamara.ai.internal.internal.security.session.UserSessionManager;
import com.github.llamara.ai.internal.internal.security.user.TestUserRepository;
import com.github.llamara.ai.internal.internal.security.user.User;
import com.github.llamara.ai.internal.internal.security.user.UserNotRegisteredException;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Test for {@link UserKnowledgeManagerImpl}. */
@QuarkusTest
class UserKnowledgeManagerImplTest {
    private static final Path FILE = Path.of("src/test/resources/llamara.txt");
    private static final String FILE_NAME = "llamara.txt";
    private static final String FILE_MIME_TYPE = "text/plain";
    private static final String
            FILE_CHECKSUM; // NOSONAR: ignore this unused static field as this is a test class

    static {
        try {
            FILE_CHECKSUM = Utils.generateChecksum(FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final User OWN_USER = new User("own");
    private static final User FOREGIN_USER = new User("foreign");

    @Inject TestUserRepository userRepository;

    // for TestKnowledgeManagerImpl
    @InjectSpy KnowledgeRepository knowledgeRepository;
    @InjectMock DocumentIngestor documentIngestor;
    @InjectMock EmbeddingStore<TextSegment> embeddingStore;
    @InjectSpy FileStorage fileStorage;
    @InjectMock EmbeddingStorePermissionMetadataManager embeddingStorePermissionMetadataManager;

    @InjectSpy UserAwareKnowledgeRepository userAwareKnowledgeRepository;
    @InjectMock UserSessionManager userSessionManager;

    @InjectMock SecurityIdentity identity;

    private TestKnowledgeManagerImpl knowledgeManager;
    private UserKnowledgeManagerImpl userKnowledgeManager;

    @Transactional
    @BeforeEach
    void setup() {
        knowledgeManager =
                spy(
                        new TestKnowledgeManagerImpl(
                                knowledgeRepository,
                                documentIngestor,
                                embeddingStore,
                                fileStorage,
                                embeddingStorePermissionMetadataManager));
        userKnowledgeManager =
                new UserKnowledgeManagerImpl(
                        knowledgeManager,
                        userAwareKnowledgeRepository,
                        userSessionManager,
                        identity);

        userRepository.init();
        userRepository.persist(OWN_USER);
        userRepository.persist(FOREGIN_USER);

        clearAllInvocations();

        doThrow(UserNotRegisteredException.class).when(userSessionManager).enforceRegistered();

        assertEquals(0, knowledgeRepository.count());
        assertEquals(3, userRepository.count());
    }

    @Transactional
    @AfterEach
    void destroy() {
        knowledgeRepository.deleteAll();
        userRepository.deleteAll();
        // TODO: Clean up file storage?
        clearAllInvocations();
    }

    /**
     * Set up the {@link SecurityIdentity} mock to return the given {@link User}.
     *
     * @param user the user
     */
    void setupIdentity(User user) {
        when(identity.getPrincipal()).thenReturn(user::getUsername);
    }

    void clearAllInvocations() {
        clearInvocations(knowledgeManager, userAwareKnowledgeRepository, userSessionManager);
    }

    @Nested
    class WithOwnAndForeignKnowledge {
        UUID ownKnowledgeId;
        UUID foreignKnowledgeId;

        @BeforeEach
        void setup() throws UnexpectedFileStorageFailureException, IOException {
            ownKnowledgeId = knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, OWN_USER);
            foreignKnowledgeId =
                    knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, FOREGIN_USER);
            setupIdentity(OWN_USER);

            doNothing().when(userSessionManager).enforceRegistered();
            clearAllInvocations();

            assertEquals(2, knowledgeRepository.count());
        }

        @Test
        void getAllKnowledgeOnlyReturnsOwnKnowledge() {
            assertEquals(1, userKnowledgeManager.getAllKnowledge().size());
            assertEquals(
                    ownKnowledgeId,
                    knowledgeManager.getAllKnowledge().stream()
                            .map(Knowledge::getId)
                            .findFirst()
                            .get());
        }

        @Test
        void getKnowledgeThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.getKnowledge(foreignKnowledgeId));
        }

        @Test
        void getKnowledgeReturnsOwnKnowledge() throws KnowledgeNotFoundException {
            Knowledge knowledge = userKnowledgeManager.getKnowledge(ownKnowledgeId);

            assertEquals(ownKnowledgeId, knowledge.getId());
        }

        @Test
        void deleteKnowledgeThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.deleteKnowledge(foreignKnowledgeId));
        }

        @Test
        void deleteKnowledgeDeletesOwnKnowledge()
                throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
            userKnowledgeManager.deleteKnowledge(ownKnowledgeId);
            verify(knowledgeManager, times(1)).deleteKnowledge(ownKnowledgeId);
        }

        @Test
        void addSourceFileReturnsExistingKnowledgeIfFileAlreadyExists()
                throws IOException, UnexpectedFileStorageFailureException {
            UUID existingKnowledgeId =
                    userKnowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE);
            assertEquals(ownKnowledgeId, existingKnowledgeId);
        }

        @Test
        void updateSourceFileThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () ->
                            userKnowledgeManager.updateSource(
                                    foreignKnowledgeId, FILE, FILE_NAME, FILE_MIME_TYPE));
        }

        @Test
        void updateSourceFileUpdatesOwnKnowledge()
                throws IOException,
                        KnowledgeNotFoundException,
                        UnexpectedFileStorageFailureException {
            userKnowledgeManager.updateSource(ownKnowledgeId, FILE, FILE_NAME, FILE_MIME_TYPE);
            verify(knowledgeManager, times(1))
                    .updateSource(ownKnowledgeId, FILE, FILE_NAME, FILE_MIME_TYPE);
        }

        @Test
        void setPermissionThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            // setup
            knowledgeRepository.setStatusFor(ownKnowledgeId, IngestionStatus.SUCCEEDED);

            // test
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () ->
                            userKnowledgeManager.setPermission(
                                    foreignKnowledgeId, FOREGIN_USER, Permission.READONLY));
        }

        @Test
        void setPermissionSetsPermissionForOwnKnowledge()
                throws KnowledgeNotFoundException, IllegalPermissionModificationException {
            // setup
            knowledgeRepository.setStatusFor(ownKnowledgeId, IngestionStatus.SUCCEEDED);

            // test
            userKnowledgeManager.setPermission(ownKnowledgeId, FOREGIN_USER, Permission.READONLY);
            verify(knowledgeManager, times(1))
                    .setPermission(ownKnowledgeId, FOREGIN_USER, Permission.READONLY);
        }

        @Test
        void removePermissionThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            // setup
            knowledgeRepository.setStatusFor(ownKnowledgeId, IngestionStatus.SUCCEEDED);

            // test
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.removePermission(foreignKnowledgeId, FOREGIN_USER));
        }

        @Test
        void removePermissionRemovesPermissionForOwnKnowledge()
                throws KnowledgeNotFoundException, IllegalPermissionModificationException {
            // setup
            knowledgeRepository.setStatusFor(ownKnowledgeId, IngestionStatus.SUCCEEDED);

            // test
            userKnowledgeManager.removePermission(ownKnowledgeId, FOREGIN_USER);
            verify(knowledgeManager, times(1)).removePermission(ownKnowledgeId, FOREGIN_USER);
        }

        @Test
        void getForeignKnowledgeThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.getFile(foreignKnowledgeId));
        }

        @Test
        void getFileReturnsOwnKnowledge()
                throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
            userKnowledgeManager.getFile(ownKnowledgeId);
            verify(knowledgeManager, times(1)).getFile(ownKnowledgeId);
        }
    }

    @Nested
    class WithSharedReadOnlyKnowledge {
        UUID roKnowledgeId;

        @BeforeEach
        void setup()
                throws UnexpectedFileStorageFailureException,
                        IOException,
                        IllegalPermissionModificationException,
                        KnowledgeNotFoundException {
            roKnowledgeId =
                    knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, FOREGIN_USER);
            knowledgeManager.setKnowledgeIngestionStatus(roKnowledgeId, IngestionStatus.SUCCEEDED);
            knowledgeManager.setPermission(roKnowledgeId, OWN_USER, Permission.READONLY);
            setupIdentity(OWN_USER);

            doNothing().when(userSessionManager).enforceRegistered();
            clearAllInvocations();

            assertEquals(1, knowledgeRepository.count());
        }

        @Test
        void deleteKnowledgeThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.deleteKnowledge(roKnowledgeId));
        }

        @Test
        void updateSourceFileThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () ->
                            userKnowledgeManager.updateSource(
                                    roKnowledgeId, FILE, FILE_NAME, FILE_MIME_TYPE));
        }

        @Test
        void setPermissionThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () ->
                            userKnowledgeManager.setPermission(
                                    roKnowledgeId, OWN_USER, Permission.READWRITE));
            assertThrows(
                    ForbiddenException.class,
                    () ->
                            userKnowledgeManager.setPermission(
                                    roKnowledgeId, FOREGIN_USER, Permission.READWRITE));
        }

        @Test
        void removePermissionThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.removePermission(roKnowledgeId, OWN_USER));
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.removePermission(roKnowledgeId, FOREGIN_USER));
        }
    }
}
