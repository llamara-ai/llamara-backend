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
package com.github.llamara.ai.internal.security;

import com.github.llamara.ai.internal.MetadataKeys;
import com.github.llamara.ai.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.security.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Mapper for converting between {@link Knowledge} permissions, {@link MetadataKeys#PERMISSION}
 * metadata and usernames of users with read permission.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class PermissionMetadataMapper {
    public static final String DELIMITER = "|";

    private PermissionMetadataMapper() {}

    /**
     * Convert {@link Knowledge} permissions to the {@link MetadataKeys#PERMISSION} metadata entry.
     *
     * @param permissions the permissions to convert
     * @return the metadata entry
     */
    public static String permissionsToMetadataEntry(Map<User, Permission> permissions) {
        String inner =
                permissions.entrySet().stream()
                        .filter(entry -> entry.getValue() != Permission.NONE)
                        .map(Map.Entry::getKey)
                        .map(User::getUsername)
                        .collect(Collectors.joining(DELIMITER));
        if (inner.isEmpty()) {
            return "";
        }
        return DELIMITER + inner + DELIMITER;
    }

    /**
     * Convert a {@link SecurityIdentity} to the query strings to check the {@link
     * MetadataKeys#PERMISSION} metadata entry for.
     *
     * @param identity the identity to convert
     * @return the query strings
     */
    public static Collection<String> identityToMetadataQueries(SecurityIdentity identity) {
        String anyQuery = DELIMITER + Users.ANY_USERNAME + DELIMITER;
        if (identity.isAnonymous()) {
            return List.of(anyQuery);
        }
        String userQuery = DELIMITER + identity.getPrincipal().getName() + DELIMITER;
        return List.of(userQuery, anyQuery);
    }
}
