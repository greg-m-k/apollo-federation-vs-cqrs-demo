package com.example.hr;

import com.example.hr.model.Person;
import com.example.hr.repository.PersonRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for HR GraphQL endpoint using real H2 database.
 * These tests run without Docker.
 */
@QuarkusTest
class PersonGraphQLIntegrationTest {

    @Inject
    PersonRepository personRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        personRepository.deleteAll();

        Person person1 = new Person("person-001", "John Doe", "john.doe@example.com",
                LocalDate.of(2020, 1, 15));
        Person person2 = new Person("person-002", "Jane Smith", "jane.smith@example.com",
                LocalDate.of(2021, 3, 20));
        person2.active = false;

        personRepository.persist(person1);
        personRepository.persist(person2);
    }

    @Test
    void testQueryAllPersons() {
        String query = """
            {
                "query": "{ persons { id name email active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.persons", hasSize(2))
            .body("data.persons[0].name", is(oneOf("John Doe", "Jane Smith")));
    }

    @Test
    void testQueryPersonById() {
        String query = """
            {
                "query": "{ person(id: \\"person-001\\") { id name email hireDate active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.person.id", is("person-001"))
            .body("data.person.name", is("John Doe"))
            .body("data.person.email", is("john.doe@example.com"))
            .body("data.person.active", is(true));
    }

    @Test
    void testQueryPersonById_NotFound() {
        String query = """
            {
                "query": "{ person(id: \\"non-existent\\") { id name } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.person", is(nullValue()));
    }

    @Test
    void testQueryPersonByEmail() {
        String query = """
            {
                "query": "{ personByEmail(email: \\"jane.smith@example.com\\") { id name email } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.personByEmail.name", is("Jane Smith"))
            .body("data.personByEmail.email", is("jane.smith@example.com"));
    }

    @Test
    void testQueryActivePersons() {
        String query = """
            {
                "query": "{ activePersons { id name active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.activePersons", hasSize(1))
            .body("data.activePersons[0].name", is("John Doe"))
            .body("data.activePersons[0].active", is(true));
    }

    @Test
    void testSearchPersons() {
        String query = """
            {
                "query": "{ searchPersons(name: \\"john\\") { id name } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.searchPersons", hasSize(1))
            .body("data.searchPersons[0].name", is("John Doe"));
    }

    @Test
    void testMutationCreatePerson() {
        String mutation = """
            {
                "query": "mutation { createPerson(name: \\"Alice Brown\\", email: \\"alice@example.com\\", hireDate: \\"2023-01-15\\") { id name email active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.createPerson.name", is("Alice Brown"))
            .body("data.createPerson.email", is("alice@example.com"))
            .body("data.createPerson.active", is(true))
            .body("data.createPerson.id", startsWith("person-"));
    }

    @Test
    void testMutationTerminatePerson() {
        String mutation = """
            {
                "query": "mutation { terminatePerson(id: \\"person-001\\") { id name active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.terminatePerson.id", is("person-001"))
            .body("data.terminatePerson.active", is(false));
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
