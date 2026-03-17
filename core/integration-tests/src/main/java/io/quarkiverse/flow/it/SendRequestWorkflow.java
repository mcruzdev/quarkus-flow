package io.quarkiverse.flow.it;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;

@ApplicationScoped
public class SendRequestWorkflow extends Flow {

    @ConfigProperty(name = "wiremock.send.request.url")
    String sendRequestUrl;

    @Override
    public Workflow descriptor() {
        return FuncWorkflowBuilder.workflow(
                "sendRequestWorkflow").tasks(
                        FuncDSL.http().POST()
                                .uri(URI.create(sendRequestUrl + "/users"))
                                .header("Content-Type", "application/json")
                                .body("${.}"))
                .build();
    }
}
