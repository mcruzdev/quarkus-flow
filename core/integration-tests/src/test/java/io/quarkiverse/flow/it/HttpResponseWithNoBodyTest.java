package io.quarkiverse.flow.it;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.impl.WorkflowModel;

@QuarkusTest
@QuarkusTestResource(HttpResponseWithNoBodyTest.WireMockTestResource.class)
public class HttpResponseWithNoBodyTest {

    @Inject
    SendRequestWorkflow requestWorkflow;

    @Test
    void should_receive_empty_response() {

        WorkflowModel model = requestWorkflow.instance(Map.of("name", "John")).start().join();

        Assertions.assertNotNull(model);

    }

    public static class WireMockTestResource implements QuarkusTestResourceLifecycleManager {

        WireMockServer wireMockServer;

        static int port;

        static {
            try {
                port = HttpPortUtils.generateRandomPort();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Map<String, String> start() {
            wireMockServer = new WireMockServer(port);
            wireMockServer.start();

            wireMockServer.stubFor(
                    post("/users")
                            .withHeader("Content-Type", equalTo("application/json"))
                            .willReturn(ok()));

            return Map.of("wiremock.send.request.url", wireMockServer.baseUrl());
        }

        @Override
        public void stop() {
            wireMockServer.stop();
        }
    }
}
