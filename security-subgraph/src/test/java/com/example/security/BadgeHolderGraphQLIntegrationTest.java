package com.example.security;

import com.example.security.model.BadgeHolder;
import com.example.security.model.BadgeHolder.AccessLevel;
import com.example.security.model.BadgeHolder.Clearance;
import com.example.security.repository.BadgeHolderRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Security GraphQL endpoint using real H2 database.
 * These tests run without Docker.
 */
@QuarkusTest
class BadgeHolderGraphQLIntegrationTest {

    @Inject
    BadgeHolderRepository badgeHolderRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        badgeHolderRepository.deleteAll();

        BadgeHolder badge1 = new BadgeHolder("badge-001", "person-001", "B10001",
                AccessLevel.STANDARD, Clearance.NONE);
        BadgeHolder badge2 = new BadgeHolder("badge-002", "person-002", "B10002",
                AccessLevel.RESTRICTED, Clearance.CONFIDENTIAL);
        badge2.active = false;

        badgeHolderRepository.persist(badge1);
        badgeHolderRepository.persist(badge2);
    }

    @Test
    void testQueryAllBadgeHolders() {
        String query = """
            {
                "query": "{ badgeHolders { id personId badgeNumber accessLevel clearance active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.badgeHolders", hasSize(2))
            .body("data.badgeHolders[0].badgeNumber", is(oneOf("B10001", "B10002")));
    }

    @Test
    void testQueryBadgeHolderById() {
        String query = """
            {
                "query": "{ badgeHolder(id: \\"badge-001\\") { id personId badgeNumber accessLevel clearance active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.badgeHolder.id", is("badge-001"))
            .body("data.badgeHolder.personId", is("person-001"))
            .body("data.badgeHolder.badgeNumber", is("B10001"))
            .body("data.badgeHolder.accessLevel", is("STANDARD"))
            .body("data.badgeHolder.clearance", is("NONE"))
            .body("data.badgeHolder.active", is(true));
    }

    @Test
    void testQueryBadgeHolderById_NotFound() {
        String query = """
            {
                "query": "{ badgeHolder(id: \\"non-existent\\") { id badgeNumber } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.badgeHolder", is(nullValue()));
    }

    @Test
    void testQueryBadgeHolderByPersonId() {
        String query = """
            {
                "query": "{ badgeHolderByPersonId(personId: \\"person-002\\") { id badgeNumber accessLevel } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.badgeHolderByPersonId.id", is("badge-002"))
            .body("data.badgeHolderByPersonId.badgeNumber", is("B10002"))
            .body("data.badgeHolderByPersonId.accessLevel", is("RESTRICTED"));
    }

    @Test
    void testQueryBadgeHolderByBadgeNumber() {
        String query = """
            {
                "query": "{ badgeHolderByBadgeNumber(badgeNumber: \\"B10001\\") { id personId clearance } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.badgeHolderByBadgeNumber.id", is("badge-001"))
            .body("data.badgeHolderByBadgeNumber.personId", is("person-001"))
            .body("data.badgeHolderByBadgeNumber.clearance", is("NONE"));
    }

    @Test
    void testQueryBadgeHoldersByAccessLevel() {
        String query = """
            {
                "query": "{ badgeHoldersByAccessLevel(accessLevel: STANDARD) { id badgeNumber accessLevel } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.badgeHoldersByAccessLevel", hasSize(1))
            .body("data.badgeHoldersByAccessLevel[0].badgeNumber", is("B10001"))
            .body("data.badgeHoldersByAccessLevel[0].accessLevel", is("STANDARD"));
    }

    @Test
    void testQueryActiveBadgeHolders() {
        String query = """
            {
                "query": "{ activeBadgeHolders { id badgeNumber active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.activeBadgeHolders", hasSize(1))
            .body("data.activeBadgeHolders[0].badgeNumber", is("B10001"))
            .body("data.activeBadgeHolders[0].active", is(true));
    }

    @Test
    void testMutationProvisionBadge() {
        String mutation = """
            {
                "query": "mutation { provisionBadge(personId: \\"person-003\\", accessLevel: ALL_ACCESS, clearance: SECRET) { id personId badgeNumber accessLevel clearance active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.provisionBadge.personId", is("person-003"))
            .body("data.provisionBadge.accessLevel", is("ALL_ACCESS"))
            .body("data.provisionBadge.clearance", is("SECRET"))
            .body("data.provisionBadge.active", is(true))
            .body("data.provisionBadge.id", startsWith("badge-"))
            .body("data.provisionBadge.badgeNumber", startsWith("B"));
    }

    @Test
    void testMutationProvisionBadge_AlreadyExists() {
        String mutation = """
            {
                "query": "mutation { provisionBadge(personId: \\"person-001\\", accessLevel: VISITOR, clearance: NONE) { id } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.provisionBadge", is(nullValue()));
    }

    @Test
    void testMutationChangeAccessLevel() {
        String mutation = """
            {
                "query": "mutation { changeAccessLevel(id: \\"badge-001\\", newAccessLevel: ALL_ACCESS) { id accessLevel } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.changeAccessLevel.id", is("badge-001"))
            .body("data.changeAccessLevel.accessLevel", is("ALL_ACCESS"));
    }

    @Test
    void testMutationChangeAccessLevel_NotFound() {
        String mutation = """
            {
                "query": "mutation { changeAccessLevel(id: \\"non-existent\\", newAccessLevel: VISITOR) { id } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.changeAccessLevel", is(nullValue()));
    }

    @Test
    void testMutationChangeClearance() {
        String mutation = """
            {
                "query": "mutation { changeClearance(id: \\"badge-001\\", newClearance: TOP_SECRET) { id clearance } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.changeClearance.id", is("badge-001"))
            .body("data.changeClearance.clearance", is("TOP_SECRET"));
    }

    @Test
    void testMutationRevokeBadge() {
        String mutation = """
            {
                "query": "mutation { revokeBadge(id: \\"badge-001\\") { id badgeNumber active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.revokeBadge.id", is("badge-001"))
            .body("data.revokeBadge.active", is(false));
    }

    @Test
    void testMutationRevokeBadge_NotFound() {
        String mutation = """
            {
                "query": "mutation { revokeBadge(id: \\"non-existent\\") { id } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.revokeBadge", is(nullValue()));
    }

    @Test
    void testHealthEndpoint() {
        given()
        .when()
            .get("/q/health/ready")
        .then()
            .statusCode(200)
            .body("status", is("UP"));
    }
}
