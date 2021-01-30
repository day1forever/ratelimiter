package com.ratelimiter.utils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class BaseExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    @Override
    public Response toResponse(T exception) {
        return toRenderableError(exception).toResponse();
    }

    protected abstract RenderableError toRenderableError(T exception);

}
