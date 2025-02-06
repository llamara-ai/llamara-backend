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
package com.github.llamara.ai.internal.security.knowledge;

import com.github.llamara.ai.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.knowledge.KnowledgeRepository;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.Roles;
import com.github.llamara.ai.internal.security.Users;

import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * {@link KnowledgeRepository} that is aware of the current user identified by its {@link
 * SecurityIdentity}. Its specific methods only allows access to knowledge accessible by the current
 * user.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(UserAwareKnowledgeRepository.class)
@ApplicationScoped
public class UserAwareKnowledgeRepository extends KnowledgeRepository {
    private final SecurityIdentity identity;

    @Inject
    UserAwareKnowledgeRepository(SecurityIdentity identity) {
        this.identity = identity;
    }

    /**
     * Check if the current user has at least read permission for the given knowledge entry.
     *
     * <ul>
     *   <li>Owners always have read/write permission.
     *   <li>Anonymous users only have access if {@link Users#ANY} has read access.
     * </ul>
     *
     * @param knowledge the knowledge entry to check
     * @return <code>true</code> if the user has read permission, <code>false</code> otherwise
     */
    private boolean hasReadPermission(Knowledge knowledge) {
        if (identity.hasRole(Roles.ADMIN)) {
            return true;
        }
        if (!identity.isAnonymous()) {
            return knowledge.getPermission(identity.getPrincipal().getName()) != Permission.NONE;
        }
        return knowledge.getPermission(Users.ANY) != Permission.NONE;
    }

    /**
     * Find a knowledge entry by its ID and check if the user has at least read permission. Admins
     * have access to everything.
     *
     * @param id the ID of the knowledge entry
     * @return the entity found, or <code>null</code> if not found
     */
    @Override
    public Knowledge findById(UUID id) {
        Knowledge knowledge = super.findById(id);
        if (knowledge == null) {
            return null;
        }
        return hasReadPermission(knowledge) ? knowledge : null;
    }

    /**
     * Check if a knowledge entry with the given checksum exists and the user has at least
     * read/write permission.
     *
     * <p>This method does not account for the user having the admin role and implicit access to
     * everything, as admins should be able to up add a source themselves if it is already in the
     * system.
     *
     * @param checksum the checksum to check for
     * @return the knowledge entry if it exists and the user has at least read/write permission
     */
    @Transactional
    public Optional<Knowledge> existsChecksum(String checksum) {
        return super.find("checksum", checksum).stream()
                .filter(
                        knowledge -> {
                            Permission permission =
                                    knowledge.getPermission(identity.getPrincipal().getName());
                            return permission == Permission.OWNER
                                    || permission == Permission.READWRITE;
                        })
                .findFirst();
    }
}
