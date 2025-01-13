package com.github.llamara.ai.internal.internal;

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
