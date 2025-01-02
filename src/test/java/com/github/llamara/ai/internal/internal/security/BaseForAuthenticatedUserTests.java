package com.github.llamara.ai.internal.internal.security;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;

import com.github.llamara.ai.internal.internal.Utils;
import com.github.llamara.ai.internal.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeRepository;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeType;
import com.github.llamara.ai.internal.internal.knowledge.TestKnowledge;
import com.github.llamara.ai.internal.internal.security.session.Session;
import com.github.llamara.ai.internal.internal.security.session.UserAwareSessionRepository;
import com.github.llamara.ai.internal.internal.security.user.TestUserRepository;
import com.github.llamara.ai.internal.internal.security.user.User;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
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
    @InjectMock protected UserInfo userInfo;

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
        when(userInfo.getName()).thenReturn(displayName);
    }

    /**
     * Set up a user in the database for the current {@link SecurityIdentity} and {@link UserInfo}.
     */
    @Transactional
    protected void setupUser() {
        String username = identity.getPrincipal().getName();
        String displayName = userInfo.getName();

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
     * @param permission
     * @return
     */
    @Transactional
    protected UUID setupKnowledgeWithPermission(Permission permission) {
        Knowledge knowledge =
                new TestKnowledge(
                        KnowledgeType.FILE, FILE_CHECKSUM, FILE_NAME, URI.create(FILE_NAME));
        knowledge.setPermission(
                userRepository.findByUsername(identity.getPrincipal().getName()), permission);
        knowledgeRepository.persist(knowledge);
        return knowledge.getId();
    }
}
