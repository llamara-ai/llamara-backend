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

import com.github.llamara.ai.internal.security.user.User;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for {@link PermissionMetadataMapper}. */
class PermissionMetadataMapperTest {
    private static final User USER = new User("user");

    @ParameterizedTest
    @MethodSource("provideArgumentsForPermissionsToMetadataEntryConverts")
    void permissionsToMetadataEntryConvertsReadonlyAndAbovePermissions(Permission permission) {
        // given
        String expected =
                PermissionMetadataMapper.DELIMITER
                        + USER.getUsername()
                        + PermissionMetadataMapper.DELIMITER;

        // test
        String metadataEntry =
                PermissionMetadataMapper.permissionsToMetadataEntry(Map.of(USER, permission));
        assertEquals(expected, metadataEntry);
    }

    private static Stream<Arguments> provideArgumentsForPermissionsToMetadataEntryConverts() {
        return Stream.of(
                Arguments.of(Permission.READONLY),
                Arguments.of(Permission.READWRITE),
                Arguments.of(Permission.OWNER));
    }

    @Test
    void permissionsToMetadataEntryDoesConvertMultiplePermissions() {
        // given
        Map<User, Permission> permissions =
                Map.of(USER, Permission.READONLY, new User("user2"), Permission.READWRITE);
        String expected1 =
                PermissionMetadataMapper.DELIMITER
                        + USER.getUsername()
                        + PermissionMetadataMapper.DELIMITER;
        String expected2 =
                PermissionMetadataMapper.DELIMITER + "user2" + PermissionMetadataMapper.DELIMITER;

        // test
        String metadataEntry = PermissionMetadataMapper.permissionsToMetadataEntry(permissions);
        assertTrue(metadataEntry.contains(expected1));
        assertTrue(metadataEntry.contains(expected2));
    }

    @Test
    void permissionsToMetadataEntryDoesNotConvertNonePermission() {
        String metadataEntry =
                PermissionMetadataMapper.permissionsToMetadataEntry(Map.of(USER, Permission.NONE));
        assertTrue(metadataEntry.isEmpty());
    }

    @Test
    void identityToMetadataQueryReturnsCorrectQueryStringForAuthenticatedIdentity() {
        // given
        SecurityIdentity identity =
                QuarkusSecurityIdentity.builder()
                        .setAnonymous(false)
                        .setPrincipal(USER::getUsername)
                        .addRole(Roles.USER)
                        .build();
        String expected =
                PermissionMetadataMapper.DELIMITER
                        + USER.getUsername()
                        + PermissionMetadataMapper.DELIMITER;

        // test
        String query = PermissionMetadataMapper.identityToMetadataQuery(identity);
        assertEquals(expected, query);
    }

    @Test
    void identityToMetadataQueryReturnsCorrectQueryStringForAnonymousIdentity() {
        // given
        SecurityIdentity identity =
                QuarkusSecurityIdentity.builder()
                        .setAnonymous(true)
                        .addRole(Roles.ANONYMOUS_USER)
                        .build();
        String expected =
                PermissionMetadataMapper.DELIMITER
                        + Users.ANY_USERNAME
                        + PermissionMetadataMapper.DELIMITER;

        // test
        String query = PermissionMetadataMapper.identityToMetadataQuery(identity);
        assertEquals(expected, query);
    }
}
