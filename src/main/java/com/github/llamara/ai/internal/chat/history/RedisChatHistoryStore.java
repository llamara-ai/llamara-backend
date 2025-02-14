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
package com.github.llamara.ai.internal.chat.history;

import com.github.llamara.ai.config.chat.ChatHistoryConfig;
import com.github.llamara.ai.internal.StartupException;

import java.util.List;
import java.util.concurrent.CompletionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;

/**
 * Implementation of {@link ChatHistoryStore} using <a href="https://redis.io/json/">Redis</a>.
 *
 * @author Florian Hotze - Initial contribution
 */
@Startup // initialize at startup to check connection
@ApplicationScoped
class RedisChatHistoryStore implements ChatHistoryStore {
    private final ChatHistoryConfig config;
    private final ReactiveKeyCommands<String> keyCommands;
    private final ReactiveListCommands<String, ChatMessageRecord> listCommands;

    @Inject
    RedisChatHistoryStore(
            ChatHistoryConfig config,
            @RedisClientName("chat-history") ReactiveRedisDataSource redis) {
        this.config = config;
        this.keyCommands = redis.key(String.class);
        this.listCommands = redis.list(ChatMessageRecord.class);

        // Check connection
        try {
            keyCommands.randomkey().await().indefinitely();
        } catch (CompletionException e) {
            throw new StartupException("Failed to connect to Redis chat history DB", e.getCause());
        }
    }

    @Override
    public Uni<List<ChatMessageRecord>> getMessages(Object historyId) {
        return listCommands.lrange(historyId.toString(), 0, -1);
    }

    @Override
    public Uni<Void> addMessage(Object historyId, ChatMessageRecord message) {
        Log.debugf("Adding message to history for session '%s'.", historyId);
        return listCommands
                .rpush(historyId.toString(), message)
                .chain(
                        () ->
                                listCommands.ltrim(
                                        historyId.toString(), -1 * (long) config.maxMessages(), -1))
                .onFailure()
                .invoke(
                        failure ->
                                Log.errorf(
                                        "Failed to add message to history for '%s'.",
                                        historyId, failure));
    }

    @Override
    public Uni<Void> deleteMessages(Object memoryId) {
        Log.debugf("Deleting history for session '%s'.", memoryId);
        return keyCommands.del(memoryId.toString()).replaceWithVoid();
    }
}
