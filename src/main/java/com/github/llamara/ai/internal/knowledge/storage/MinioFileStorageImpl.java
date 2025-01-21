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

import com.github.llamara.ai.internal.StartupException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Item;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Implementation of the {@link FileStorage} using <a href="https://min.io/">MinIO</a> as the
 * storage backend.
 *
 * @author Florian Hotze - Initial contribution
 */
@Startup // initialize at startup to check connection
@ApplicationScoped
class MinioFileStorageImpl implements FileStorage {
    private static final String S3_EXCEPTION_MESSAGE = "Unexpected exception in S3 service";
    private static final String S3_ERROR_RESPONSE_MESSAGE =
            "Unexpected error response from S3 service";

    private final String bucketName;
    private final MinioClient minioClient;

    @Inject
    MinioFileStorageImpl(
            MinioClient minioClient,
            @ConfigProperty(name = "quarkus.minio.bucket-name") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;

        try {
            checkConnectionAndCreateBucketIfMissing();
        } catch (Exception e) {
            throw new StartupException("Failed to connect to MinIO object storage", e);
        }
    }

    private void checkConnectionAndCreateBucketIfMissing()
            throws UnexpectedFileStorageFailureException {
        try {
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (found) {
                return;
            }
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (InsufficientDataException
                | InternalException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | XmlParserException e) {
            throw new UnexpectedFileStorageFailureException(
                    "Unexpected exception thrown while creating bucket if missing", e);
        } catch (InvalidResponseException | IOException | ServerException e) {
            throw new UnexpectedFileStorageFailureException(S3_EXCEPTION_MESSAGE, e);
        } catch (ErrorResponseException e) {
            throw new UnexpectedFileStorageFailureException(S3_ERROR_RESPONSE_MESSAGE, e);
        }
    }

    @Override
    public void storeFile(String checksum, Path file, Map<String, String> metadata)
            throws UnexpectedFileStorageFailureException {
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(checksum)
                            .filename(file.toString())
                            .userMetadata(metadata)
                            .build());
        } catch (InsufficientDataException
                | InternalException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | XmlParserException e) {
            throw new UnexpectedFileStorageFailureException(
                    "Unexpected exception thrown while uploading file to bucket", e);
        } catch (InvalidResponseException | IOException | ServerException e) {
            throw new UnexpectedFileStorageFailureException(S3_EXCEPTION_MESSAGE, e);
        } catch (ErrorResponseException e) {
            throw new UnexpectedFileStorageFailureException(S3_ERROR_RESPONSE_MESSAGE, e);
        }
    }

    @Override
    public FileContainer getFile(String checksum)
            throws FileNotFoundException, UnexpectedFileStorageFailureException {
        try {
            Map<String, String> metadata =
                    minioClient
                            .statObject(
                                    StatObjectArgs.builder()
                                            .bucket(bucketName)
                                            .object(checksum)
                                            .build())
                            .userMetadata();
            InputStream inputStream =
                    minioClient.getObject(
                            GetObjectArgs.builder().bucket(bucketName).object(checksum).build());
            return new FileContainer(inputStream, metadata);
        } catch (InsufficientDataException
                | InternalException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | XmlParserException e) {
            throw new UnexpectedFileStorageFailureException(
                    "Unexpected exception thrown while deleting file from bucket", e);
        } catch (InvalidResponseException | IOException | ServerException e) {
            throw new UnexpectedFileStorageFailureException(S3_EXCEPTION_MESSAGE, e);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new FileNotFoundException(
                        String.format("File not found for checksum '%s'", checksum));
            }
            throw new UnexpectedFileStorageFailureException(S3_ERROR_RESPONSE_MESSAGE, e);
        }
    }

    @Override
    public void deleteFile(String checksum) throws UnexpectedFileStorageFailureException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(checksum).build());
        } catch (InsufficientDataException
                | InternalException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | XmlParserException e) {
            throw new UnexpectedFileStorageFailureException(
                    "Unexpected exception thrown while deleting file from bucket", e);
        } catch (InvalidResponseException | IOException | ServerException e) {
            throw new UnexpectedFileStorageFailureException(S3_EXCEPTION_MESSAGE, e);
        } catch (ErrorResponseException e) {
            throw new UnexpectedFileStorageFailureException(S3_ERROR_RESPONSE_MESSAGE, e);
        }
    }

    @Override
    public void deleteAllFiles() throws UnexpectedFileStorageFailureException {
        for (Result<Item> result :
                minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build())) {
            try {
                deleteFile(result.get().objectName());
            } catch (InsufficientDataException
                    | InternalException
                    | InvalidKeyException
                    | NoSuchAlgorithmException
                    | XmlParserException e) {
                throw new UnexpectedFileStorageFailureException(
                        "Unexpected exception thrown while deleting all files from bucket", e);
            } catch (InvalidResponseException | IOException | ServerException e) {
                throw new UnexpectedFileStorageFailureException(S3_EXCEPTION_MESSAGE, e);
            } catch (ErrorResponseException e) {
                throw new UnexpectedFileStorageFailureException(S3_ERROR_RESPONSE_MESSAGE, e);
            }
        }
    }
}
