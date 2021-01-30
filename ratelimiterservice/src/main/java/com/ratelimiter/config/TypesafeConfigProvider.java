package com.ratelimiter.config;


import io.dropwizard.configuration.ConfigurationSourceProvider;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TypesafeConfigProvider implements ConfigurationSourceProvider {
    private static final Logger log = LoggerFactory.getLogger(TypesafeConfigProvider.class);
    @NonNull
    private final TypeSafeReader<String> reader;

    public TypesafeConfigProvider(TypeSafeReader<String> reader) {
        this.reader = reader;
    }

    public InputStream open(@NonNull String source) throws IOException {
        if (source == null) {
            throw new NullPointerException("source");
        } else {
            String json = this.reader.read(source);
            log.debug("Running with configuration:\n{}", json);
            return new ByteArrayInputStream(json.getBytes("UTF-8"));
        }
    }
}
