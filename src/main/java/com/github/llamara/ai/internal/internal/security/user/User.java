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
package com.github.llamara.ai.internal.internal.security.user;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.llamara.ai.internal.internal.security.session.Session;

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
