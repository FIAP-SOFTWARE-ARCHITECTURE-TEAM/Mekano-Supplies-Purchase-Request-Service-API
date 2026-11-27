package com.fiap.mekano.supplies.rest.api.observability;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@DisplayName("Health check")
class ApplicationLivenessCheckTest {

    @Test
    @DisplayName("GET /q/health/live responde UP")
    void livenessRespondeUp() {
        given()
                .when()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks[0].name", equalTo("mekano-supplies"));
    }
}
