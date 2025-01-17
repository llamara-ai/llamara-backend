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
package com.github.llamara.ai.internal.rest;

import com.github.llamara.ai.internal.chat.ChatModelContainer;
import com.github.llamara.ai.internal.chat.ChatModelNotFoundException;
import com.github.llamara.ai.internal.chat.ChatModelProvider;
import com.github.llamara.ai.internal.chat.aiservice.ChatModelAiService;
import com.github.llamara.ai.internal.chat.history.ChatMessageRecord;
import com.github.llamara.ai.internal.security.Roles;
import com.github.llamara.ai.internal.security.session.Session;
import com.github.llamara.ai.internal.security.session.SessionManager;
import com.github.llamara.ai.internal.security.session.SessionNotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * REST resource for the chat endpoint.
 *
 * @author Florian Hotze - Initial contribution
 */
@RolesAllowed({Roles.ADMIN, Roles.USER, Roles.ANONYMOUS_USER})
@Path("/rest/chat")
@APIResponse(
        responseCode = "400",
        description =
                "Bad Request, usually returned when an operation is requested before the user has"
                        + " logged in.")
class ChatResource {
    private final SessionManager sessionManager;
    private final ChatModelProvider chatModelProvider;
    private final SecurityIdentity identity;

    @Inject
    ChatResource(
            SessionManager sessionManager,
            ChatModelProvider chatModelProvider,
            SecurityIdentity identity) {
        this.sessionManager = sessionManager;
        this.chatModelProvider = chatModelProvider;
        this.identity = identity;
    }

    @NonBlocking
    @GET
    @Path("/models")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getModels", summary = "Get the available chat models.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = ChatModelContainer.class)))
    public Collection<ChatModelContainer> getModels() {
        return chatModelProvider.getModels();
    }

    @Blocking
    @POST
    @Path("/prompt")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "prompt", summary = "Send a prompt to the given chat model.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = String.class)))
    @APIResponse(
            responseCode = "404",
            description = "No chat model or no session with given ID found.")
    public String chat(
            @QueryParam("uid")
                    @Parameter(
                            name = "uid",
                            description = "UID of the chat model to use",
                            required = true)
                    String uid,
            @QueryParam("sessionId")
                    @Parameter(
                            name = "sessionId",
                            description = "ID of the session to use",
                            required = true)
                    UUID sessionId,
            String prompt)
            throws ChatModelNotFoundException, SessionNotFoundException {
        sessionManager.enforceSessionValid(sessionId);
        ChatModelAiService chatModelAiService = chatModelProvider.getModel(uid).service();
        return chatModelAiService.chat(sessionId, !identity.isAnonymous(), prompt);
    }

    /*
    // Note: This method is currently broken when using blocking chat memory and/or blocking
    // retrieval augmentor.
    @Blocking
    @POST
    @Path("/prompt/stream")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(
            operationId = "sendPromptStreamResponse",
            summary = "Send a prompt to the given chat model.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            mediaType = MediaType.SERVER_SENT_EVENTS,
                            schema = @Schema(implementation = String.class)))
    @APIResponse(
            responseCode = "404",
            description = "No chat model or no session with given ID found.")
    public Multi<String> streamingPrompt(
            @QueryParam("uid")
                    @Parameter(
                            name = "uid",
                            description = "UID of the chat model to use",
                            required = true)
                    String uid,
            @QueryParam("sessionId")
                    @Parameter(
                            name = "sessionId",
                            description = "ID of the session to use",
                            required = true)
                    UUID sessionId,
            String prompt)
            throws ChatModelNotFoundException, SessionNotFoundException {
        sessionManager.enforceSessionValid(sessionId);
        ChatModelAiService chatModelAiService = chatModelProvider.getModel(uid).service();
        Multi<String> sourceMulti =
                Multi.createFrom()
                        .emitter(
                                emitter ->
                                        chatModelAiService
                                                .chatAndStreamResponse(
                                                        sessionId, !identity.isAnonymous(), prompt)
                                                .onNext(emitter::emit)
                                                .onError(emitter::fail)
                                                .onComplete(response -> emitter.complete())
                                                .start());

        return RestMulti.fromMultiData(sourceMulti).withDemand(1).status(200).build();
    }
    */

    @Blocking
    @GET
    @Path("/sessions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getSessions", summary = "Get all chat sessions.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = Session.class)))
    public Collection<Session> getSessions() {
        return sessionManager.getSessions();
    }

    @Blocking
    @PUT
    @Path("/sessions/create")
    @ResponseStatus(201)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "createSession", summary = "Create a new chat session and get its ID.")
    @APIResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(schema = @Schema(implementation = Session.class)))
    public Session createSession() {
        return sessionManager.createSession();
    }

    @Blocking
    @DELETE
    @Path("/sessions/{sessionId}")
    @ResponseStatus(200)
    @Operation(operationId = "deleteSession", summary = "Delete a chat session.")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "404", description = "No session with the given ID found.")
    public void deleteSession(@PathParam("sessionId") UUID sessionId)
            throws SessionNotFoundException {
        sessionManager.deleteSession(sessionId);
    }

    @Blocking
    @GET
    @Path("/sessions/{sessionId}/history")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "getHistory",
            summary = "Get the chat history for the given session id.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = ChatMessageRecord.class)))
    @APIResponse(responseCode = "404", description = "No session with the given ID found.")
    public Uni<List<ChatMessageRecord>> getHistory(
            @PathParam("sessionId")
                    @Parameter(
                            name = "sessionId",
                            description = "UID of the chat history to get",
                            required = true)
                    UUID sessionId)
            throws SessionNotFoundException {
        return sessionManager.getChatHistory(sessionId);
    }
}
