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
package com.github.llamara.ai.internal.knowledge.storage;

import com.github.llamara.ai.config.EnvironmentVariables;
import com.github.llamara.ai.config.FileStorageConfig;
import com.github.llamara.ai.internal.StartupException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

import io.minio.MinioClient;
import io.minio.http.HttpUtils;
import io.quarkus.runtime.Startup;

/**
 * CDI Bean Producer for {@link FileStorage}. It produces the bean based on the {@link
 * FileStorageConfig}.
 *
 * @author Florian Hotze - Initial contribution
 */
@ApplicationScoped
class FileStorageProducer {
    private final FileStorageConfig config;
    private final EnvironmentVariables env;
    private final io.vertx.core.Vertx vertx;
    private final io.vertx.mutiny.core.Vertx reactiveVertx;

    @Inject
    FileStorageProducer(
            FileStorageConfig config,
            EnvironmentVariables env,
            io.vertx.core.Vertx vertx,
            io.vertx.mutiny.core.Vertx reactiveVertx) {
        this.config = config;
        this.env = env;
        this.vertx = vertx;
        this.reactiveVertx = reactiveVertx;
    }

    @Startup // create bean at startup to check connection
    @Produces
    @ApplicationScoped
    FileStorage produceFileStorage() {
        return switch (config.type()) {
            case FS -> produceFSFileStorage();
            case MINIO -> produceMinioFileStorage();
        };
    }

    private FSFileStorageImpl produceFSFileStorage() {
        if (config.path().isEmpty()) {
            throw new StartupException("File storage path is required but missing.");
        }
        return new FSFileStorageImpl(
                config.path().get(), vertx, reactiveVertx); // NOSONAR: we have checked for empty
    }

    private MinioFileStorageImpl produceMinioFileStorage() {
        if (config.url().isEmpty()) {
            throw new StartupException("MinIO URL is required but missing.");
        }
        String url = config.url().get(); // NOSONAR: we have checked for empty

        MinioClient.Builder builder = MinioClient.builder();
        config.port()
                .ifPresentOrElse(
                        port -> builder.endpoint(url, port, config.secure()),
                        () ->
                                builder.endpoint(
                                        HttpUtils.getBaseUrl(url)
                                                .newBuilder()
                                                .scheme(config.secure() ? "https" : "http")
                                                .build()));
        builder.credentials(env.getMinioAccessKey(), env.getMinioSecretKey());
        return new MinioFileStorageImpl(builder.build(), config.bucketName());
    }
}
