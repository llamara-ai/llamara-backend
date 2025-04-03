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
package com.github.llamara.ai.internal.security.user;

import com.github.llamara.ai.internal.knowledge.persistence.Knowledge;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.session.Session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.annotation.JsonValue;
import org.hibernate.annotations.SQLJoinTableRestriction;

/**
 * JPA {@link Entity} storing user information.
 *
 * <p>Users are identified by their unique username.
 *
 * @author Florian Hotze - Initial contribution
 */
@Entity
@Table(name = "authenticated_users") // user is a reserved keyword in some databases
public class User {
    @Id
    @Column(name = "username", unique = true, updatable = false, nullable = false)
    private String username;

    private String displayName;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "username")
    private final Set<Session> sessions = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "knowledge_permissions",
            joinColumns = @JoinColumn(name = "username", referencedColumnName = "username"),
            inverseJoinColumns = @JoinColumn(name = "knowledge_id", referencedColumnName = "id"))
    @SQLJoinTableRestriction("permission IN ('OWNER', 'READWRITE', 'READONLY')")
    private final Set<Knowledge> knowledge = new HashSet<>();

    /** Constructor for JPA. */
    protected User() {}

    /** Creates a new user. Constructor for application. */
    public User(String username) {
        this.username = username;
        this.displayName = username;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Collection<Session> getSessions() {
        return Collections.unmodifiableCollection(sessions);
    }

    /**
     * Get all knowledge the user has explicit read permission for. Read permission is granted
     * through {@link Permission#READONLY} or higher.
     *
     * @return the knowledge
     */
    @Transactional // transaction is needed to get Knowledge
    public Collection<Knowledge> getKnowledge() {
        return Collections.unmodifiableSet(knowledge);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? username : displayName;
    }

    public void addSession(Session session) {
        sessions.add(session);
    }

    @JsonValue
    String toJson() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
