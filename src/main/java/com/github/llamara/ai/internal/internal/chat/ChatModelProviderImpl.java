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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static com.github.llamara.ai.internal.internal.Utils.AZURE_OPENAI_API_VERSION;
import static com.github.llamara.ai.internal.internal.Utils.buildAzureOpenaiEndpoint;

import com.github.llamara.ai.internal.config.EnvironmentVariables;
import com.github.llamara.ai.internal.config.chat.ChatModelConfig;
import com.github.llamara.ai.internal.internal.StartupException;
import com.github.llamara.ai.internal.internal.chat.aiservice.ChatModelAiService;
import com.github.llamara.ai.internal.internal.chat.history.ChatHistoryStore;
import com.github.llamara.ai.internal.internal.chat.history.HistoryInterceptingAiService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import io.quarkiverse.langchain4j.azure.openai.AzureOpenAiChatModel;
import io.quarkiverse.langchain4j.azure.openai.AzureOpenAiStreamingChatModel;
import io.quarkiverse.langchain4j.ollama.OllamaChatLanguageModel;
import io.quarkiverse.langchain4j.ollama.OllamaStreamingChatLanguageModel;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;

/**
 * Implementation of the {@link ChatModelProvider}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Startup // initialize at startup to validate config
@ApplicationScoped
class ChatModelProviderImpl implements ChatModelProvider {
    private static final Pattern UID_REGEX = Pattern.compile("[a-zA-z][a-zA-Z0-9_-]*");
    private static final String INITIALIZATION_FAILURE_MESSAGE = "Failed to initialize chat models";

    private final ChatModelConfig chatModelConfig;
    private final EnvironmentVariables env;
    private final ChatMemoryProvider chatMemoryProvider;
    private final ChatHistoryStore chatHistoryStore;
    private final RetrievalAugmentor retrievalAugmentor;

    private final Map<String, ChatModelContainer> chatModels = new HashMap<>();

    @Inject
    ChatModelProviderImpl(
            ChatModelConfig chatModelConfig,
            EnvironmentVariables env,
            ChatMemoryProvider chatMemoryProvider,
            ChatHistoryStore chatHistoryStore,
            RetrievalAugmentor retrievalAugmentor) {
        this.chatModelConfig = chatModelConfig;
        this.env = env;
        this.chatMemoryProvider = chatMemoryProvider;
        this.chatHistoryStore = chatHistoryStore;
        this.retrievalAugmentor = retrievalAugmentor;
        initializeChatModels();
    }

    private void initializeChatModels() {
        for (ChatModelConfig.ModelConfig // NOSONAR: we want to have more than a single "continue"
                // statement
                config : chatModelConfig.models()) {
            if (chatModels.containsKey(config.uid())) {
                Log.warn(String.format("Duplicate uid %s, skipping chat model.", config.uid()));
                continue;
            }
            if (!UID_REGEX.matcher(config.uid()).matches()) {
                Log.warn(String.format("Invalid uid %s, skipping chat model..", config.uid()));
                continue;
            }
            ChatLanguageModel clm = produceChatLanguageModel(config);
            StreamingChatLanguageModel sclm = produceStreamingChatLanguageModel(config);

            ChatModelAiService service =
                    new HistoryInterceptingAiService(
                            AiServices.builder(ChatModelAiService.class)
                                    .chatLanguageModel(clm)
                                    .streamingChatLanguageModel(sclm)
                                    .chatMemoryProvider(chatMemoryProvider)
                                    .retrievalAugmentor(retrievalAugmentor)
                                    .build(),
                            chatHistoryStore);

            ChatModelContainer cm =
                    new ChatModelContainer(
                            config.uid(),
                            config.label().orElse(config.uid()),
                            config.description().orElse(config.provider() + " " + config.model()),
                            service);
            chatModels.put(config.uid(), cm);
        }
    }

    private ChatLanguageModel produceChatLanguageModel(ChatModelConfig.ModelConfig config) {
        return switch (config.provider()) {
            case OPENAI -> OpenAiChatModel.builder()
                    .baseUrl(config.baseUrl().orElse(null))
                    .apiKey(env.getOpenaiApiKey())
                    .modelName(config.model())
                    .build();
            case OLLAMA -> {
                if (config.baseUrl().isEmpty()) {
                    throw new StartupException(
                            INITIALIZATION_FAILURE_MESSAGE,
                            new IllegalArgumentException(
                                    "Base URL is required for Ollama chat model."));
                }
                yield OllamaChatLanguageModel.builder()
                        .baseUrl(config.baseUrl().get()) // NOSONAR: we have checked for empty
                        .model(config.model())
                        .build();
            }
            case AZURE -> {
                String endpoint;
                try {
                    endpoint = buildAzureOpenaiEndpoint(config);
                } catch (IllegalArgumentException e) {
                    throw new StartupException(INITIALIZATION_FAILURE_MESSAGE, e);
                }
                yield AzureOpenAiChatModel.builder()
                        .endpoint(endpoint)
                        .apiKey(env.getAzureApiKey())
                        .apiVersion(AZURE_OPENAI_API_VERSION)
                        .build();
            }
        };
    }

    private StreamingChatLanguageModel produceStreamingChatLanguageModel(
            ChatModelConfig.ModelConfig config) {
        return switch (config.provider()) {
            case OPENAI -> OpenAiStreamingChatModel.builder()
                    .baseUrl(config.baseUrl().orElse(null))
                    .apiKey(env.getOpenaiApiKey())
                    .modelName(config.model())
                    .build();
            case OLLAMA -> {
                if (config.baseUrl().isEmpty()) {
                    throw new StartupException(
                            INITIALIZATION_FAILURE_MESSAGE,
                            new IllegalArgumentException(
                                    "Base URL is required for Ollama chat model."));
                }
                yield OllamaStreamingChatLanguageModel.builder()
                        .baseUrl(config.baseUrl().get()) // NOSONAR: we have checked for empty
                        .model(config.model())
                        .build();
            }
            case AZURE -> {
                String endpoint;
                try {
                    endpoint = buildAzureOpenaiEndpoint(config);
                } catch (IllegalArgumentException e) {
                    throw new StartupException(INITIALIZATION_FAILURE_MESSAGE, e);
                }
                yield AzureOpenAiStreamingChatModel.builder()
                        .endpoint(endpoint)
                        .apiKey(env.getAzureApiKey())
                        .apiVersion(AZURE_OPENAI_API_VERSION)
                        .build();
            }
        };
    }

    @Override
    public Collection<ChatModelContainer> getModels() {
        return Collections.unmodifiableCollection(chatModels.values());
    }

    @Override
    public ChatModelContainer getModel(String uid) throws ChatModelNotFoundException {
        ChatModelContainer cm = chatModels.get(uid);
        if (cm == null) {
            throw new ChatModelNotFoundException(uid);
        }
        return cm;
    }
}
