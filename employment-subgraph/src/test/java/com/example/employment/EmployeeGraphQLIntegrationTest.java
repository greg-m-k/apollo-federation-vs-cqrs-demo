package com.example.employment;

import com.example.employment.model.Employee;
import com.example.employment.repository.EmployeeRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Employment GraphQL endpoint using real H2 database.
 * These tests run without Docker.
 */
@QuarkusTest
class EmployeeGraphQLIntegrationTest {

    @Inject
    EmployeeRepository employeeRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        employeeRepository.deleteAll();

        Employee emp1 = new Employee("emp-001", "person-001", "Software Engineer",
                "Engineering", new BigDecimal("85000.00"));
        Employee emp2 = new Employee("emp-002", "person-002", "Senior Engineer",
                "Engineering", new BigDecimal("120000.00"));
        Employee emp3 = new Employee("emp-003", "person-003", "Product Manager",
                "Product", new BigDecimal("110000.00"));
        emp3.active = false;

        employeeRepository.persist(emp1);
        employeeRepository.persist(emp2);
        employeeRepository.persist(emp3);
    }

    @Test
    void testQueryAllEmployees() {
        String query = """
            {
                "query": "{ employees { id title department salary active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.employees", hasSize(3))
            .body("data.employees[0].id", is(oneOf("emp-001", "emp-002", "emp-003")));
    }

    @Test
    void testQueryEmployeeById() {
        String query = """
            {
                "query": "{ employee(id: \\"emp-001\\") { id personId title department salary active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.employee.id", is("emp-001"))
            .body("data.employee.personId", is("person-001"))
            .body("data.employee.title", is("Software Engineer"))
            .body("data.employee.department", is("Engineering"))
            .body("data.employee.active", is(true));
    }

    @Test
    void testQueryEmployeeById_NotFound() {
        String query = """
            {
                "query": "{ employee(id: \\"non-existent\\") { id title } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.employee", is(nullValue()));
    }

    @Test
    void testQueryEmployeeByPersonId() {
        String query = """
            {
                "query": "{ employeeByPersonId(personId: \\"person-002\\") { id title department } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.employeeByPersonId.id", is("emp-002"))
            .body("data.employeeByPersonId.title", is("Senior Engineer"))
            .body("data.employeeByPersonId.department", is("Engineering"));
    }

    @Test
    void testQueryEmployeesByDepartment() {
        String query = """
            {
                "query": "{ employeesByDepartment(department: \\"Engineering\\") { id title department } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.employeesByDepartment", hasSize(2))
            .body("data.employeesByDepartment.department", everyItem(is("Engineering")));
    }

    @Test
    void testQueryActiveEmployees() {
        String query = """
            {
                "query": "{ activeEmployees { id title active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.activeEmployees", hasSize(2))
            .body("data.activeEmployees.active", everyItem(is(true)));
    }

    @Test
    void testMutationAssignEmployee() {
        String mutation = """
            {
                "query": "mutation { assignEmployee(personId: \\"person-004\\", title: \\"Data Analyst\\", department: \\"Analytics\\", salary: 75000) { id personId title department salary active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.assignEmployee.personId", is("person-004"))
            .body("data.assignEmployee.title", is("Data Analyst"))
            .body("data.assignEmployee.department", is("Analytics"))
            .body("data.assignEmployee.active", is(true))
            .body("data.assignEmployee.id", startsWith("emp-"));
    }

    @Test
    void testMutationAssignEmployee_AlreadyExists() {
        String mutation = """
            {
                "query": "mutation { assignEmployee(personId: \\"person-001\\", title: \\"New Title\\", department: \\"New Dept\\", salary: 50000) { id } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.assignEmployee", is(nullValue()));
    }

    @Test
    void testMutationPromoteEmployee() {
        String mutation = """
            {
                "query": "mutation { promoteEmployee(id: \\"emp-001\\", newTitle: \\"Staff Engineer\\", newSalary: 150000) { id title salary } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.promoteEmployee.id", is("emp-001"))
            .body("data.promoteEmployee.title", is("Staff Engineer"));
    }

    @Test
    void testMutationPromoteEmployee_NotFound() {
        String mutation = """
            {
                "query": "mutation { promoteEmployee(id: \\"non-existent\\", newTitle: \\"Staff Engineer\\", newSalary: 150000) { id } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.promoteEmployee", is(nullValue()));
    }

    @Test
    void testMutationTransferEmployee() {
        String mutation = """
            {
                "query": "mutation { transferEmployee(id: \\"emp-001\\", newDepartment: \\"Platform\\") { id department } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.transferEmployee.id", is("emp-001"))
            .body("data.transferEmployee.department", is("Platform"));
    }

    @Test
    void testMutationTerminateEmployee() {
        String mutation = """
            {
                "query": "mutation { terminateEmployee(id: \\"emp-001\\") { id active } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.terminateEmployee.id", is("emp-001"))
            .body("data.terminateEmployee.active", is(false));
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

    @Test
    void testGraphQLSchemaAvailable() {
        given()
        .when()
            .get("/graphql/schema.graphql")
        .then()
            .statusCode(200)
            .body(containsString("Employee"));
    }
}
