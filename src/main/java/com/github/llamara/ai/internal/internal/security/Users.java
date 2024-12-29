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
package com.github.llamara.ai.internal.internal.security;

import com.github.llamara.ai.internal.internal.security.user.User;

/**
 * Constant class for users.
 *
 * @author Florian Hotze - Initial contribution
 */
public final class Users {
    private Users() {}

    public static final String ANY_USERNAME = "*";
    public static final User ANY;

    static {
        ANY = new User(ANY_USERNAME);
    }
}
