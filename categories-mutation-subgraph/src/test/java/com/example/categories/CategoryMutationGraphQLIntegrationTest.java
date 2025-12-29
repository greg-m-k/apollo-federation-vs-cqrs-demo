package com.example.categories;

import com.example.categories.model.Category;
import com.example.categories.repository.CategoryRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for GraphQL mutation endpoints using real H2 database.
 * These tests run without Docker.
 */
@QuarkusTest
class CategoryMutationGraphQLIntegrationTest {

    @Inject
    CategoryRepository categoryRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        categoryRepository.deleteAll();
    }

    @Test
    void testCreateCategory() {
        String mutation = """
            {
                "query": "mutation { createCategory(input: { name: \\"Electronics\\", description: \\"Electronic devices and accessories\\" }) { id name description } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.createCategory.name", is("Electronics"))
            .body("data.createCategory.description", is("Electronic devices and accessories"))
            .body("data.createCategory.id", notNullValue());
    }

    @Test
    void testCreateCategoryWithMinimalFields() {
        String mutation = """
            {
                "query": "mutation { createCategory(input: { name: \\"Books\\" }) { id name description } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.createCategory.name", is("Books"))
            .body("data.createCategory.description", is(nullValue()))
            .body("data.createCategory.id", notNullValue());
    }

    @Test
    void testUpdateCategory() {
        // First, create a category to update in a committed transaction
        createCategoryForUpdate();

        String mutation = """
            {
                "query": "mutation { updateCategory(id: \\"cat-update-001\\", input: { name: \\"Updated Name\\", description: \\"Updated description\\" }) { id name description } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.updateCategory.id", is("cat-update-001"))
            .body("data.updateCategory.name", is("Updated Name"))
            .body("data.updateCategory.description", is("Updated description"));
    }

    @Transactional
    void createCategoryForUpdate() {
        Category category = new Category("cat-update-001", "Original Name", "Original description");
        categoryRepository.persist(category);
    }

    @Test
    void testUpdateCategoryPartial() {
        // Create a category with all fields in a committed transaction
        createCategoryForPartialUpdate();

        // Update only the name
        String mutation = """
            {
                "query": "mutation { updateCategory(id: \\"cat-partial-001\\", input: { name: \\"New Name Only\\" }) { id name description } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.updateCategory.id", is("cat-partial-001"))
            .body("data.updateCategory.name", is("New Name Only"))
            .body("data.updateCategory.description", is("Original description"));
    }

    @Transactional
    void createCategoryForPartialUpdate() {
        Category category = new Category("cat-partial-001", "Original Name", "Original description");
        categoryRepository.persist(category);
    }

    @Test
    void testUpdateCategoryNotFound() {
        String mutation = """
            {
                "query": "mutation { updateCategory(id: \\"non-existent\\", input: { name: \\"Updated\\" }) { id name } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("errors", hasSize(greaterThan(0)))
            .body("data.updateCategory", is(nullValue()));
    }

    @Test
    void testDeleteCategory() {
        // Create a category to delete in a committed transaction
        createCategoryForDelete();

        String mutation = """
            {
                "query": "mutation { deleteCategory(id: \\"cat-delete-001\\") }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.deleteCategory", is(true));
    }

    @Transactional
    void createCategoryForDelete() {
        Category category = new Category("cat-delete-001", "To Be Deleted", "Will be deleted");
        categoryRepository.persist(category);
    }

    @Test
    void testDeleteCategoryNotFound() {
        String mutation = """
            {
                "query": "mutation { deleteCategory(id: \\"non-existent\\") }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.deleteCategory", is(false));
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
