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

import com.github.llamara.ai.internal.chat.ChatModelContainer;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Provides configuration for the {@link ChatModelContainer}s. Model parameter descriptions are
 * taken from the <a href="https://platform.openai.com/docs/api-reference/chat/create">OpenAI
 * docs</a>.
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

        @WithDefault("true")
        boolean systemPromptEnabled();

        /**
         * What sampling temperature to use, between 0 and 2. Higher values like 0.8 will make the
         * output more random, while lower values like 0.2 will make it more focused and
         * deterministic. It is generally recommended altering this or topP but not both.
         *
         * @return sampling temperature
         */
        @WithDefault("0.7")
        Double temperature();

        /**
         * An alternative to sampling with temperature, called nucleus sampling, where the model
         * considers the results of the tokens with top_p probability mass. So 0.1 means only the
         * tokens comprising the top 10% probability mass are considered. It is generally
         * recommended altering this or temperature but not both.
         *
         * @return top_p
         */
        Optional<Double> topP();

        /**
         * Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing
         * frequency in the text so far, decreasing the model's likelihood to repeat the same line
         * verbatim.
         *
         * @return frequency penalty
         */
        Optional<Double> frequencyPenalty();

        /**
         * Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they
         * appear in the text so far, increasing the model's likelihood to talk about new topics.
         *
         * @return presence penalty
         */
        Optional<Double> presencePenalty();

        /**
         * An upper bound for the number of tokens that can be generated for a completion, including
         * visible output tokens and reasoning tokens. This value can be used to control costs for
         * API calls.
         *
         * @return max tokens
         */
        Optional<Integer> maxTokens();
    }

    enum ChatModelProvider {
        AZURE,
        GOOGLE_GEMINI,
        MISTRAL,
        OLLAMA,
        OPENAI
    }
}
