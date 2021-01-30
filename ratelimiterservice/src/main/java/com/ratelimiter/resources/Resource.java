package com.ratelimiter.resources;

import com.ratelimiter.dao.LimitDao;
import com.ratelimiter.mode.Limit;
import com.ratelimiter.utils.RenderableException;
import com.ratelimiter.utils.ResourceInputValidationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


// TODO: should add metrics for critical path, like catch
// TODO: add authentication
// TODO: check if apiName is whitelisted in throttle request and create user limit info
@Slf4j
@Singleton
@Path("/v20210131")
@Produces({ "application/json" })
public class Resource {
    private final ResourceInputValidationUtil validationUtil;
    private final LimitDao limitDao;

    @Inject
    public Resource(LimitDao limitDao) {
        this.limitDao = limitDao;
        validationUtil = new ResourceInputValidationUtil();
    }

    @GET
    @Path("/limit-info/{userId}/{apiName}")
    @Produces({ "application/json" })
    public Limit getLimitInfo(@Valid @NotNull @PathParam("userId") String userId,
                               @Valid @NotNull @PathParam("apiName") String apiName) throws Exception {
        validationUtil.validateApiName(apiName);

        Optional<Limit> userLimit = limitDao.getLimitInfo(userId, apiName);

        if (!userLimit.isPresent()) {
            throw new RenderableException(Response.Status.NOT_FOUND, "User limit info is not existing");
        }
        return userLimit.get();
    }

    /*
     check if request should be throttled for given userId + apiName
     algorithm: sliding window + counter
     implementation: keep track of request counts for each user+apiName with multi fixed time windows,
      */
    @GET
    @Path("/throttle/{userId}/{apiName}")
    @Produces({ "application/json" })
    public boolean throttleRequest(@Valid @NotNull @PathParam("userId") String userId,
                              @Valid @NotNull @PathParam("apiName") String apiName) throws Exception {

        validationUtil.validateApiName(apiName);
        Optional<Limit> userLimit = limitDao.getLimitInfo(userId, apiName);
        if (!userLimit.isPresent()) {
            throw new RenderableException(Response.Status.NOT_FOUND, "User limit info is not existing");
        }

        Limit limit = userLimit.get();
        int fixedWindow = limit.getFixedWindowInMs();
        if (fixedWindow == 0) {
            throw new RenderableException(Response.Status.INTERNAL_SERVER_ERROR,
                    "user limit info has unexpected fixed window");
        }

        long now = Instant.now().toEpochMilli();
        long key = now / fixedWindow;

        Map<String, String> requestInfo = limitDao.getRequestCountInfo(userId, apiName).entrySet().stream()
                // filter out request out of window
                .filter(entry -> Math.abs(key - Long.parseLong(entry.getKey())) <= Limit.FIXED_WINDOW_NUM)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int currentRequestCnt = requestInfo.entrySet().stream()
                .mapToInt(entry -> Integer.parseInt(entry.getValue())).sum();
        if (currentRequestCnt >= limit.getLimitNum()) {
            return true;
        }
        int cnt = (requestInfo.containsKey(key) ? Integer.parseInt(requestInfo.get(key)) : 0) + 1;
        requestInfo.put("" + key, "" + cnt);

        limitDao.writeRequestCountInfo(userId, apiName, requestInfo);
        return false;
    }

    @POST
    @Path("/limit-info")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ "application/json" })
    public void createLimitInfo(@Valid  Limit limitInfo) throws Exception {
        validationUtil.validateLimitInfo(limitInfo);

        limitDao.createUserLimitInfo(limitInfo);
    }


    @POST
    @Path("/apiName")
    @Produces({ "application/json" })
    public void registerApiName(@Valid @HeaderParam("apiName") String apiName) throws Exception {
        validationUtil.validateApiName(apiName);
        limitDao.addApiNames(apiName);
    }

    @DELETE
    @Path("/apiName")
    @Produces({ "application/json" })
    public void removeApiName(@Valid @HeaderParam("apiName") String apiName) throws Exception {
        throw new RenderableException(Response.Status.INTERNAL_SERVER_ERROR, "Not supported yet!");
    }
}
