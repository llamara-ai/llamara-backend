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
package com.github.llamara.ai.internal.rest.dto;

import com.github.llamara.ai.internal.ingestion.IngestionStatus;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.user.User;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO record for {@link com.github.llamara.ai.internal.knowledge.persistence.Knowledge} with
 * additional properties to transfer the values of its subclasses.
 *
 * @param type
 * @param id
 * @param checksum
 * @param ingestionStatus
 * @param tokenCount
 * @param createdAt
 * @param lastUpdatedAt
 * @param contentType
 * @param permissions
 * @param label
 * @param tags
 * @param source only for {@link com.github.llamara.ai.internal.knowledge.persistence.FileKnowledge}
 * @author Florian Hotze - Initial contribution
 */
public record KnowledgeRecord(
        KnowledgeType type,
        UUID id,
        String checksum,
        IngestionStatus ingestionStatus,
        Integer tokenCount,
        Instant createdAt,
        Instant lastUpdatedAt,
        String contentType,
        Map<User, Permission> permissions,
        String label,
        Set<String> tags,
        @JsonInclude(JsonInclude.Include.NON_NULL) URI source) {}
