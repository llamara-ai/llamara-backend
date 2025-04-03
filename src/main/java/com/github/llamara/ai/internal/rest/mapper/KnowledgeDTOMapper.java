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
package com.github.llamara.ai.internal.rest.mapper;

import com.github.llamara.ai.internal.knowledge.persistence.FileKnowledge;
import com.github.llamara.ai.internal.knowledge.persistence.Knowledge;
import com.github.llamara.ai.internal.rest.dto.KnowledgeRecord;
import com.github.llamara.ai.internal.rest.dto.KnowledgeType;

import java.util.Collection;

/**
 * DTO mapper class for mapping {@link Knowledge} to {@link KnowledgeRecord}.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class KnowledgeDTOMapper {
    private KnowledgeDTOMapper() {}

    public static KnowledgeRecord map(Knowledge knowledge) {
        if (knowledge instanceof FileKnowledge fileKnowledge) {
            return new KnowledgeRecord(
                    KnowledgeType.FILE,
                    fileKnowledge.getId(),
                    fileKnowledge.getChecksum(),
                    fileKnowledge.getIngestionStatus(),
                    fileKnowledge.getTokenCount().orElse(null),
                    fileKnowledge.getCreatedAt(),
                    fileKnowledge.getLastUpdatedAt(),
                    fileKnowledge.getContentType(),
                    fileKnowledge.getPermissions(),
                    fileKnowledge.getLabel().orElse(null),
                    fileKnowledge.getTags(),
                    fileKnowledge.getSource());
        }
        return null;
    }

    public static Collection<KnowledgeRecord> map(Collection<Knowledge> knowledge) {
        return knowledge.stream().map(KnowledgeDTOMapper::map).toList();
    }
}
