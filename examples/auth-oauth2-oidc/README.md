# HTTP OAuth2 + OIDC (Quarkus Flow)

This example shows how to call **OAuth2 / OIDC protected HTTP services** from a workflow using the
Quarkus Flow Java DSL. It covers the most common token scenarios you hit when orchestrating remote
APIs:

* **Client Credentials grant** — the workflow obtains an `access_token` from an authorization server
  and uses it to call a downstream service.
* **User-provided token** — a token presented by the caller is propagated to the downstream service.
* **Multiple OAuth2 clients** — several authorization servers / clients declared once and reused
  across tasks in the same workflow.
* **OpenAPI call with OAuth2/OIDC** — an OpenAPI operation secured with an `oauth2`/`openIdConnect`
  security scheme.

All external services (authorization servers and downstream APIs) are **mocked with
[WireMock Dev Services](https://docs.quarkiverse.io/quarkus-wiremock/dev/)**, so the example runs
end-to-end with no real OAuth2 server.

> "Call OAuth2/OIDC protected HTTP and OpenAPI endpoints from a workflow, letting Quarkus Flow handle
> the token retrieval, and surface the result via REST."

---

## What you'll build

A REST API under `/quarkus-flow` that triggers four workflows, each demonstrating one
authentication scenario:

| Endpoint                       | Workflow                        | Scenario                                                |
|--------------------------------|---------------------------------|---------------------------------------------------------|
| `GET /quarkus-flow/images`     | `client-credentials`            | OAuth2 **Client Credentials** grant on a plain HTTP call |
| `GET /quarkus-flow/repos`      | `user-providing-token`          | Propagate a **caller-provided** bearer token downstream  |
| `GET /quarkus-flow/read-all-emails` | `multiple-oauth2-clients`  | **Multiple** OAuth2 clients/authorities in one workflow  |
| `GET /quarkus-flow/openapi/images`  | (OpenAPI)                  | **OpenAPI** operation secured with OAuth2/**OIDC**       |

---

## Project layout

```text
examples/auth-oauth2-oidc/
├── src
│   ├── main
│   │   ├── java
│   │   │   └── org/acme/http
│   │   │       ├── AuthServerConfig.java                # @ConfigMapping for auth-server.* credentials
│   │   │       ├── ClientCredentialsListImagesFlow.java # Client Credentials grant
│   │   │       ├── UserProvidingTokenFlow.java          # Propagate caller-provided token
│   │   │       ├── MultipleOAuth2ClientsFlow.java       # Multiple OAuth2 clients in one workflow
│   │   │       ├── OpenAPIWithOAuth2Flow.java           # OpenAPI operation with OAuth2/OIDC
│   │   │       └── api/FlowResource.java                # REST API starting the workflows
│   │   └── resources
│   │       ├── application.properties                  # auth-server.* credentials + service URLs
│   │       ├── __files/openapi-oauth2.yaml             # OpenAPI document served by WireMock
│   │       └── mappings/                               # WireMock stubs (token + downstream services)
│   └── test
│       └── java
│           └── org/acme/http
│               └── AuthFlowResourceTest.java           # RestAssured tests for each scenario
└── pom.xml
```

---

## Configuration (`application.properties`)

Credentials for each authorization server are grouped under the `auth-server.*` prefix and exposed
through an `@ConfigMapping` (`AuthServerConfig`). Service and authorization-server URLs point at the
WireMock Dev Services port:

```properties
image.service.url=http://localhost:${quarkus.wiremock.devservices.port}/image-service/images
repos.service.url=http://localhost:${quarkus.wiremock.devservices.port}/repos-service/repos

# Credentials per authorization server: auth-server.<name>.*
auth-server.keycloakMock.client-id=quarkus-flow
auth-server.keycloakMock.client-secret=dummy-client-secret
auth-server.keycloakMock.base-url=http://localhost:${quarkus.wiremock.devservices.port}/auth/realms/test-realm/protocol/openid-connect/token

auth-server.joogle.client-id=quarkus-flow
auth-server.joogle.client-secret=joogle-client-secret
auth-server.joogle.base-url=${base.url}

auth-server.jahoo.client-id=quarkus-flow
auth-server.jahoo.client-secret=jahoo-client-secret
auth-server.jahoo.base-url=${base.url}

# WireMock loads stubs/files from src/main/resources (mappings/ and __files/)
quarkus.wiremock.devservices.files-mapping=src/main/resources
```

The matching `@ConfigMapping`:

```java
@ConfigMapping(prefix = "auth-server")
public interface AuthServerConfig {

    @WithParentName
    Map<String, Credentials> credentials();

    @ConfigGroup
    interface Credentials {
        String clientId();
        String clientSecret();
        String baseUrl();
    }
}
```

---

## 1. Client Credentials grant (`/quarkus-flow/images`)

The workflow asks the authorization server for a token using the **Client Credentials** grant and
uses it to call the image service. The OAuth2 configuration is attached **inline** to the HTTP call
via `FuncDSL.oauth2(...)`:

```java
@ApplicationScoped
public class ClientCredentialsListImagesFlow extends Flow {

    private static final String AUTHORIZATION_SERVER = "keycloakMock";

    @ConfigProperty(name = "image.service.url")
    String imageService;

    @Inject
    AuthServerConfig authServer;

    @Override
    public Workflow descriptor() {
        var creds = authServer.credentials().get(AUTHORIZATION_SERVER);
        return FuncWorkflowBuilder.workflow("client-credentials", "quarkus-flow")
                .tasks(
                        FuncDSL.call(
                                FuncDSL.http("listImages")
                                        .GET()
                                        .header("Accept", "application/json")
                                        .uri(URI.create(imageService),
                                                FuncDSL.oauth2(creds.baseUrl(), CLIENT_CREDENTIALS,
                                                        creds.clientId(), creds.clientSecret()))
                        )
                )
                .build();
    }
}
```

Quarkus Flow performs the token request against `base-url`, caches the result, and adds the
`Authorization: Bearer <token>` header to the downstream call automatically.

---

## 2. Propagate a caller-provided token (`/quarkus-flow/repos`)

Sometimes the caller already has a token (e.g. injected as `WebToken jwt` when using `quarkus-oidc`
or `quarkus-smallrye-jwt`). Here the REST resource forwards the incoming `Authorization` header into
the workflow input, and the workflow passes it straight to the downstream service:

```java
@Override
public Workflow descriptor() {
    return FuncWorkflowBuilder.workflow("user-providing-token", "quarkus-flow")
            .tasks(
                    FuncDSL.call(
                            FuncDSL.http("listRepos")
                                    .GET()
                                    .header("Accept", "application/json")
                                    .header("Authorization", "${ .bearerToken }")
                                    .uri(URI.create(reposUrl))
                    )
            )
            .build();
}
```

The resource starts the instance with the token as input and maps a downstream `401` to an HTTP
`401` response:

```java
@GET
@Path("/repos")
public Response listMyRepos(@HeaderParam("Authorization") String bearerToken) {
    try {
        WorkflowModel model = userProvidingToken
                .instance(Map.of("bearerToken", bearerToken))
                .start().join();
        return Response.ok(model.asJavaObject()).build();
    } catch (Exception e) {
        if (e.getCause() instanceof WorkflowException error) {
            return Response.status(401).entity(error.getWorkflowError()).build();
        }
        throw e;
    }
}
```

In the mocked setup, a token starting with `A` is accepted and one starting with `B` is rejected
with `401`.

---

## 3. Multiple OAuth2 clients in one workflow (`/quarkus-flow/read-all-emails`)

When a workflow talks to several services, each with its own authorization server, declare the
authentications **once** under `use(...)` and reference them by name with `FuncDSL.use("name")`.
The two calls run in parallel via `fork`:

```java
return FuncWorkflowBuilder.workflow("multiple-oauth2-clients", "quarkus-flow")
        .use(use -> use.authentications(auth -> {
            auth.authentication("joogle", a -> a.oauth2(oauth2 ->
                    oauth2.endpoints(e -> e.token("/auth/joogle/token"))
                          .client(c -> c.id(joogle.clientId())
                                        .secret(joogle.clientSecret())
                                        .authentication(CLIENT_SECRET_POST))
                          .authority(joogle.baseUrl())
                          .grant(CLIENT_CREDENTIALS)));
            auth.authentication("jahoo", a -> a.oauth2(oauth2 ->
                    oauth2.endpoints(e -> e.token("/auth/jahoo/oidc/token"))
                          .client(c -> c.id(jahoo.clientId())
                                        .secret(jahoo.clientSecret())
                                        .authentication(CLIENT_SECRET_POST))
                          .authority(jahoo.baseUrl())
                          .grant(CLIENT_CREDENTIALS)));
        }))
        .tasks(
                FuncDSL.fork(
                        FuncDSL.http("getEmailsFromJoogle").GET()
                                .header("Accept", "application/json")
                                .uri(URI.create(wireMock + "/joogle/inbox"), FuncDSL.use("joogle")),
                        FuncDSL.http("getEmailsFromJahoo").GET()
                                .header("Accept", "application/json")
                                .uri(URI.create(wireMock + "/jahoo/inbox"), FuncDSL.use("jahoo"))),
                FuncDSL.function("merge", o -> { Log.info("Merging emails: " + o); return o; })
        )
        .build();
```

Each authentication can target a different token endpoint, authority, client authentication method
(`CLIENT_SECRET_POST` here), and grant type.

---

## 4. OpenAPI operation with OAuth2/OIDC (`/quarkus-flow/openapi/images`)

Instead of a raw HTTP call, this workflow drives an **OpenAPI operation**. The OpenAPI document
(`__files/openapi-oauth2.yaml`, served by WireMock) declares an OAuth2/OIDC security scheme, and the
DSL supplies the credentials through `FuncDSL.oidc(...)`:

```java
return FuncWorkflowBuilder.workflow()
        .tasks(t -> t.openapi("imageService", f ->
                f.document("http://localhost:" + wireMockPort + "/openapi/openapi-oauth2.yaml?wireMockPort=" + wireMockPort)
                 .operation("listImages")
                 .parameters(Map.of("Accept", "application/json"))
                 .authentication(FuncDSL.oidc(
                         authServer.baseUrl(), CLIENT_CREDENTIALS,
                         authServer.clientId(), authServer.clientSecret()))))
        .build();
```

`oidc(...)` mirrors `oauth2(...)` but is meant for OpenID Connect discovery / `openIdConnect`
security schemes.

---

## Mocked services (WireMock)

There is no real OAuth2 server in this example. WireMock Dev Services serves:

* **Token endpoints** — return a JSON `access_token` for the client-credentials requests
  (`mappings/client-credentials.json`, `mappings/joogle.json`, `mappings/jahoo.json`).
* **Downstream APIs** — image service, repos service, and the two email inboxes, each verifying the
  `Authorization` header (`mappings/list-images.json`, `mappings/list-repos-401.json`, ...).
* **The OpenAPI document** — `__files/openapi-oauth2.yaml`, served via
  `mappings/openapi-provider.json` (the `{{wireMockPort}}` placeholder is replaced using WireMock
  response templating so URLs resolve to the random dev-services port).

Stubs and files are loaded from `src/main/resources` thanks to
`quarkus.wiremock.devservices.files-mapping=src/main/resources`.

---

## Testing with RestAssured

`AuthFlowResourceTest` exercises every scenario against the mocked services:

```java
@Test
@DisplayName("Should do a request using the access_token provided by the Authorization Server")
void should_list_images_authorized_with_client_credentials() {
    RestAssured.given().accept(ContentType.JSON)
            .get("/quarkus-flow/images")
            .then().statusCode(200);
}

@Test
@DisplayName("Should pass token to the downstream service and must return 401 when is invalid")
void should_pass_token_but_should_return_401_due_to_invalid_token() {
    RestAssured.given().accept(ContentType.JSON)
            .header("Authorization", "Bearer B.abc.signature") // starting with 'B' is invalid
            .get("/quarkus-flow/repos")
            .then().statusCode(401);
}
```

Run:

```bash
./mvnw test
```

---

## Running the example

From the example directory:

```bash
./mvnw quarkus:dev
```

Quarkus starts WireMock Dev Services automatically. Then trigger the workflows:

```bash
# 1. Client Credentials grant
curl http://localhost:8080/quarkus-flow/images | jq

# 2. Caller-provided token (A.* is accepted, B.* is rejected with 401)
curl -H "Authorization: Bearer A.abc.signature" http://localhost:8080/quarkus-flow/repos | jq

# 3. Multiple OAuth2 clients
curl http://localhost:8080/quarkus-flow/read-all-emails | jq

# 4. OpenAPI operation secured with OAuth2/OIDC
curl http://localhost:8080/quarkus-flow/openapi/images | jq
```

---

## Learn more

* Quarkus Flow documentation:
  [https://docs.quarkiverse.io/quarkus-flow/dev/](https://docs.quarkiverse.io/quarkus-flow/dev/)
* CNCF Serverless Workflow — Authentication:
  [https://github.com/serverlessworkflow/specification](https://github.com/serverlessworkflow/specification)
* Quarkus OIDC:
  [https://quarkus.io/guides/security-oidc-bearer-token-authentication](https://quarkus.io/guides/security-oidc-bearer-token-authentication)
* Quarkus WireMock Dev Services:
  [https://docs.quarkiverse.io/quarkus-wiremock/dev/](https://docs.quarkiverse.io/quarkus-wiremock/dev/)
</content>
</invoke>
