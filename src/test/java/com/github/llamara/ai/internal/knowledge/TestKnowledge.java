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

import java.net.URI;
import jakarta.persistence.Entity;

/**
 * Extends {@link Knowledge} for modifying visibility of constructor and methods to access them in
 * tests.
 */
@Entity
public class TestKnowledge extends Knowledge {
    protected TestKnowledge() {
        super();
    }

    public TestKnowledge(KnowledgeType type, String checksum, String contentType, URI source) {
        super(type, checksum, contentType, source);
    }
}
