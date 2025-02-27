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
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for {@link FrontendForwardingFilter}. */
class FrontendForwardingFilterTest {
    @Test
    void doFilterDoesNothingIfNo404() throws ServletException, IOException {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        FrontendForwardingFilter filter = new FrontendForwardingFilter();

        when(response.getStatus()).thenReturn(200);

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(response, never()).reset();
    }

    @ParameterizedTest
    @CsvSource({"/rest/chat/sessions", "/rest/chat/models", "/q/info", "/q/health"})
    void doFilterDoesNothingIf404inRestOrQuarkusPath(String path)
            throws ServletException, IOException {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        FrontendForwardingFilter filter = new FrontendForwardingFilter();

        when(request.getRequestURI()).thenReturn(path);
        when(response.getStatus()).thenReturn(404);

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(response, never()).reset();
    }

    @ParameterizedTest
    @CsvSource({"/sessions", "/knowledge"})
    void doFilterRewrites404inUiPath(String path) throws ServletException, IOException {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        FrontendForwardingFilter filter = new FrontendForwardingFilter();
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);

        when(request.getRequestURI()).thenReturn(path);
        when(request.getRequestDispatcher("/")).thenReturn(requestDispatcher);
        when(response.getStatus()).thenReturn(404);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        // when
        filter.doFilter(request, response, chain);

        // then
        verify(response, times(1)).reset();
        verify(response, times(1)).setStatus(200);
        verify(response, times(1)).setContentType(MediaType.TEXT_HTML);
        verify(requestDispatcher, times(1)).forward(request, response);
    }
}
