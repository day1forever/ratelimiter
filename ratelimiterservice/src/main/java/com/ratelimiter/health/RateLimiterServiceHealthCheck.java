package com.ratelimiter.health;

import com.codahale.metrics.health.HealthCheck;

/**
 * This is the default service health check. Any number of health checks can exist by creating a
 * class that extends HealthCheck.
 * 
 * Access the health check on the admin endpoint and port defined in the base.conf followed by
 * /healthcheck.
 * 
 * Example: http://127.0.0.1:18081/healthcheck
 * 
 * Append ?pretty to pretty print: /healthcheck?pretty
 */
public class RateLimiterServiceHealthCheck extends HealthCheck {

    public static String getName() {
        return "rate-limiter-service";
    }

    @Override
    protected Result check() throws Exception {
        return Result.healthy();
    }
}
