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
package com.github.llamara.ai.internal.config;

import com.github.llamara.ai.internal.internal.security.session.UserSessionManager;
import io.smallrye.config.ConfigMapping;

/**
 * Provides configuration for the {@link UserSessionManager}.
 *
 * @author Florian Hotze - Initial contribution
 */
@ConfigMapping(prefix = "security")
public interface UserSecurityConfig {

    boolean anonymousUserEnabled();

    int anonymousUserSessionTimeout();
}
