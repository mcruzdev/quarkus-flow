package org.acme.http;

import io.quarkiverse.flow.Flow;
import io.quarkus.logging.Log;
import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.OAuth2AuthenticationDataClient;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;

@ApplicationScoped
public class MultipleOAuth2ClientsFlow extends Flow {

    @Inject
    AuthServerConfig authServerConfig;

    @ConfigProperty(name = "base.url")
    String wireMock;

    @Override
    public Workflow descriptor() {

        AuthServerConfig.Credentials joogle = authServerConfig.credentials().get("joogle");
        AuthServerConfig.Credentials jahoo = authServerConfig.credentials().get("jahoo");

        return FuncWorkflowBuilder.workflow(
                "multiple-oauth2-clients", "quarkus-flow")
                .use(use -> {
                    use.authentications(auth -> {
                        auth.authentication("joogle", a -> {
                            a.oauth2(oauth2 -> {
                                oauth2.endpoints(e -> e.token("/auth/joogle/token"))
                                        .client(client -> client.id(joogle.clientId())
                                                .secret(joogle.clientSecret())
                                                .authentication(
                                                        OAuth2AuthenticationDataClient.ClientAuthentication.CLIENT_SECRET_POST))
                                        .authority(joogle.baseUrl())
                                        .grant(OAuth2AuthenticationData.OAuth2AuthenticationDataGrant.CLIENT_CREDENTIALS);
                            });
                        });
                        auth.authentication("jahoo", a -> {
                            a.oauth2(oauth2 -> {
                                oauth2.endpoints(e -> e.token("/auth/jahoo/oidc/token"))
                                        .client(client -> client.id(jahoo.clientId())
                                                .secret(jahoo.clientSecret())
                                                .authentication(
                                                        OAuth2AuthenticationDataClient.ClientAuthentication.CLIENT_SECRET_POST))
                                        .authority(jahoo.baseUrl())
                                        .grant(OAuth2AuthenticationData.OAuth2AuthenticationDataGrant.CLIENT_CREDENTIALS);
                            });
                        });
                    });

                })
                .tasks(
                        FuncDSL.fork(
                                FuncDSL.http("getEmailsFromJoogle").GET()
                                        .header("Accept", "application/json")
                                        .uri(URI.create(wireMock + "/joogle/inbox"), FuncDSL.use("joogle")),
                                FuncDSL.http("getEmailsFromJahoo").GET()
                                        .header("Accept", "application/json")
                                        .uri(URI.create(wireMock + "/jahoo/inbox"), FuncDSL.use("jahoo"))),
                        FuncDSL.function("merge", o -> {
                            Log.info("Merging emails: " + o);
                            return o;
                        }))
                .build();
    }
}
