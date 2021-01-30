package com.ratelimiter.utils;

import com.google.common.base.Preconditions;
import com.ratelimiter.mode.DurationUnit;
import com.ratelimiter.mode.Limit;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for validating input at the resources level.
 */
public final class ResourceInputValidationUtil {

    private static final int API_NAME_MAX_LENGTH = 64;

    public void validateLimitInfo(Limit limitinfo) {
        validateApiName(limitinfo.getApiName());
        validateLimitWindow(limitinfo.getDuration(), limitinfo.getDurationUnit());

    }

    public void validateApiName(String apiName) {
        try {
            Preconditions.checkArgument(StringUtils.isNotBlank(apiName), "apiName cannot be blank");

            Preconditions.checkArgument(apiName.length() <= API_NAME_MAX_LENGTH,
                    "apiName can't be longer than %s", API_NAME_MAX_LENGTH);
        } catch (Exception e) {
            throw new ValidationException(e);
        }
    }

    public void validateLimitWindow(int duration, DurationUnit unit) {
        final int dayInMin = 24 * 60;
        final int dayInSec = dayInMin * 60;
        try {
            Preconditions.checkArgument(
                    duration > 0 && (unit == DurationUnit.Minite && duration <= dayInMin ||
                            unit == DurationUnit.Second && duration <= dayInSec),
                    "limit window is not correct!");
        } catch (Exception e) {
            throw new ValidationException(e);
        }
    }

}
