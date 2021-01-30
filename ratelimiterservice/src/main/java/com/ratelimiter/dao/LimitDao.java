package com.ratelimiter.dao;

import com.ratelimiter.mode.Limit;

import java.util.Map;
import java.util.Optional;

public interface LimitDao {

    /**
     * return user limit info stored
     * @param userId userId
     * @param apiName apiName
     * @return Limit info
     */
    Optional<Limit> getLimitInfo(String userId, String apiName);

    /**
     * create limit info for user
     * @param userLimitInfo user limit info
     */
    void createUserLimitInfo(Limit userLimitInfo);


    /**
     * white list an apiName
     * @param apiName apiName
     * @return true api white listed, false api is white listed already
     */
    boolean addApiNames(String apiName);

    /**
     * get request info for given user
     * @param userId userId
     * @param apiName apiName
     * @return user request info map
     */
    Map<String, String> getRequestCountInfo(String userId, String apiName);

    /**
     * write/overwrite user request info
     * @param userId userId
     * @param apiName apiName
     * @param requestInfo requestInfo map
     */
    void writeRequestCountInfo(String userId, String apiName, Map<String, String> requestInfo);
}
