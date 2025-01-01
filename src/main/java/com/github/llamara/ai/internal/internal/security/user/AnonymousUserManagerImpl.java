package com.github.llamara.ai.internal.internal.security.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

@Typed(AnonymousUserManagerImpl.class)
@ApplicationScoped
public class AnonymousUserManagerImpl implements UserManager {
    @Override
    public boolean register() {
        return true;
    }

    @Override
    public void enforceRegistered() throws UserNotRegisteredException {
        // do nothing
    }

    @Override
    public void delete() {
        // Do nothing here
    }
}
