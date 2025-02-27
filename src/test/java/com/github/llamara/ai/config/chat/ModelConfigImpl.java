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
package com.github.llamara.ai.config.chat;

import java.util.Optional;

public abstract class ModelConfigImpl implements ChatModelConfig.ModelConfig {
    @Override
    public String uid() {
        return "";
    }

    @Override
    public ChatModelConfig.ChatModelProvider provider() {
        return null;
    }

    @Override
    public Optional<String> baseUrl() {
        return Optional.empty();
    }

    @Override
    public Optional<String> resourceName() {
        return Optional.empty();
    }

    @Override
    public String model() {
        return "";
    }

    @Override
    public Optional<String> label() {
        return Optional.empty();
    }

    @Override
    public Optional<String> description() {
        return Optional.empty();
    }

    @Override
    public boolean systemPromptEnabled() {
        return false;
    }

    @Override
    public Double temperature() {
        return 0.0;
    }

    @Override
    public Optional<Double> topP() {
        return Optional.empty();
    }

    @Override
    public Optional<Double> frequencyPenalty() {
        return Optional.empty();
    }

    @Override
    public Optional<Double> presencePenalty() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> maxTokens() {
        return Optional.empty();
    }
}
