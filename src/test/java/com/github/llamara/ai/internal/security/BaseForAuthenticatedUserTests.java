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
package com.github.llamara.ai.internal.security;

import com.github.llamara.ai.internal.Utils;
import com.github.llamara.ai.internal.knowledge.KnowledgeRepository;
import com.github.llamara.ai.internal.knowledge.persistence.FileKnowledge;
import com.github.llamara.ai.internal.knowledge.persistence.Knowledge;
import com.github.llamara.ai.internal.security.session.Session;
import com.github.llamara.ai.internal.security.session.UserAwareSessionRepository;
import com.github.llamara.ai.internal.security.user.TestUserRepository;
import com.github.llamara.ai.internal.security.user.User;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for tests that require setting up a {@link SecurityIdentity} and {@link UserInfo}, and
 * need to set up {@link User} and {@link Session} in the database.
 */
@QuarkusTest
public abstract class BaseForAuthenticatedUserTests {
    protected static final String OWN_USERNAME = "test";
    protected static final String OWN_DISPLAYNAME = "Test";

    protected static final Path FILE = Path.of("src/test/resources/llamara.txt");
    protected static final String FILE_NAME = "llamara.txt";
    protected static final String FILE_MIME_TYPE = "text/plain";
    protected static final String FILE_CHECKSUM;

    static {
        try {
            FILE_CHECKSUM = Utils.generateChecksum(FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject KnowledgeRepository knowledgeRepository;

    @InjectSpy protected TestUserRepository userRepository;
    @InjectSpy protected UserAwareSessionRepository userAwareSessionRepository;

    @InjectMock protected SecurityIdentity identity;

    @Transactional
    @BeforeEach
    protected void setup() {
        setupIdentity(OWN_USERNAME, OWN_DISPLAYNAME);

        userRepository.init();

        clearAllInvocations();

        assertEquals(1, userRepository.count());
        assertEquals(0, userAwareSessionRepository.count());
    }

    @Transactional
    @AfterEach
    protected void destroy() {
        userAwareSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    void clearAllInvocations() {
        clearInvocations(userRepository, userAwareSessionRepository);
    }

    /**
     * Set up the mock for the {@link SecurityIdentity} and {@link UserInfo} to return the given
     * username and display name.
     *
     * @param username the username
     * @param displayName the display name
     */
    protected void setupIdentity(String username, String displayName) {
        when(identity.getPrincipal()).thenReturn(() -> username);
        when(identity.getAttribute(Claims.full_name.name())).thenReturn(displayName);
    }

    /**
     * Set up a user in the database for the current {@link SecurityIdentity} and {@link UserInfo}.
     */
    @Transactional
    protected void setupUser() {
        String username = identity.getPrincipal().getName();
        String displayName = identity.getAttribute(Claims.full_name.name());

        User user = new User(username);
        user.setDisplayName(displayName);
        userRepository.persist(user);
        assertEquals(username, user.getUsername());
        assertEquals(displayName, user.getDisplayName());
        assertEquals(0, user.getSessions().size());

        clearAllInvocations();
    }

    /**
     * Set up a session for the user of the current {@link SecurityIdentity} and {@link UserInfo}.
     * Validates the session and enforces that the user has exactly one session.
     *
     * @return the ID of the created session
     */
    @Transactional
    protected UUID setupSession() {
        User user = userRepository.findByUsername(identity.getPrincipal().getName());
        Session session = new Session(user);
        user.addSession(session);
        userRepository.persist(user);
        assertEquals(identity.getPrincipal().getName(), session.getUser().getUsername());
        assertEquals(
                1,
                userRepository
                        .findByUsername(identity.getPrincipal().getName())
                        .getSessions()
                        .size());
        assertEquals(1, userAwareSessionRepository.count());

        clearAllInvocations();

        return session.getId();
    }

    /**
     * Set up knowledge with the given permission for the current {@link SecurityIdentity}.
     *
     * @param permission the permission
     * @return the ID of the created knowledge
     */
    @Transactional
    protected UUID setupKnowledgeWithPermission(Permission permission) {
        Knowledge knowledge = new FileKnowledge(FILE_CHECKSUM, FILE_NAME, URI.create(FILE_NAME));
        knowledge.setPermission(
                userRepository.findByUsername(identity.getPrincipal().getName()), permission);
        knowledgeRepository.persist(knowledge);
        return knowledge.getId();
    }
}
