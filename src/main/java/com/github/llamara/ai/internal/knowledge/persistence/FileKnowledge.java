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
package com.github.llamara.ai.internal.knowledge.persistence;

import java.net.URI;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * JPA {@link Entity} for file-based {@link Knowledge}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Entity
@DiscriminatorValue("FILE")
public class FileKnowledge extends Knowledge {

    @Column(nullable = false)
    private URI source;

    /** Constructor for JPA. */
    protected FileKnowledge() {}

    /**
     * Create new knowledge. Constructor for application.
     *
     * @param checksum
     * @param contentType
     * @param source
     */
    public FileKnowledge(String checksum, String contentType, URI source) {
        super(checksum, contentType);
        this.source = source;
    }

    /**
     * Get the URI of the source file.
     *
     * @return
     */
    public URI getSource() {
        return source;
    }

    /**
     * Update URI of the source file.
     *
     * @param source
     */
    public void setSource(URI source) {
        this.source = source;
    }
}
