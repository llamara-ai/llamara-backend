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

import com.github.llamara.ai.internal.security.Users;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

/**
 * {@link UserManager} implementation for handling anonymous users.
 *
 * <p>Anonymous users are not stored in the database.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(AnonymousUserManagerImpl.class)
@ApplicationScoped
public class AnonymousUserManagerImpl implements UserManager {
    private final UserRepository userRepository;

    @Inject
    public AnonymousUserManagerImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

    @Override
    public User getCurrentUser() {
        return Users.ANY;
    }

    @Override
    public User getUser(String username) throws UserNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException(username);
        }
        return user;
    }

    @Override
    public User getUserAny() {
        return userRepository.findByUsername(Users.ANY_USERNAME);
    }
}
