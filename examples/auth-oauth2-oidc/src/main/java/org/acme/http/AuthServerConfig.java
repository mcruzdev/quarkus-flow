package org.acme.http;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

import java.util.Map;

@ConfigMapping(prefix = "auth-server")
public interface AuthServerConfig {

    @WithParentName
    Map<String, Credentials> credentials();

    @ConfigGroup
    interface Credentials {
        String clientSecret();

        String clientId();

        String baseUrl();

    }
}
