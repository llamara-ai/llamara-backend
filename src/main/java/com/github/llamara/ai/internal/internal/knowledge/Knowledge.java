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
package com.github.llamara.ai.internal.internal.knowledge;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.Table;

import com.github.llamara.ai.internal.internal.ingestion.IngestionStatus;
import com.github.llamara.ai.internal.internal.knowledge.storage.FileStorage;
import com.github.llamara.ai.internal.internal.security.Permission;
import com.github.llamara.ai.internal.internal.security.Users;
import com.github.llamara.ai.internal.internal.security.user.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA {@link Entity} storing knowledge metadata.
 *
 * <p>Knowledge is identified by its unique ID.
 *
 * <p>The embedding is stored in the {@link dev.langchain4j.store.embedding.EmbeddingStore} and the
 * knowledge source file is stored in the {@link FileStorage}. Stored embeddings and source files
 * are associated with the knowledge through the checksum of the knowledge source.
 *
 * @author Florian Hotze - Initial contribution
 */
@Entity
@Table(name = "knowledge")
public class Knowledge {

    @GeneratedValue
    @Id
    @Column(name = "id", unique = true, updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false, nullable = false)
    private KnowledgeType type;

    @Column(nullable = false)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false)
    private IngestionStatus ingestionStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(nullable = false)
    private URI source;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @ElementCollection
    @CollectionTable(
            name = "knowledge_permissions",
            joinColumns = {@JoinColumn(name = "knowledge_id", referencedColumnName = "id")})
    @MapKeyJoinColumn(name = "username")
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private final Map<User, Permission> permissions = new HashMap<>();

    private String label;

    @ElementCollection // specify that tags is a collection of elements
    @CollectionTable(
            name = "knowledge_tags", // name of table to store tags to
            joinColumns =
                    @JoinColumn(name = "knowledge_id")) // specify foreign key column that will link
    // elements to Knowledge entity
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    /** Constructor for JPA. */
    protected Knowledge() {}

    /**
     * Create new knowledge. Constructor for application.
     *
     * @param type
     * @param checksum
     * @param contentType
     * @param source
     */
    Knowledge(KnowledgeType type, String checksum, String contentType, URI source) {
        this.type = type;
        this.checksum = checksum;
        this.ingestionStatus = IngestionStatus.PENDING;
        this.contentType = contentType;
        this.source = source;
    }

    /**
     * Get the id of the knowledge.
     *
     * @return
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the type of the knowledge source.
     *
     * @return
     */
    public KnowledgeType getType() {
        return type;
    }

    /**
     * Get the checksum of the knowledge source. The checksum is used to associate the embedding and
     * source file with the knowledge.
     *
     * @return
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Get the current status of the asynchronous ingestion.
     *
     * @return
     */
    public IngestionStatus getIngestionStatus() {
        return ingestionStatus;
    }

    /**
     * Get the creation timestamp.
     *
     * @return
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Get the last update timestamp.
     *
     * @return
     */
    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /**
     * Get the content type/MIME Type of knowledge source.
     *
     * @return
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get users' permissions for this knowledge.
     *
     * @return
     */
    public Map<User, Permission> getPermissions() {
        return Collections.unmodifiableMap(permissions);
    }

    /**
     * Get the permission for a given user.
     *
     * @param user
     * @return
     */
    public Permission getPermission(User user) {
        return Objects.requireNonNullElse(
                permissions.get(user),
                Objects.requireNonNullElse(permissions.get(Users.ANY), Permission.NONE));
    }

    /**
     * Get the permission for a given user.
     *
     * @param username
     * @return
     */
    public Permission getPermission(String username) {
        return getPermission(new User(username));
    }

    /**
     * Get the source URI.
     *
     * @return
     */
    public URI getSource() {
        return source;
    }

    /**
     * Get the optional, user-provided label.
     *
     * @return
     */
    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    /**
     * Get the user-provided tags.
     *
     * @return
     */
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Update the checksum for the knowledge source.
     *
     * @param checksum
     */
    void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * Update the status of the asynchronous ingestion.
     *
     * @param status
     */
    void setIngestionStatus(IngestionStatus status) {
        this.ingestionStatus = status;
    }

    /**
     * Update the source URI.
     *
     * @param source
     */
    void setSource(URI source) {
        this.source = source;
    }

    /**
     * Update the content type/MIME type of the knowledge source.
     *
     * @param contentType
     */
    void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Set a {@link Permission} for a {@link User}.
     *
     * @param user
     * @param permission
     */
    public void setPermission(User user, Permission permission) {
        this.permissions.put(user, permission);
    }

    /**
     * Remove the {@link Permission} for a {@link User}
     *
     * @param user
     */
    void removePermission(User user) {
        this.permissions.remove(user);
    }

    /**
     * Set the optional, user-defined label.
     *
     * @param label
     */
    void setLabel(String label) {
        this.label = label;
    }

    /**
     * Add a user-provided tag.
     *
     * @param tag
     */
    void addTag(String tag) {
        tags.add(tag);
    }

    /**
     * Remove a user-provided tag.
     *
     * @param tag
     */
    void removeTag(String tag) {
        tags.remove(tag);
    }
}
