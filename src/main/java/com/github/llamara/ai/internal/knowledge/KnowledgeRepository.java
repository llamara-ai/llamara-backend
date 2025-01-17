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
package com.github.llamara.ai.internal.knowledge;

import com.github.llamara.ai.internal.ingestion.IngestionStatus;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.Users;

import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

/**
 * Hibernate ORM {@link PanacheRepository} for {@link Knowledge}.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
public class KnowledgeRepository implements PanacheRepository<Knowledge> {
    /**
     * List all knowledge entries that are public, i.e. shared with {@link Users#ANY}.
     *
     * @return public knowledge
     */
    public List<Knowledge> listAllPublicKnowledge() {
        return listAll().stream() // TODO: Filter in DB query
                .filter(k -> k.getPermission(Users.ANY) != Permission.NONE)
                .toList();
    }

    /**
     * Find knowledge by its ID.
     *
     * @param id
     * @return the entity found, or <code>null</code> if not found
     */
    public Knowledge findById(UUID id) {
        return find("id", id).firstResult();
    }

    /**
     * Get the number of knowledge entries that have the given checksum.
     *
     * @param checksum
     * @return
     */
    public long countChecksum(String checksum) {
        return find("checksum", checksum).count();
    }

    /**
     * Set the status of knowledge identified by its ID if it exists.
     *
     * @param id
     * @param status
     */
    @Transactional
    public void setStatusFor(UUID id, IngestionStatus status) {
        Knowledge knowledge = findById(id);
        if (knowledge == null) {
            return;
        }
        knowledge.setIngestionStatus(status);
        persist(knowledge);
    }
}
