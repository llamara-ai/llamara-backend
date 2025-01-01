package com.github.llamara.ai.internal.internal.security.user;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Extends {@link UserRepository} for modifying visibility of constructor and methods to access them
 * in tests.
 */
@ApplicationScoped
public class TestUserRepository extends UserRepository {
    public void init() {
        super.init();
    }
}
