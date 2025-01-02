package com.github.llamara.ai.internal.internal.knowledge;

import java.net.URI;
import jakarta.persistence.Entity;

/**
 * Extends {@link Knowledge} for modifying visibility of constructor and methods to access them in
 * tests.
 */
@Entity
public class TestKnowledge extends Knowledge {
    protected TestKnowledge() {
        super();
    }

    public TestKnowledge(KnowledgeType type, String checksum, String contentType, URI source) {
        super(type, checksum, contentType, source);
    }
}
