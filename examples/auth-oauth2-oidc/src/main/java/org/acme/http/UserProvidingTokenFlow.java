package org.acme.http;

import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;

@ApplicationScoped
public class UserProvidingTokenFlow extends Flow {

    @ConfigProperty(name = "repos.service.url")
    String reposUrl;

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow(
                "user-providing-token", "quarkus-flow")
                .tasks(
                        FuncDSL.call(
                                FuncDSL.http("listRepos")
                                        .GET()
                                        .header("Accept", "application/json")
                                        .header("Authorization", "${ .bearerToken }")
                                        .uri(URI.create(reposUrl))

                        ))
                .build();
    }
}
