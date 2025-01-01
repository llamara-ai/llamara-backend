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
package com.github.llamara.ai.internal.config.chat;

import java.util.List;
import java.util.Optional;

import com.github.llamara.ai.internal.internal.chat.ChatModelContainer;
import io.smallrye.config.ConfigMapping;

/**
 * Provides configuration for the {@link ChatModelContainer}s.
 *
 * @author Florian Hotze - Initial contribution
 */
@ConfigMapping(prefix = "chat")
public interface ChatModelConfig {
    List<ModelConfig> models();

    interface ModelConfig {
        String uid();

        ChatModelProvider provider();

        Optional<String> baseUrl();

        Optional<String> resourceName();

        String model();

        Optional<String> label();

        Optional<String> description();
    }

    enum ChatModelProvider {
        AZURE,
        OLLAMA,
        OPENAI
    }
}
