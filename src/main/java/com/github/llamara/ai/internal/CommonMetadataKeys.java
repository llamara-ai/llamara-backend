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
package com.github.llamara.ai.internal;

/**
 * Constant class for common metadata keys.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class CommonMetadataKeys {
    private CommonMetadataKeys() {}

    /** The checksum of the knowledge source. */
    public static final String CHECKSUM = "checksum";

    /** The content type of the document. */
    public static final String CONTENT_TYPE = "content_type";

    /** The number of segments the document is split into. */
    public static final String SEGMENTS = "segments";
}
