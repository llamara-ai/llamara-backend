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
 * Exception thrown when the application fails to start, e.g. due to wrong configuration or failed
 * database connection.
 *
 * @author Florian Hotze - Initial contribution
 */
public class StartupException extends RuntimeException {
    public StartupException(String message) {
        super(message);
    }

    public StartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
