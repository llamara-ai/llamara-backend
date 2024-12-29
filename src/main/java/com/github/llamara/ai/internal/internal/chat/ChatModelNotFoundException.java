/*
 * #%L
 * llamara-backend
 * %%
 * Copyright (C) 2024 Contributors to the LLAMARA project
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
package com.github.llamara.ai.internal.internal.chat;

/**
 * Exception signaling that no {@link ChatModelContainer} with the given UID has been found.
 *
 * @author Florian Hotze - Initial contribution
 */
public class ChatModelNotFoundException extends Exception {
    ChatModelNotFoundException(String uid) {
        super(String.format("Chat model with uid '%s' not found!", uid));
    }
}
