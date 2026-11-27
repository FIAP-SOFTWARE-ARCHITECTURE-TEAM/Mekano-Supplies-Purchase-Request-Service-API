package com.fiap.mekano.supplies.infrastructure.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Sobe um WireMock em porta dinamica e aponta o REST Client do Registry para ele.
 */
public class WireMockRegistryTestResource implements QuarkusTestResourceLifecycleManager {

    private static WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();

        return Map.of(
                "quarkus.rest-client.registry-api.url", server.baseUrl(),
                "quarkus.rest-client.registry-api.connect-timeout", "1000",
                "quarkus.rest-client.registry-api.read-timeout", "500");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public static WireMockServer server() {
        if (server == null) {
            throw new IllegalStateException("WireMock nao iniciado");
        }
        return server;
    }
}
