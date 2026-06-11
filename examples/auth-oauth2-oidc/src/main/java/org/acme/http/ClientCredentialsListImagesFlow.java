package org.acme.http;

import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;

import static io.serverlessworkflow.api.types.OAuth2AuthenticationData.OAuth2AuthenticationDataGrant.CLIENT_CREDENTIALS;

@ApplicationScoped
public class ClientCredentialsListImagesFlow extends Flow {

    private static final String AUTHORIZATION_SERVER = "keycloakMock";

    @ConfigProperty(name = "image.service.url")
    String imageService;

    @Inject
    AuthServerConfig authServer;

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow(
                "client-credentials", "quarkus-flow")
                .tasks(
                        FuncDSL.call(
                                FuncDSL.http("listImages")
                                        .GET()
                                        .header("Accept", "application/json")
                                        .uri(URI.create(imageService),
                                                FuncDSL.oauth2(authServer.credentials().get(AUTHORIZATION_SERVER).baseUrl(),
                                                        CLIENT_CREDENTIALS,
                                                        authServer.credentials().get(AUTHORIZATION_SERVER).clientId(),
                                                        authServer.credentials().get(AUTHORIZATION_SERVER).clientSecret()))

                        ))
                .build();
    }
}
