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
package com.github.llamara.ai.internal.chat;

import com.github.llamara.ai.config.chat.ChatModelConfig;
import com.github.llamara.ai.internal.MetadataKeys;
import com.github.llamara.ai.internal.chat.history.ChatHistoryStore;
import com.github.llamara.ai.internal.chat.history.ChatMessageRecord;
import com.github.llamara.ai.internal.chat.response.ChatResponseRecord;
import com.github.llamara.ai.internal.chat.response.RagSourceRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.DefaultContent;
import dev.langchain4j.service.Result;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ChatModel}. */
@QuarkusTest
public class ChatModelTest {
    private static final String MODEL_UID = "gpt-4o";

    ChatModelConfig.ModelConfig modelConfig = mock(ChatModelConfig.ModelConfig.class);
    AiService aiService = mock(AiService.class);
    @InjectSpy ChatHistoryStore chatHistoryStore;

    ChatModel model;
    UUID sessionId;

    @BeforeEach
    void setup() {
        when(modelConfig.uid()).thenReturn(MODEL_UID);

        model = new ChatModel(modelConfig, aiService, chatHistoryStore);
        sessionId = UUID.randomUUID();

        clearInvocations(chatHistoryStore);
    }

    @AfterEach
    void destroy() {
        chatHistoryStore.deleteMessages(sessionId).await().indefinitely();

        sessionId = null;
        model = null;
    }

    void setupAiServiceChat(String prompt, String response) {
        Result<String> result =
                Result.<String>builder().content(response).sources(Collections.emptyList()).build();
        when(aiService.chat(sessionId, prompt)).thenReturn(result);
        when(aiService.chatWithoutSystemMessage(sessionId, prompt)).thenReturn(result);
    }

    @Nested
    class WithoutSources {
        private static final String PROMPT = "Hello world!";
        private static final String RESPONSE = "Hi!";

        @BeforeEach
        void setup() {
            Result<String> result =
                    Result.<String>builder()
                            .content(RESPONSE)
                            .sources(Collections.emptyList())
                            .build();
            when(aiService.chat(sessionId, PROMPT)).thenReturn(result);
            when(aiService.chatWithoutSystemMessage(sessionId, PROMPT)).thenReturn(result);
        }

        @Test
        void chatDelegatesToAiServiceAndUsesSystemPromptIfEnabled() {
            // given
            when(modelConfig.systemPromptEnabled()).thenReturn(true);

            // when
            ChatResponseRecord response = model.chat(sessionId, true, PROMPT);

            // then
            assertEquals(RESPONSE, response.response());
            verify(aiService).chat(sessionId, PROMPT);
            verify(aiService, never()).chatWithoutSystemMessage(eq(sessionId), any());
        }

        @Test
        void chatDelegatesToAiServiceAndDoesNotUseSystemPromptIfDisabled() {
            // given
            when(modelConfig.systemPromptEnabled()).thenReturn(false);

            // when
            ChatResponseRecord response = model.chat(sessionId, false, PROMPT);

            // then
            assertEquals(RESPONSE, response.response());
            verify(aiService).chatWithoutSystemMessage(sessionId, PROMPT);
            verify(aiService, never()).chat(eq(sessionId), any());
        }

        @Test
        void chatDoesNotProvideSources() {
            // when
            ChatResponseRecord response = model.chat(sessionId, true, PROMPT);

            // then
            assertEquals(0, response.sources().size());
        }

        @Test
        void chatStoresPromptAndResponseToHistoryIfEnabled() {
            // when
            model.chat(sessionId, true, PROMPT);

            // then
            verify(chatHistoryStore, times(2)).addMessage(eq(sessionId), any());
            List<ChatMessageRecord> history =
                    chatHistoryStore.getMessages(sessionId).await().indefinitely();
            assertEquals(2, history.size());
            ChatMessageRecord promptRecord = history.get(0);
            assertEquals(ChatMessageType.USER, promptRecord.type());
            assertEquals(PROMPT, promptRecord.text());
            ChatMessageRecord responseRecord = history.get(1);
            assertEquals(ChatMessageType.AI, responseRecord.type());
            assertEquals(RESPONSE, responseRecord.text());
        }

        @Test
        void chatDoesNotStorePromptAndResponseToHistoryIfDisabled() {
            // when
            model.chat(sessionId, false, PROMPT);

            // then
            verify(chatHistoryStore, never()).addMessage(any(), any());
        }
    }

    @Nested
    class WithSources {
        private static final UUID USED_KNOWLEDGE_ID = UUID.randomUUID();
        private static final Metadata TEXT_SEGMENT_METADATA =
                Metadata.from(Map.of("knowledge_id", USED_KNOWLEDGE_ID));
        private static final UUID USED_EMBEDDING_ID = UUID.randomUUID();
        private static final String USED_EMBEDDING = "Text of used embedding.";
        private static final Content USED_CONTENT =
                new DefaultContent(
                        TextSegment.from(USED_EMBEDDING, TEXT_SEGMENT_METADATA),
                        Map.of(ContentMetadata.EMBEDDING_ID, USED_EMBEDDING_ID));
        private static final UUID UNUSED_EMBEDDING_ID = UUID.randomUUID();
        private static final String UNUSED_EMBEDDING_TEXT = "Text of unused embedding.";
        private static final Content UNUSED_CONTENT =
                new DefaultContent(
                        TextSegment.from(UNUSED_EMBEDDING_TEXT, TEXT_SEGMENT_METADATA),
                        Map.of(ContentMetadata.EMBEDDING_ID, UNUSED_EMBEDDING_ID));

        private static final UUID UNUSED_KNOWLEDGE_ID = UUID.randomUUID();
        private static final Content UNUSED_KNOWLEDGE_CONTENT =
                new DefaultContent(
                        TextSegment.from(
                                "Text of unused knowledge.",
                                Metadata.from(
                                        Map.of(MetadataKeys.KNOWLEDGE_ID, UNUSED_KNOWLEDGE_ID))),
                        Map.of(ContentMetadata.EMBEDDING_ID, UUID.randomUUID()));

        private static final String PROMPT = "Hello retrieval augmented generation!";
        private static final String RESPONSE =
                String.format(
                        "Response with sources. { knowledge_id: \"%s\", embedding_id: \"%s\" }",
                        USED_KNOWLEDGE_ID, USED_EMBEDDING_ID);

        @BeforeEach
        void setup() {
            Result<String> result =
                    Result.<String>builder()
                            .content(RESPONSE)
                            .sources(
                                    List.of(USED_CONTENT, UNUSED_CONTENT, UNUSED_KNOWLEDGE_CONTENT))
                            .build();
            when(aiService.chat(sessionId, PROMPT)).thenReturn(result);
            when(aiService.chatWithoutSystemMessage(sessionId, PROMPT)).thenReturn(result);
        }

        @Test
        void chatProvidesOnlySourcesWhoseEmbeddingIdIsInResponse() {
            // when
            ChatResponseRecord response = model.chat(sessionId, true, PROMPT);

            // then
            assertTrue(
                    response.sources().stream()
                            .filter(s -> s.embeddingId().equals(UNUSED_EMBEDDING_ID))
                            .findFirst()
                            .isEmpty());
            Optional<RagSourceRecord> usedSource =
                    response.sources().stream()
                            .filter(s -> s.embeddingId().equals(USED_EMBEDDING_ID))
                            .findFirst();
            assertTrue(usedSource.isPresent());
            assertEquals(USED_EMBEDDING_ID, usedSource.get().embeddingId());
            assertEquals(USED_EMBEDDING, usedSource.get().content());
        }

        @Test
        void chatProvidesOnlySourcesWhoseKnowledgeIdIsInResponse() {
            // when
            ChatResponseRecord response = model.chat(sessionId, true, PROMPT);

            // then
            assertTrue(
                    response.sources().stream()
                            .filter(s -> s.knowledgeId().equals(UNUSED_KNOWLEDGE_ID))
                            .findFirst()
                            .isEmpty());
        }
    }
}
