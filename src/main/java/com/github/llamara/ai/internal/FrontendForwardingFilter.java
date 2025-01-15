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
package com.github.llamara.ai.internal;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

/**
 * A Jakarta Servlet {@link HttpFilter} that provides routes forwarding for JavaScript frontends.
 * This filter redirects all 404 errors outside the {@link FrontendForwardingFilter#REST_ROOT_PATH}
 * and {@link FrontendForwardingFilter#QUARKUS_ROOT_PATH} to the index page, which allows the SPA's
 * router to take over.
 *
 * @author Florian Hotze - Initial contribution
 */
@WebFilter("/*")
class FrontendForwardingFilter extends HttpFilter {
    private static final String REST_ROOT_PATH = "/rest"; // NOSONAR: this is not customizable
    private static final String QUARKUS_ROOT_PATH = "/q"; // NOSONAR: this is not customizable

    @Override
    protected void doFilter(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        super.doFilter(request, response, chain);

        if (response.getStatus() != 404) {
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith(REST_ROOT_PATH) || path.startsWith(QUARKUS_ROOT_PATH)) {
            return;
        }

        try {
            response.reset();
            response.setStatus(200);
            response.setContentType(MediaType.TEXT_HTML);
            request.getRequestDispatcher("/").forward(request, response);
        } finally {
            response.getOutputStream().close();
        }
    }
}
