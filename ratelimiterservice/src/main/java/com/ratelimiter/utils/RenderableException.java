package com.ratelimiter.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;

import javax.ws.rs.core.Response;
import java.util.Arrays;

public final class RenderableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    @JsonProperty("code")
    @NonNull
    private final Response.Status status;

    @JsonProperty("message")
    public String getMessage() {
        return super.getMessage();
    }

    public RenderableException(Response.Status status, String formatString, Object... args) {
        super(computeMessage(formatString, args), computeCause(args));
        this.status = status;
    }

    private static String computeMessage(String formatString, Object... args) {
        if (args.length == 0) {
            return formatString;
        } else {
            Object last = args[args.length - 1];
            return last instanceof Throwable ?
                    String.format(formatString, Arrays.copyOf(args, args.length - 1)) :
                    String.format(formatString, args);
        }
    }

    private static Throwable computeCause(Object... args) {
        if (args.length == 0) {
            return null;
        } else {
            Object last = args[args.length - 1];
            return last instanceof Throwable ? (Throwable)last : null;
        }
    }

    private RenderableException() {
        this.status = null;
    }

    @NonNull
    public Response.Status getStatus() {
        return this.status;
    }

    public String toString() {
        return "RenderableException(status=" + this.getStatus() + ")";
    }

    protected boolean canEqual(Object other) {
        return other instanceof RenderableException;
    }
}
