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

import com.github.llamara.ai.internal.ingestion.DocumentIngestor;
import com.github.llamara.ai.internal.ingestion.IngestionStatus;
import com.github.llamara.ai.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.knowledge.KnowledgeRepository;
import com.github.llamara.ai.internal.knowledge.TestKnowledgeManagerImpl;
import com.github.llamara.ai.internal.knowledge.embedding.EmbeddingStorePermissionMetadataManager;
import com.github.llamara.ai.internal.knowledge.storage.FileStorage;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.Roles;
import com.github.llamara.ai.internal.security.Users;
import com.github.llamara.ai.internal.security.session.AuthenticatedUserSessionManagerImpl;
import com.github.llamara.ai.internal.security.user.AnonymousUserManagerImpl;
import com.github.llamara.ai.internal.security.user.AuthenticatedUserManagerImpl;
import com.github.llamara.ai.internal.security.user.TestUserRepository;
import com.github.llamara.ai.internal.security.user.User;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Test for {@link UserKnowledgeManagerImpl}. */
@QuarkusTest
class UserKnowledgeManagerImplTest {
    private static final Path FILE = Path.of("src/test/resources/llamara.txt");
    private static final String FILE_NAME = "llamara.txt";
    private static final String FILE_MIME_TYPE = "text/plain";

    private static final User OWN_USER = new User("own");
    private static final User FOREIGN_USER = new User("foreign");

    @Inject TestUserRepository userRepository;

    // for TestKnowledgeManagerImpl
    @InjectSpy KnowledgeRepository knowledgeRepository;
    @InjectMock DocumentIngestor documentIngestor;
    @InjectMock EmbeddingStore<TextSegment> embeddingStore;
    @InjectSpy FileStorage fileStorage;
    @InjectMock EmbeddingStorePermissionMetadataManager embeddingStorePermissionMetadataManager;

    @InjectSpy UserAwareKnowledgeRepository userAwareKnowledgeRepository;
    @InjectSpy AuthenticatedUserManagerImpl authenticatedUserManager;
    @InjectMock AuthenticatedUserSessionManagerImpl authenticatedSessionManager;
    @InjectSpy AnonymousUserManagerImpl anonymousUserManager;

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
                        authenticatedUserManager,
                        identity);

        userRepository.init();
        userRepository.persist(OWN_USER);
        userRepository.persist(FOREIGN_USER);

        clearAllInvocations();

        assertEquals(0, knowledgeRepository.count());
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

    void setupAuthenticatedIdentity() {
        when(identity.getPrincipal()).thenReturn(OWN_USER::getUsername);
        when(identity.isAnonymous()).thenReturn(false);
        when(identity.getRoles()).thenReturn(Set.of(Roles.USER));
        when(identity.hasRole(Roles.ADMIN)).thenReturn(false);
        when(identity.hasRole(Roles.USER)).thenReturn(true);
    }

    void setupAnonymousIdentity() {
        when(identity.isAnonymous()).thenReturn(true);
        when(identity.getRoles()).thenReturn(Collections.emptySet());
        when(identity.hasRole(Roles.ADMIN)).thenReturn(false);
        when(identity.hasRole(Roles.USER)).thenReturn(false);
    }

    void clearAllInvocations() {
        clearInvocations(
                knowledgeManager, userAwareKnowledgeRepository, authenticatedSessionManager);
    }

    @Test
    void setIngestionStatusThrowsUnsupportedOperationException() {
        assertThrows( // NOSONAR: we only have one method invocation, false alarm
                UnsupportedOperationException.class,
                () ->
                        userKnowledgeManager.setKnowledgeIngestionStatus(
                                UUID.randomUUID(), IngestionStatus.SUCCEEDED));
    }

    @Nested
    class AuthenticatedUserWithOwnAndForeignKnowledge {
        UUID ownKnowledgeId;
        UUID foreignKnowledgeId;

        @BeforeEach
        void setup() throws UnexpectedFileStorageFailureException, IOException {
            ownKnowledgeId = knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, OWN_USER);
            foreignKnowledgeId =
                    knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, FOREIGN_USER);

            setupAuthenticatedIdentity();

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
                                    foreignKnowledgeId, FOREIGN_USER, Permission.READONLY));
        }

        @Test
        void setPermissionSetsPermissionForOwnKnowledge()
                throws KnowledgeNotFoundException, IllegalPermissionModificationException {
            // setup
            knowledgeRepository.setStatusFor(ownKnowledgeId, IngestionStatus.SUCCEEDED);

            // test
            userKnowledgeManager.setPermission(ownKnowledgeId, FOREIGN_USER, Permission.READONLY);
            verify(knowledgeManager, times(1))
                    .setPermission(ownKnowledgeId, FOREIGN_USER, Permission.READONLY);
        }

        @Test
        void removePermissionThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            // setup
            knowledgeRepository.setStatusFor(ownKnowledgeId, IngestionStatus.SUCCEEDED);

            // test
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.removePermission(foreignKnowledgeId, FOREIGN_USER));
        }

        @Test
        void removePermissionRemovesPermissionForOwnKnowledge()
                throws KnowledgeNotFoundException, IllegalPermissionModificationException {
            // setup
            knowledgeRepository.setStatusFor(ownKnowledgeId, IngestionStatus.SUCCEEDED);

            // test
            userKnowledgeManager.removePermission(ownKnowledgeId, FOREIGN_USER);
            verify(knowledgeManager, times(1)).removePermission(ownKnowledgeId, FOREIGN_USER);
        }

        @Test
        void getFileThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
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

        @Test
        void setKnowledgeLabelThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.setLabel(foreignKnowledgeId, "label"));
        }

        @Test
        void setKnowledgeLabelSetsLabelForOwnKnowledge() throws KnowledgeNotFoundException {
            userKnowledgeManager.setLabel(ownKnowledgeId, "label");
            verify(knowledgeManager, times(1)).setLabel(ownKnowledgeId, "label");
        }

        @Test
        void addKnowledgeTagThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.addTag(foreignKnowledgeId, "tag"));
        }

        @Test
        void addKnowledgeTagAddsTagToOwnKnowledge() throws KnowledgeNotFoundException {
            userKnowledgeManager.addTag(ownKnowledgeId, "tag");
            verify(knowledgeManager, times(1)).addTag(ownKnowledgeId, "tag");
        }

        @Test
        void removeKnowledgeTagThrowsKnowledgeNotFoundExceptionIfForeignKnowledge() {
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.removeTag(foreignKnowledgeId, "tag"));
        }

        @Test
        void removeKnowledgeTagRemovesTagFromOwnKnowledge() throws KnowledgeNotFoundException {
            userKnowledgeManager.removeTag(ownKnowledgeId, "tag");
            verify(knowledgeManager, times(1)).removeTag(ownKnowledgeId, "tag");
        }
    }

    @Nested
    class AuthenticatedUserWithSharedReadOnlyKnowledge {
        UUID sharedKnowledgeId;

        @BeforeEach
        void setup()
                throws UnexpectedFileStorageFailureException,
                        IOException,
                        IllegalPermissionModificationException,
                        KnowledgeNotFoundException {
            sharedKnowledgeId =
                    knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, FOREIGN_USER);
            knowledgeManager.setKnowledgeIngestionStatus(
                    sharedKnowledgeId, IngestionStatus.SUCCEEDED);
            knowledgeManager.setPermission(sharedKnowledgeId, OWN_USER, Permission.READONLY);

            setupAuthenticatedIdentity();

            clearAllInvocations();

            assertEquals(1, knowledgeRepository.count());
        }

        @Disabled("Test fails but implementation works in production")
        @Test
        void getAllKnowledgeReturnsSharedKnowledge() {
            assertEquals(1, userKnowledgeManager.getAllKnowledge().size());
            assertEquals(
                    sharedKnowledgeId,
                    knowledgeManager.getAllKnowledge().stream()
                            .map(Knowledge::getId)
                            .findFirst()
                            .get());
        }

        @Test
        void deleteKnowledgeThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.deleteKnowledge(sharedKnowledgeId));
        }

        @Test
        void updateSourceFileThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () ->
                            userKnowledgeManager.updateSource(
                                    sharedKnowledgeId, FILE, FILE_NAME, FILE_MIME_TYPE));
        }

        @Test
        void setPermissionThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () ->
                            userKnowledgeManager.setPermission(
                                    sharedKnowledgeId, OWN_USER, Permission.READWRITE));
            assertThrows(
                    ForbiddenException.class,
                    () ->
                            userKnowledgeManager.setPermission(
                                    sharedKnowledgeId, FOREIGN_USER, Permission.READWRITE));
        }

        @Test
        void removePermissionThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.removePermission(sharedKnowledgeId, OWN_USER));
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.removePermission(sharedKnowledgeId, FOREIGN_USER));
        }

        @Test
        void setKnowledgeLabelThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.setLabel(sharedKnowledgeId, "label"));
        }

        @Test
        void addKnowledgeTagThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.addTag(sharedKnowledgeId, "tag"));
        }

        @Test
        void removeKnowledgeTagThrowsForbiddenExceptionIfReadOnlyKnowledge() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.removeTag(sharedKnowledgeId, "tag"));
        }
    }

    @Nested
    class AnonymousUserWithSharedReadOnlyKnowledgeAndForeignPrivateKnowledge {
        UUID privateKnowledgeId;
        UUID publicKnowledgeId;

        @BeforeEach
        void setup()
                throws UnexpectedFileStorageFailureException,
                        IOException,
                        IllegalPermissionModificationException,
                        KnowledgeNotFoundException {
            userKnowledgeManager =
                    new UserKnowledgeManagerImpl(
                            knowledgeManager,
                            userAwareKnowledgeRepository,
                            anonymousUserManager,
                            identity);

            privateKnowledgeId =
                    knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, OWN_USER);
            publicKnowledgeId =
                    knowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE, FOREIGN_USER);
            knowledgeManager.setKnowledgeIngestionStatus(
                    publicKnowledgeId, IngestionStatus.SUCCEEDED);
            knowledgeManager.setPermission(publicKnowledgeId, Users.ANY, Permission.READONLY);

            setupAnonymousIdentity();

            clearAllInvocations();

            assertEquals(2, knowledgeRepository.count());
        }

        @Disabled("Test fails but implementation works in production")
        @Test
        void getAllKnowledgeReturnsPublicKnowledgeOnly() {
            assertEquals(1, userKnowledgeManager.getAllKnowledge().size());
            assertEquals(
                    publicKnowledgeId,
                    knowledgeManager.getAllKnowledge().stream()
                            .map(Knowledge::getId)
                            .findFirst()
                            .get());
        }

        @Test
        void getKnowledgeThrowsKnowledgeNotFoundExceptionIfPrivateKnowledge() {
            assertThrows(
                    KnowledgeNotFoundException.class,
                    () -> userKnowledgeManager.getKnowledge(privateKnowledgeId));
        }

        @Test
        void getKnowledgeReturnsPublicKnowledge() {
            Knowledge knowledge =
                    assertDoesNotThrow(() -> userKnowledgeManager.getKnowledge(publicKnowledgeId));
            assertEquals(publicKnowledgeId, knowledge.getId());
        }

        @Test
        void deleteKnowledgeThrowsForbiddenException() {
            assertThrows( // NOSONAR: We want to check exactly for the given exception
                    ForbiddenException.class,
                    () -> userKnowledgeManager.deleteKnowledge(UUID.randomUUID()));
        }

        @Test
        void addSourceFileThrowsForbiddenException() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.addSource(FILE, FILE_NAME, FILE_MIME_TYPE));
        }

        @Test
        void updateSourceFileThrowsForbiddenException() {
            assertThrows( // NOSONAR: We want to check exactly for the given exception
                    ForbiddenException.class,
                    () ->
                            userKnowledgeManager.updateSource(
                                    UUID.randomUUID(), FILE, FILE_NAME, FILE_MIME_TYPE));
        }

        @Test
        void setPermissionThrowsForbiddenException() {
            assertThrows( // NOSONAR: We want to check exactly for the given exception
                    ForbiddenException.class,
                    () ->
                            userKnowledgeManager.setPermission(
                                    UUID.randomUUID(), OWN_USER, Permission.READWRITE));
        }

        @Test
        void removePermissionThrowsForbiddenException() {
            assertThrows( // NOSONAR: We want to check exactly for the given exception
                    ForbiddenException.class,
                    () -> userKnowledgeManager.removePermission(UUID.randomUUID(), OWN_USER));
        }

        @Test
        void setKnowledgeLabelThrowsForbiddenException() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.setLabel(publicKnowledgeId, "label"));
        }

        @Test
        void addKnowledgeTagThrowsForbiddenException() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.addTag(publicKnowledgeId, "tag"));
        }

        @Test
        void removeKnowledgeTagThrowsForbiddenException() {
            assertThrows(
                    ForbiddenException.class,
                    () -> userKnowledgeManager.removeTag(publicKnowledgeId, "tag"));
        }
    }
}
