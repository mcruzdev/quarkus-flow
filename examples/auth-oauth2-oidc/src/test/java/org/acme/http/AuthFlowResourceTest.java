package org.acme.http;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

@QuarkusTest
class AuthFlowResourceTest {

    @Test
    @DisplayName("Should do a request using the access_token provided by the Authorization Server")
    void should_list_images_authorized_with_client_credentials() {

        RestAssured.given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/quarkus-flow/images")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should pass token to the downstream service and must return 200")
    void should_pass_token_successful() {
        RestAssured.given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer A.abc.signature") // starting with 'A'  is valid
                .get("/quarkus-flow/repos")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should pass token to the downstream service and must return 401 when is invalid")
    void should_pass_token_but_should_return_401_due_to_invalid_token() {
        RestAssured.given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer B.abc.signature") // starting with 'B'  is invalid
                .get("/quarkus-flow/repos")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should use multiple client/authorization servers in the same workflow")
    void should_use_multiple_clients_requesting_token_on_multiple_auth_servers_successfully() {
        RestAssured.given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer B.abc.signature") // starting with 'B'  is invalid
                .get("/quarkus-flow/read-all-emails")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should do a request using the access_token provided by the Authorization Server (using OpenAPI)")
    void should_list_images_using_openapi_with_oauth2_schema() {
        RestAssured.given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/quarkus-flow/openapi/images")
                .then()
                .statusCode(200);
    }

}