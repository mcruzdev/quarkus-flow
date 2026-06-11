package org.acme.http.api;

import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.acme.http.ClientCredentialsListImagesFlow;
import org.acme.http.MultipleOAuth2ClientsFlow;
import org.acme.http.OpenAPIWithOAuth2Flow;
import org.acme.http.UserProvidingTokenFlow;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@Path("/quarkus-flow")
public class FlowResource {

    @Inject
    ClientCredentialsListImagesFlow clientCredentials;

    @Inject
    UserProvidingTokenFlow userProvidingToken;

    @Inject
    MultipleOAuth2ClientsFlow multipleOAuth2Client;

    @Inject
    OpenAPIWithOAuth2Flow openAPIWithOAuth2;

    @ConfigProperty(name = "quarkus.wiremock.devservices.port")
    Integer wireMockPort;

    @GET
    @Path("/images")
    public Response listMyImages() {
        WorkflowModel model = clientCredentials.instance().start().join();
        return Response.ok(model.asJavaObject()).build();
    }

    @GET
    @Path("/openapi/images")
    public Response listMyImageUsingOpenAPI() {
        WorkflowModel model = openAPIWithOAuth2.instance().start().join();
        return Response.ok(model.asJavaObject()).build();
    }

    @GET
    @Path("/repos")
    public Response listMyRepos(@HeaderParam("Authorization") String bearerToken) {
        // The bearerToken would be got by @Inject WebToken jwt when using quarkus-oidc or quarkus-smallrye-jwt
        try {
            WorkflowModel model = userProvidingToken.instance(
                    Map.of("bearerToken", bearerToken)).start().join();
            return Response.ok(model.asJavaObject()).build();
        } catch (Exception e) {
            if (e.getCause() instanceof WorkflowException error) {
                return Response.status(401).entity(error.getWorkflowError()).build();
            }
            throw e;
        }
    }

    @GET
    @Path("/read-all-emails")
    public Response readAllEmails() {
        WorkflowModel model = multipleOAuth2Client.instance().start().join();
        return Response.ok(model).build();
    }
}
