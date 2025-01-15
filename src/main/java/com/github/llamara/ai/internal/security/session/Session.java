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
package com.github.llamara.ai.internal.security.session;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.llamara.ai.internal.security.user.User;
import org.hibernate.annotations.CreationTimestamp;

/**
 * JPA {@link Entity} storing session information.
 *
 * <p>Sessions are identified by their unique id. They are owned by a user.
 *
 * @author Florian Hotze - Initial contribution
 */
@Entity
@Table(name = "sessions")
public class Session {
    @GeneratedValue
    @Id
    @Column(name = "id", unique = true, updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "username")
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    private String label;

    /** Constructor for JPA. */
    protected Session() {}

    /** Constructor for application. */
    public Session(User user) {
        this.user = user;
    }

    /** Constructor for {@link AnonymousSession} */
    protected Session(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Session session)) return false;
        return Objects.equals(id, session.id)
                && Objects.equals(user, session.user)
                && Objects.equals(createdAt, session.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, createdAt);
    }
}
