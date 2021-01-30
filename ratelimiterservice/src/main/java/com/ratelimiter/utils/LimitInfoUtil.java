package com.ratelimiter.utils;

import com.ratelimiter.mode.DurationUnit;
import com.ratelimiter.mode.Limit;
import java.util.List;

public class LimitInfoUtil {
    public static final int LIMIT_INFO_LEN = 3;
    public static final String WHITE_LISTED_API_NAME_KEY = "allowedApis";
    public static final String LIMIT_INFO_KEY_PREFIX = "limitInfo#";
    public static final String REQUEST_INFO_KEY_PREFIX = "requestInfo#";

    /**
     * generate key based on userId and apiName, which will be used to fetch user limit info from storage
     * @param userId userId
     * @param apiName apiName
     * @return key
     */
    public static String getUserLimitInfoKey(String userId, String apiName) {
        return new StringBuilder().append(LIMIT_INFO_KEY_PREFIX).append(userId).append('#').append(apiName).toString();
    }

    /**
     * generate key based on userId and apiName, which will be used to fetch user request info from storage
     * @param userId userId
     * @param apiName apiName
     * @return key
     */
    public static String getRequestInfoKey(String userId, String apiName) {
        return new StringBuilder().append(REQUEST_INFO_KEY_PREFIX)
                .append(userId).append('#').append(apiName).toString();
    }

    public static Limit.Builder getLimitBuilder(List<String> list) {
        if (list.size() != LIMIT_INFO_LEN) {
            throw new RuntimeException("limit info is with unexpected length");
        }
        return Limit.builder()
                .limitNum(Integer.parseInt(list.get(0)))
                .duration(Integer.parseInt(list.get(1)))
                .unit(DurationUnit.from(list.get(2)));
    }

    public static String[] getLimitInfoInArr(Limit limit) {
        // it is important to keep elements in same order as getLimitBuilder does deserialize
        String[] limitInfoArr = new String[LIMIT_INFO_LEN];
        limitInfoArr[0] = "" + limit.getLimitNum();
        limitInfoArr[1] = "" + limit.getDuration();
        limitInfoArr[2] = limit.getDurationUnit().name();
        return limitInfoArr;
    }
}
