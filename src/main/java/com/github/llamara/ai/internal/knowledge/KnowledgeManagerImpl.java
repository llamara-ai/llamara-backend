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

import static com.github.llamara.ai.internal.Utils.generateChecksum;

import com.github.llamara.ai.internal.CommonMetadataKeys;
import com.github.llamara.ai.internal.EmbeddingMetadataKeys;
import com.github.llamara.ai.internal.StartupException;
import com.github.llamara.ai.internal.Utils;
import com.github.llamara.ai.internal.ingestion.DocumentIngestor;
import com.github.llamara.ai.internal.ingestion.IngestionStatus;
import com.github.llamara.ai.internal.ingestion.parser.PdfDocumentParser;
import com.github.llamara.ai.internal.knowledge.embedding.EmbeddingStorePermissionMetadataManager;
import com.github.llamara.ai.internal.knowledge.persistence.FileKnowledge;
import com.github.llamara.ai.internal.knowledge.persistence.Knowledge;
import com.github.llamara.ai.internal.knowledge.storage.FileContainer;
import com.github.llamara.ai.internal.knowledge.storage.FileStorage;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.PermissionMetadataMapper;
import com.github.llamara.ai.internal.security.user.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static dev.langchain4j.data.document.Document.ABSOLUTE_DIRECTORY_PATH;
import static dev.langchain4j.data.document.Document.FILE_NAME;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.source.FileSystemSource;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.Startup;

/**
 * Implementation of the {@link KnowledgeManager} using {@link KnowledgeRepository} for storing
 * knowledge metadata, {@link EmbeddingStore} for storing embeddings and {@link FileStorage} for
 * storing uploaded files.
 *
 * @author Florian Hotze - Initial contribution
 */
@Startup
@ApplicationScoped
class KnowledgeManagerImpl implements KnowledgeManager {
    private static final String FILE_STORAGE_FILE_NOT_FOUND_PATTERN =
            "File for knowledge '%s' should exist in storage, but is missing";
    private static final Path TEMP_DIR = Path.of("/tmp/llamara/knowledge");

    private final DocumentIngestor ingestor;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final KnowledgeRepository repository;
    private final FileStorage fileStorage;
    private final EmbeddingStorePermissionMetadataManager embeddingStorePermissionMetadataManager;

    @Inject
    KnowledgeManagerImpl(
            KnowledgeRepository repository,
            DocumentIngestor ingestor,
            EmbeddingStore<TextSegment> embeddingStore,
            FileStorage fileStorage,
            EmbeddingStorePermissionMetadataManager embeddingStorePermissionMetadataManager) {
        this.repository = repository;
        this.ingestor = ingestor;
        this.embeddingStore = embeddingStore;
        this.fileStorage = fileStorage;
        this.embeddingStorePermissionMetadataManager = embeddingStorePermissionMetadataManager;
    }

    @PostConstruct
    void init() {
        try {
            if (Utils.isOsUnix()) {
                FileAttribute<Set<PosixFilePermission>> attr =
                        PosixFilePermissions.asFileAttribute(
                                PosixFilePermissions.fromString("rwx------"));
                Files.createDirectories(TEMP_DIR, attr);
            } else {
                File f = Files.createDirectories(TEMP_DIR).toFile();
                if (!f.setReadable(true, true)
                        || !f.setWritable(true, true)
                        || !f.setExecutable(true, true)) {
                    throw new StartupException("Failed to set permissions on temporary directory");
                }
            }
        } catch (IOException e) {
            throw new StartupException("Failed to create temporary directory", e);
        }
    }

    @Override
    public Collection<Knowledge> getAllKnowledge() {
        return repository.listAll();
    }

    @Override
    public Knowledge getKnowledge(UUID id) throws KnowledgeNotFoundException {
        Knowledge knowledge = repository.findById(id);
        if (knowledge == null) {
            throw new KnowledgeNotFoundException(id);
        }
        return knowledge;
    }

    @Override
    public void deleteKnowledge(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        Knowledge knowledge = getKnowledge(id);
        QuarkusTransaction.begin();
        deleteEmbeddings(knowledge.getId());
        if (repository.countChecksum(knowledge.getChecksum()) == 1
                && knowledge instanceof FileKnowledge) {
            // Only source file if no other knowledge has the same source
            fileStorage.deleteFile(knowledge.getChecksum());
        }
        Optional<String> ownerUsername = getOwnerUsername(knowledge);
        repository.delete(knowledge);
        QuarkusTransaction.commit();
        if (ownerUsername.isPresent()) {
            Log.infof("Deleted knowledge '%s' with owner '%s'.", id, ownerUsername.get());
        } else {
            Log.infof("Deleted knowledge '%s'.", id);
        }
    }

    @Override
    public void setKnowledgeIngestionMetadata(UUID id, IngestionStatus status, Integer tokenCount) {
        repository.setIngestionMetadata(id, status, tokenCount);
    }

    private Knowledge addSourceInternal(Path file, String fileName, String contentType)
            throws IOException, UnexpectedFileStorageFailureException {
        if (Files.size(file) == 0) {
            throw new EmptyFileException(fileName);
        }

        String checksum = generateChecksum(file);
        boolean existingChecksum = repository.countChecksum(checksum) > 0;

        // Start transaction
        QuarkusTransaction.begin();
        // Add file to knowledge index
        Knowledge knowledge = new FileKnowledge(checksum, contentType, URI.create(fileName));
        knowledge.setLabel(fileName);
        repository.persist(knowledge);
        // Store file in file storage if the file hasn't been added before
        if (!existingChecksum) {
            fileStorage.storeFile(checksum, file, createFileMetadata(checksum, contentType));
        }
        // Commit transaction
        QuarkusTransaction.commit();
        return knowledge;
    }

    @Override
    public UUID addSource(Path file, String fileName, String contentType)
            throws IOException, UnexpectedFileStorageFailureException {
        Knowledge knowledge = addSourceInternal(file, fileName, contentType);
        // Dispatch ingestion
        ingestToStore(file, createEmbeddingMetadata(knowledge));
        Log.infof("Added knowledge '%s' with source file '%s'.", knowledge.getId(), fileName);
        return knowledge.getId();
    }

    @Override
    public UUID addSource(Path file, String fileName, String contentType, User owner)
            throws IOException, UnexpectedFileStorageFailureException {
        UUID id = addSourceInternal(file, fileName, contentType).getId();
        // Start transaction
        QuarkusTransaction.begin();
        // Reload knowledge to prevent jakarta.persistence.EntityExistsException: detached entity
        // passed to persist
        Knowledge knowledge = repository.findById(id);
        knowledge.setPermission(owner, Permission.OWNER);
        repository.persist(knowledge);
        // Commit transaction
        QuarkusTransaction.commit();
        // Dispatch ingestion
        ingestToStore(file, createEmbeddingMetadata(knowledge));
        Log.infof(
                "Added knowledge '%s' with source file '%s' and owner '%s'.",
                id, fileName, owner.getUsername());
        return knowledge.getId();
    }

    @Override
    public void updateSource(UUID id, Path file, String fileName, String contentType)
            throws KnowledgeNotFoundException, IOException, UnexpectedFileStorageFailureException {
        if (Files.size(file) == 0) {
            throw new EmptyFileException(fileName);
        }

        Knowledge knowledge = getKnowledge(id);
        if (!(knowledge instanceof FileKnowledge fileKnowledge)) {
            throw new KnowledgeNotFoundException(id);
        }
        String checksum = generateChecksum(file);

        if (knowledge.getChecksum().equals(checksum)) {
            Log.infof(
                    "Skipping update of unchanged source file '%s' of knowledge '%s'.",
                    fileName, id);
            return;
        }

        // Remove old file and embeddings if no other knowledge has the same source
        if (repository.countChecksum(knowledge.getChecksum()) == 1) {
            fileStorage.deleteFile(knowledge.getChecksum());
        }
        deleteEmbeddings(knowledge.getId());

        // Start transaction
        QuarkusTransaction.begin();
        // Reload knowledge to prevent jakarta.persistence.EntityExistsException: detached entity
        // passed to persist
        fileKnowledge = (FileKnowledge) getKnowledge(id);
        // Update knowledge index
        fileKnowledge.setChecksum(checksum);
        fileKnowledge.setSource(URI.create(fileName));
        fileKnowledge.setContentType(contentType);
        fileKnowledge.setIngestionStatus(IngestionStatus.PENDING);
        fileKnowledge.setLabel(fileName);
        repository.persistAndFlush(fileKnowledge);
        // Store new file
        fileStorage.storeFile(checksum, file, createFileMetadata(checksum, fileName));
        // Create metadata while having the transaction open to avoid
        // org.hibernate.LazyInitializationException
        Map<String, String> metadata = createEmbeddingMetadata(fileKnowledge);
        Optional<String> ownerUsername = getOwnerUsername(fileKnowledge);
        // Commit transaction
        QuarkusTransaction.commit();
        // Dispatch ingestion
        ingestToStore(file, metadata);
        if (ownerUsername.isPresent()) {
            Log.infof(
                    "Updated source file of knowledge '%s' with new file '%s' and owner '%s'.",
                    id, fileName, ownerUsername.get());
        } else {
            Log.infof("Updated source file of knowledge '%s' with new file '%s'.", id, fileName);
        }
    }

    @Transactional
    @Override
    public void setPermission(UUID id, User user, Permission permission)
            throws KnowledgeNotFoundException, IllegalPermissionModificationException {
        Knowledge knowledge = getKnowledge(id);

        if (knowledge.getIngestionStatus() != IngestionStatus.SUCCEEDED) {
            throw new IllegalPermissionModificationException(
                    "Cannot modify permission for knowledge that has not been ingested");
        }

        if (permission == Permission.NONE) {
            throw new IllegalPermissionModificationException(
                    "Setting explicit permission NONE is not allowed");
        }
        if (permission == Permission.OWNER) {
            throw new IllegalPermissionModificationException(
                    "Adding owner permission is not allowed");
        }

        Permission existingPermission = knowledge.getPermission(user);
        if (existingPermission == Permission.OWNER) {
            throw new IllegalPermissionModificationException(
                    "Modifying owner's permission is not allowed");
        }
        knowledge.setPermission(user, permission);
        repository.persist(knowledge);
        embeddingStorePermissionMetadataManager.updatePermissionMetadata(knowledge);
    }

    @Transactional
    @Override
    public void removePermission(UUID id, User user)
            throws KnowledgeNotFoundException, IllegalPermissionModificationException {
        Knowledge knowledge = getKnowledge(id);

        if (knowledge.getIngestionStatus() != IngestionStatus.SUCCEEDED) {
            throw new IllegalPermissionModificationException(
                    "Cannot modify permission for knowledge that has not been ingested");
        }

        Permission existingPermission = knowledge.getPermission(user);
        if (existingPermission == Permission.OWNER) {
            throw new IllegalPermissionModificationException("Cannot remove owner permission");
        }
        knowledge.removePermission(user);
        repository.persist(knowledge);
        embeddingStorePermissionMetadataManager.updatePermissionMetadata(knowledge);
    }

    @Transactional
    @Override
    public void addTag(UUID id, String tag) throws KnowledgeNotFoundException {
        Knowledge knowledge = getKnowledge(id);
        knowledge.addTag(tag);
        repository.persist(knowledge);
    }

    @Transactional
    @Override
    public void removeTag(UUID id, String tag) throws KnowledgeNotFoundException {
        Knowledge knowledge = getKnowledge(id);
        knowledge.removeTag(tag);
        repository.persist(knowledge);
    }

    @Transactional
    @Override
    public void setLabel(UUID id, String label) throws KnowledgeNotFoundException {
        Knowledge knowledge = getKnowledge(id);
        knowledge.setLabel(label);
        repository.persist(knowledge);
    }

    @Override
    public NamedFileContainer getFile(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        Knowledge knowledge = getKnowledge(id);
        if (!(knowledge instanceof FileKnowledge fileKnowledge)) {
            throw new KnowledgeNotFoundException(id);
        }
        try {
            FileContainer fc = fileStorage.getFile(fileKnowledge.getChecksum());
            return new NamedFileContainer(
                    fileKnowledge.getSource().toString(), fc.content(), fc.metadata());
        } catch (FileNotFoundException e) {
            throw new RuntimeException( // NOSONAR: this should never happen
                    String.format(FILE_STORAGE_FILE_NOT_FOUND_PATTERN, fileKnowledge.getId()), e);
        }
    }

    @Override
    public void retryFailedIngestion(UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        Knowledge knowledge = getKnowledge(id);

        // Do nothing if ingestion status is not FAILED
        if (knowledge.getIngestionStatus() != IngestionStatus.FAILED) {
            return;
        }

        // Get the file from the file storage and save it as temporary file
        NamedFileContainer fc = getFile(id);
        File tempFile;
        try {
            tempFile = File.createTempFile(id.toString(), null, TEMP_DIR.toFile());
            tempFile.deleteOnExit(); // the file will be deleted when the JVM exits
            Files.copy(fc.content(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UnexpectedFileStorageFailureException(
                    "Failed to temporary save retrieved file to disk", e);
        }

        // Create metadata
        Map<String, String> metadata = createEmbeddingMetadata(knowledge);

        // Reset ingestion status
        setKnowledgeIngestionMetadata(id, IngestionStatus.PENDING, null);

        // Retry ingestion
        ingestToStore(tempFile.toPath(), metadata);
    }

    private Map<String, String> createFileMetadata(String checksum, String contentType) {
        return Map.of(
                CommonMetadataKeys.CHECKSUM,
                checksum,
                CommonMetadataKeys.CONTENT_TYPE,
                contentType);
    }

    private Map<String, String> createEmbeddingMetadata(Knowledge knowledge) {
        return Map.of(
                EmbeddingMetadataKeys.KNOWLEDGE_ID,
                knowledge.getId().toString(),
                CommonMetadataKeys.CHECKSUM,
                knowledge.getChecksum(),
                CommonMetadataKeys.CONTENT_TYPE,
                knowledge.getContentType(),
                EmbeddingMetadataKeys.PERMISSION,
                PermissionMetadataMapper.permissionsToMetadataEntry(knowledge.getPermissions()));
    }

    /**
     * Remove embeddings belonging to the given id from the {@link EmbeddingStore}.
     *
     * @param id the id of the knowledge to remove embeddings for
     */
    private void deleteEmbeddings(UUID id) {
        Filter filter = new IsEqualTo(EmbeddingMetadataKeys.KNOWLEDGE_ID, id);
        embeddingStore.removeAll(filter);
        Log.infof("Deleted embeddings for knowledge '%s'.", id);
    }

    /**
     * Ingest a file specified by its {@link Path} to the {@link EmbeddingStore}.
     *
     * @param file the file to ingest
     * @param metadata the metadata to attach to the embeddings
     */
    private void ingestToStore(Path file, Map<String, String> metadata) {
        DocumentSource ds = FileSystemSource.from(file);
        // Use our own Apache PDFBox document parser implementation for PDF files to embed page
        // markers.
        // Fallback to Apache Tika as document parser, as it can automatically detect and parse a
        // large
        // number of file formats. Tika is using Apache PDFBox and Apache POI under the hood.
        // https://docs.langchain4j.dev/tutorials/rag/#document-parser
        boolean isPdf = "application/pdf".equals(metadata.get(CommonMetadataKeys.CONTENT_TYPE));
        DocumentParser parser = isPdf ? new PdfDocumentParser() : new ApacheTikaDocumentParser();
        Document document = DocumentLoader.load(ds, parser);
        document.metadata().remove(FILE_NAME);
        document.metadata().remove(ABSOLUTE_DIRECTORY_PATH);
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            document.metadata().put(entry.getKey(), entry.getValue());
        }
        ingestor.ingestDocument(document);
    }

    private Optional<String> getOwnerUsername(Knowledge knowledge) {
        return knowledge.getPermissions().entrySet().stream()
                .filter(entry -> entry.getValue() == Permission.OWNER)
                .map(Map.Entry::getKey)
                .map(User::getUsername)
                .findFirst();
    }
}
