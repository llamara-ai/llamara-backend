package com.github.llamara.ai.internal.internal.security.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

/**
 * Extends {@link UserRepository} for modifying visibility of constructor and methods to access them
 * in tests.
 */
@Typed(TestUserRepository.class)
@ApplicationScoped
public class TestUserRepository extends UserRepository {
    @Override
    public void init() {
        super.init();
    }
}
