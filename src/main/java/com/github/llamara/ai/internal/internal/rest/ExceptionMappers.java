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
package com.github.llamara.ai.internal.internal.rest;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import com.github.llamara.ai.internal.internal.chat.ChatModelNotFoundException;
import com.github.llamara.ai.internal.internal.knowledge.EmptyFileException;
import com.github.llamara.ai.internal.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.internal.security.session.SessionNotFoundException;
import com.github.llamara.ai.internal.internal.security.user.UserNotRegisteredException;
import io.quarkus.logging.Log;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Global exception mappers for the REST resources.
 *
 * @author Florian Hotze - Initial contribution
 */
class ExceptionMappers {

    @ServerExceptionMapper
    Response handleChatModelNotFoundException(ChatModelNotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "Chat model not found.")
                .build();
    }

    @ServerExceptionMapper
    Response handleKnowledgeNotFoundException(KnowledgeNotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "Knowledge not found.")
                .build();
    }

    @ServerExceptionMapper
    Response handleEmptyFileException(EmptyFileException e) {
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), "Empty file.").build();
    }

    @ServerExceptionMapper
    Response handleSessionNotFoundException(SessionNotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND.getStatusCode(), "Session not found.")
                .build();
    }

    @ServerExceptionMapper
    Response handleUserNotLoggedInException(UserNotRegisteredException e) {
        return Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "User not logged in.")
                .build();
    }

    /**
     * Restore default handling of {@link WebApplicationException}s like {@link NotFoundException}
     * and {@BadRequestException}.
     *
     * @param e
     * @return
     */
    @ServerExceptionMapper
    Response handleWebApplicationException(WebApplicationException e) {
        return Response.status(e.getResponse().getStatus(), e.getMessage()).build();
    }

    /**
     * Handle all other exceptions. Avoid that the server returns a stack trace to the client.
     *
     * @param e
     * @return
     */
    @ServerExceptionMapper
    Response handleException(Exception e) {
        Log.error("Failed to process request: ", e);
        return Response.status(
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "Internal server error.")
                .build();
    }
}
