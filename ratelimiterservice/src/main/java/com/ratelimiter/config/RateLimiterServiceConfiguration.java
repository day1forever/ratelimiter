package com.ratelimiter.config;

import io.dropwizard.Configuration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import javax.validation.constraints.NotNull;


@Getter
@Setter
@ToString
public class RateLimiterServiceConfiguration extends Configuration {
    @NotNull
    private String logdir;

    @NotNull
    private String realm;

    @NotNull
    private String region;

    @NotNull
    private String stage;

    @NotNull
    private String jedisHost;
}
