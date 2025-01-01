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
package com.github.llamara.ai.internal.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.test.Mock;
import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.Config;

public class UserSecurityConfigMockProducer {
    @Inject Config config;

    @Produces
    @ApplicationScoped
    @Mock
    UserSecurityConfig userSecurityConfig() {
        return config.unwrap(SmallRyeConfig.class).getConfigMapping(UserSecurityConfig.class);
    }
}
