package com.ratelimiter;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ratelimiter.config.RateLimiterServiceConfiguration;
import com.ratelimiter.config.RateLimiterServiceModule;
import com.ratelimiter.config.TypeSafeFileReader;
import com.ratelimiter.config.TypesafeConfigProvider;
import com.ratelimiter.health.RateLimiterServiceHealthCheck;
import com.ratelimiter.resources.Resource;
import com.ratelimiter.utils.RenderableExceptionCatchAllMapper;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import jersey.repackaged.com.google.common.collect.ImmutableList;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ExceptionMapper;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class RateLimiterService extends Application<RateLimiterServiceConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final String SERVICE_NAME = "RateLimiterService";

    // Add your resources to this list.
    public static final List<Class<?>> RESOURCE_CLASSES = ImmutableList.<Class<?>>builder()
            .add(Resource.class)
            .build();
    /**
     * The entry point of the service.
     */
    public static void main(String[] args) throws Exception {
        log.info("Starting rateLimiter Service....");
        new RateLimiterService().run(args);
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public void initialize(Bootstrap<RateLimiterServiceConfiguration> bootstrap) {
        // Allows restricted headers, to suppress the large volume of "may be ignored" message during Jersey calls.
        // See https://java.net/jira/browse/JERSEY-2231 for some notes about why this is necessary.
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        bootstrap.setConfigurationSourceProvider(new TypesafeConfigProvider(new TypeSafeFileReader()));

        bootstrap.addBundle(new AssetsBundle("/ui", "/ui", "index.html", "ui"));
        bootstrap.addBundle(new AssetsBundle("/spec", "/spec", null, "spec"));

        log.info("Spec and API explorer: \n\n    SPEC    /spec/api.json\n    UI      /ui\n");
    }

    @Override
    public void run(RateLimiterServiceConfiguration config, Environment environment) throws Exception {
        log.info("rateLimiter ServiceConfig {}", config);
        log.info("Initializing rateLimiter service...");

        try {
            if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
                Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
            }

            // Configure dependency injection
            log.info("Configuring Guice Injector");

            Injector injector = Guice.createInjector(new RateLimiterServiceModule(config, environment));

            //Register resources and health checks
            registerExceptionMapper(environment);
            registerResources(environment, injector);
            registerHealthChecks(environment);
        } catch (Exception e) {
            log.error("rateLimiter service failed to start", e);
            throw e;
        }
        log.info("rateLimiter service initialization completed");
    }

    private void registerHealthChecks(Environment environment) {
        log.info("Registering health checks");
        environment.healthChecks().register(RateLimiterServiceHealthCheck.getName(),
                new RateLimiterServiceHealthCheck());
    }

    private void registerResources(Environment environment, Injector injector) {
        JerseyEnvironment jersey = environment.jersey();
        for (Class<?> clazz : RESOURCE_CLASSES) {
            log.info("Registering resource {}", clazz.getSimpleName());
            jersey.register(injector.getInstance(clazz));
        }
    }

    private void registerExceptionMapper(Environment environment) {
        List<Class<? extends ExceptionMapper<? extends Throwable>>> exceptionMappers = new ArrayList<>();
        exceptionMappers.add(RenderableExceptionCatchAllMapper.class);
        JerseyEnvironment jersey = environment.jersey();
        exceptionMappers.forEach(clazz -> {
            jersey.register(clazz);
        });
    }

}
