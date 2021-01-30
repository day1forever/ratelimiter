package com.ratelimiter.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Value
@Builder
public class RenderableError {

    public static class RenderableErrorBuilder {
        public RenderableErrorBuilder status(Response.Status status) {
            return status(status);
        }
    }

    @JsonIgnore
    @NonNull
    Response.Status status;

    public Response toResponse() {
        return Response.status(status).entity(this).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
