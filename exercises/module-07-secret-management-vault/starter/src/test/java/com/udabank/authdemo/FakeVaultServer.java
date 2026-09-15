package com.udabank.authdemo;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Given — not part of the exercise. Stands in for a real Vault server so
 * this exercise doesn't require Docker: implements just enough of Vault's
 * KV v1 HTTP API (and the token self-lookup Spring Vault does on startup)
 * for spring-cloud-starter-vault-config's real client code to talk to.
 *
 * Fixed port (not dynamic) so it matches whatever you configure as
 * spring.cloud.vault.uri in application.yml — see the TODO there.
 *
 * Implemented as a JUnit 5 extension (register with @RegisterExtension,
 * not @BeforeAll) so it starts before Spring resolves
 * spring.config.import: vault://... — that resolution happens very early
 * in context bootstrap, earlier than a plain @BeforeAll method runs.
 */
public final class FakeVaultServer implements BeforeAllCallback, AfterAllCallback {

    public static final int PORT = 18200;

    private HttpServer server;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/v1/secret/authdemo", exchange -> {
            String body = "{\"data\":{"
                    + "\"db.username\":\"vault_managed_user\","
                    + "\"db.password\":\"vault-managed-secret-xyz\""
                    + "}}";
            sendJson(exchange, body);
        });

        server.createContext("/v1/auth/token/lookup-self", exchange -> {
            String body = "{\"data\":{\"renewable\":false,\"ttl\":0,\"policies\":[\"root\"]}}";
            sendJson(exchange, body);
        });

        server.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        server.stop(0);
    }

    private static void sendJson(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
