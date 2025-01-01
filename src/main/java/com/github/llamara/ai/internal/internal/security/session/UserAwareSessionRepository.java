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
package com.github.llamara.ai.internal.internal.security.session;

import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * Hibernate ORM {@link PanacheRepository} for {@link Session} that is aware of the current {@link
 * SecurityIdentity}. Its specific methods only allows access to sessions that are owned by the
 * current user.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
public class UserAwareSessionRepository implements PanacheRepository<Session> {
    private final SecurityIdentity identity;

    @Inject
    UserAwareSessionRepository(SecurityIdentity identity) {
        this.identity = identity;
    }

    private boolean ownsSession(Session session) {
        return identity.getPrincipal().getName().equals(session.getUser().getUsername());
    }

    /**
     * Find a session that is owned by the current user by its ID.
     *
     * @param id the id of the session
     * @return the session
     * @throws SessionNotFoundException if no session with the given ID was found for the current
     *     user
     */
    public Session findById(UUID id) throws SessionNotFoundException {
        Session session = find("id", id).firstResult();
        if (session == null || !ownsSession(session)) {
            throw new SessionNotFoundException(id);
        }
        return session;
    }

    /**
     * Delete a session that is owned by the current user by its ID.
     *
     * @param id the id of the session
     * @throws SessionNotFoundException if no session with the given ID was found for the current
     *     user
     */
    @Transactional
    public void deleteById(UUID id) throws SessionNotFoundException {
        Session session = findById(id);
        delete(session);
    }
}
