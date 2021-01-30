package com.ratelimiter.utils;

import lombok.extern.slf4j.Slf4j;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Slf4j
public class RenderableExceptionCatchAllMapper extends BaseExceptionMapper<RenderableException> {
    @Override
    protected RenderableError toRenderableError(RenderableException exception) {
        if (exception.getStatus().getFamily() == Response.Status.Family.SERVER_ERROR) {
            // Only log errors for server errors
            log.error( exception.getMessage(), exception);
        } else {
            //We want to log something anyways
            log.warn(exception.getMessage(), exception);
        }

        return RenderableError.builder().status(exception.getStatus()).build();
    }
}