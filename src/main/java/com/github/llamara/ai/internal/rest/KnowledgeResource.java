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

import com.github.llamara.ai.internal.knowledge.IllegalPermissionModificationException;
import com.github.llamara.ai.internal.knowledge.Knowledge;
import com.github.llamara.ai.internal.knowledge.KnowledgeManager;
import com.github.llamara.ai.internal.knowledge.KnowledgeNotFoundException;
import com.github.llamara.ai.internal.knowledge.storage.UnexpectedFileStorageFailureException;
import com.github.llamara.ai.internal.security.Permission;
import com.github.llamara.ai.internal.security.Roles;
import com.github.llamara.ai.internal.security.knowledge.UserKnowledgeManager;
import com.github.llamara.ai.internal.security.user.UserNotFoundException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.Cache;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * REST resource for the knowledge endpoint.
 *
 * @author Florian Hotze - Initial contribution
 */
@Path("/rest/knowledge")
class KnowledgeResource {
    private final UserKnowledgeManager knowledgeManager;

    @Inject
    KnowledgeResource(UserKnowledgeManager knowledgeManager) {
        this.knowledgeManager = knowledgeManager;
    }

    @ServerExceptionMapper
    Response handleIOException(IOException e) {
        return Response.status(
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "Failed to handle file upload.")
                .build();
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER, Roles.ANONYMOUS_USER})
    @Blocking
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAllKnowledge", summary = "Get all knowledge.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = Knowledge.class)))
    public Collection<Knowledge> getAllKnowledge() {
        return knowledgeManager.getAllKnowledge();
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER, Roles.ANONYMOUS_USER})
    @Blocking
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getKnowledge", summary = "Get a single knowledge.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Knowledge.class)))
    @APIResponse(responseCode = "404", description = "No knowledge with the given id found.")
    public Knowledge getKnowledge(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to get",
                            required = true)
                    UUID id)
            throws KnowledgeNotFoundException {
        return knowledgeManager.getKnowledge(id);
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER, Roles.ANONYMOUS_USER})
    @Blocking
    @GET
    @Path("/{id}/file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Cache(noTransform = true, mustRevalidate = true, noCache = true)
    @Operation(
            operationId = "getKnowledgeFile",
            summary = "Get the source file of a single knowledge.")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM))
    @APIResponse(responseCode = "404", description = "No knowledge with the given id found.")
    public Response getKnowledgeFile(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to get the source file of",
                            required = true)
                    UUID id,
            @HeaderParam(HttpHeaders.IF_NONE_MATCH) String ifNoneMatch)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        Knowledge knowledge = knowledgeManager.getKnowledge(id);
        if (knowledge.getChecksum().equals(ifNoneMatch)) {
            return Response.notModified().build();
        }

        KnowledgeManager.NamedFileContainer fileContainer = knowledgeManager.getFile(id);

        return Response.ok(fileContainer.content())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileContainer.fileName() + "\"")
                .header(HttpHeaders.ETAG, knowledge.getChecksum())
                .build();
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @DELETE
    @Path("/{id}")
    @ResponseStatus(200)
    @Operation(operationId = "deleteKnowledge", summary = "Delete a single knowledge.")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "404", description = "No knowledge with the given id found.")
    public void deleteKnowledge(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to delete",
                            required = true)
                    UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        knowledgeManager.deleteKnowledge(id);
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @POST
    @Path("/add/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "addFileSource",
            summary = "Add a set of files to the knowledge.",
            description = "If a file is empty, it is skipped.")
    @APIResponse(
            responseCode = "201",
            description = "OK. Returns the ids of the added knowledge.",
            content =
                    @Content(
                            schema = @Schema(type = SchemaType.ARRAY, implementation = UUID.class)))
    @APIResponse(responseCode = "400", description = "File upload is invalid.")
    public List<UUID> addKnowledge(
            @FormParam("files")
                    @Parameter(name = "files", description = "File(s) to upload", required = true)
                    List<FileUpload> files)
            throws IOException, UnexpectedFileStorageFailureException {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("File upload is invalid.");
        }
        List<UUID> ids = new ArrayList<>();
        for (FileUpload file : files) {
            String fileName =
                    Paths.get(file.fileName())
                            .getFileName()
                            .toString(); // sanitize file name to prevent path traversal
            // attacks

            try {
                UUID id =
                        knowledgeManager.addSource(
                                file.uploadedFile(), fileName, file.contentType());
                ids.add(id);
            } catch (IOException e) {
                Log.error("Error while uploading files to knowledge.", e);
                throw e;
            }
        }
        return ids;
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @PUT
    @Path("/update/{id}/file")
    @ResponseStatus(200)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
            operationId = "updateFileSource",
            summary = "Update the file source of a single knowledge identified by it ID.",
            description = "If the file is empty, it is skipped.")
    @APIResponse(responseCode = "200", description = "OK.")
    @APIResponse(responseCode = "400", description = "File upload is invalid.")
    @APIResponse(responseCode = "404", description = "No knowledge with the given id found.")
    public void updateKnowledge(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to update",
                            required = true)
                    UUID id,
            @FormParam("file")
                    @Parameter(name = "file", description = "File to upload", required = true)
                    FileUpload file)
            throws KnowledgeNotFoundException, IOException, UnexpectedFileStorageFailureException {
        if (file == null || file.fileName() == null) {
            throw new BadRequestException("File upload is invalid.");
        }
        String fileName =
                Paths.get(file.fileName())
                        .getFileName()
                        .toString(); // sanitize file name to prevent path traversal attacks

        try {
            knowledgeManager.updateSource(id, file.uploadedFile(), fileName, file.contentType());
        } catch (IOException e) {
            Log.error("Error while uploading files to knowledge.", e);
            throw e;
        }
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @PUT
    @Path("/{id}/permission/{username}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ResponseStatus(200)
    @Operation(
            operationId = "setKnowledgePermission",
            summary = "Set a user's permission for a single knowledge identified by its ID.")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "400", description = "Illegal permission modification")
    @APIResponse(
            responseCode = "404",
            description =
                    "No knowledge with the given id found or no user with the given username"
                            + " found.")
    public void setKnowledgePermission(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge set permission for",
                            required = true)
                    UUID id,
            @PathParam("username")
                    @Parameter(
                            name = "username",
                            description = "name of user to set permission for",
                            required = true)
                    String username,
            @RequestBody(
                            name = "permission",
                            description = "permission to set",
                            content =
                                    @Content(
                                            mediaType = MediaType.TEXT_PLAIN,
                                            schema =
                                                    @Schema(
                                                            enumeration = {
                                                                "READWRITE",
                                                                "READONLY"
                                                            })))
                    Permission permission)
            throws KnowledgeNotFoundException,
                    UserNotFoundException,
                    IllegalPermissionModificationException {
        knowledgeManager.setPermission(id, username, permission);
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @DELETE
    @Path("/{id}/permission/{username}")
    @ResponseStatus(200)
    @Operation(
            operationId = "removeKnowledgePermission",
            summary = "Remove a user's permissions from a single knowledge identified by its ID.")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(
            responseCode = "404",
            description =
                    "No knowledge with the given id found or no user with the given username"
                            + " found.")
    public void removeKnowledgePermissions(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to remove the permission from",
                            required = true)
                    UUID id,
            @PathParam("username")
                    @Parameter(
                            name = "username",
                            description = "name of user to remove permission for",
                            required = true)
                    String username)
            throws KnowledgeNotFoundException,
                    UserNotFoundException,
                    IllegalPermissionModificationException {
        knowledgeManager.removePermission(id, username);
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @PUT
    @Path("/{id}/label")
    @ResponseStatus(200)
    @Operation(
            operationId = "setKnowledgeLabel",
            summary = "Set the label of a single knowledge identified by its ID.")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "404", description = "No knowledge with the given id found.")
    public void setKnowledgeLabel(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to set the label for",
                            required = true)
                    UUID id,
            @QueryParam("label")
                    @Parameter(
                            name = "label",
                            description = "the knowledge label to set",
                            required = true)
                    String label)
            throws KnowledgeNotFoundException {
        knowledgeManager.setLabel(id, label);
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @PUT
    @Path("/{id}/tag")
    @ResponseStatus(200)
    @Operation(
            operationId = "addKnowledgeTag",
            summary = "Add a tag to a single knowledge identified by its ID.")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "404", description = "No knowledge with the given id found.")
    public void addKnowledgeTag(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to which the tag should be added",
                            required = true)
                    UUID id,
            @QueryParam("tag") @Parameter(name = "tag", description = "tag to add", required = true)
                    String tag)
            throws KnowledgeNotFoundException {
        knowledgeManager.addTag(id, tag);
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @DELETE
    @Path("/{id}/tag")
    @ResponseStatus(200)
    @Operation(
            operationId = "removeKnowledgeTag",
            summary = "Remove a tag from a single knowledge identified by its ID.")
    @APIResponse(responseCode = "200", description = "OK")
    @APIResponse(responseCode = "404", description = "No knowledge with the given id found.")
    public void removeKnowledgeTag(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to which the tag should be removed",
                            required = true)
                    UUID id,
            @QueryParam("tag")
                    @Parameter(name = "tag", description = "tag to remove", required = true)
                    String tag)
            throws KnowledgeNotFoundException {
        knowledgeManager.removeTag(id, tag);
    }

    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Blocking
    @PUT
    @Path("/retry/{id}/ingestion")
    @ResponseStatus(200)
    @Operation(
            operationId = "retryFailedIngestion",
            summary = "Retry the failed ingestion of a single knowledge identified by it ID.")
    @APIResponse(responseCode = "200", description = "OK.")
    @APIResponse(responseCode = "404", description = "No knowledge with the given id found.")
    public void retryFailedIngestion(
            @PathParam("id")
                    @Parameter(
                            name = "id",
                            description = "UID of the knowledge to retry the ingestion for",
                            required = true)
                    UUID id)
            throws KnowledgeNotFoundException, UnexpectedFileStorageFailureException {
        knowledgeManager.retryFailedIngestion(id);
    }
}
