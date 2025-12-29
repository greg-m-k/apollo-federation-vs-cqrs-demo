package com.example.hr.filter;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the TimingFilter that adds X-Subgraph-Time-Ms header to responses.
 */
@QuarkusTest
class TimingFilterTest {

    @Test
    void graphqlEndpoint_shouldIncludeTimingHeader() {
        Response response = given()
            .contentType("application/json")
            .body("{\"query\": \"{ persons { id name } }\"}")
            .when()
            .post("/graphql")
            .then()
            .extract()
            .response();

        // Verify the timing header is present
        String timingHeader = response.getHeader("X-Subgraph-Time-Ms");
        assertThat(timingHeader).isNotNull();

        // Verify it's a valid number
        int timing = Integer.parseInt(timingHeader);
        assertThat(timing).isGreaterThanOrEqualTo(0);
        assertThat(timing).isLessThan(10000); // Should be less than 10 seconds
    }

    @Test
    void graphqlEndpoint_timingHeaderShouldBeReasonable() {
        // Make multiple requests and verify timing is consistent
        for (int i = 0; i < 3; i++) {
            Response response = given()
                .contentType("application/json")
                .body("{\"query\": \"{ person(id: \\\"person-001\\\") { id name } }\"}")
                .when()
                .post("/graphql")
                .then()
                .extract()
                .response();

            String timingHeader = response.getHeader("X-Subgraph-Time-Ms");
            assertThat(timingHeader).isNotNull();

            int timing = Integer.parseInt(timingHeader);
            // Simple query should complete in reasonable time
            assertThat(timing).isLessThan(5000);
        }
    }

    @Test
    void graphqlEndpoint_timingHeaderIncludedOnErrors() {
        // Even error responses should have timing
        Response response = given()
            .contentType("application/json")
            .body("{\"query\": \"{ invalidQuery }\"}")
            .when()
            .post("/graphql")
            .then()
            .extract()
            .response();

        // Timing header should still be present
        String timingHeader = response.getHeader("X-Subgraph-Time-Ms");
        assertThat(timingHeader).isNotNull();
    }
}
