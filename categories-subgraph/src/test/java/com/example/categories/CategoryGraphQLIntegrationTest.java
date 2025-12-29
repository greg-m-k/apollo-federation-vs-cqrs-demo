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
 * Integration tests for GraphQL endpoint using real H2 database.
 * These tests run without Docker.
 */
@QuarkusTest
class CategoryGraphQLIntegrationTest {

    @Inject
    CategoryRepository categoryRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        categoryRepository.deleteAll();

        Category category1 = new Category("cat-electronics", "Electronics", "Electronic devices and accessories");
        Category category2 = new Category("cat-books", "Books", "Books and literature");

        categoryRepository.persist(category1);
        categoryRepository.persist(category2);
    }

    @Test
    void testQueryAllCategories() {
        String query = """
            {
                "query": "{ categories { id name description } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.categories", hasSize(2))
            .body("data.categories[0].name", is(oneOf("Electronics", "Books")))
            .body("data.categories[1].name", is(oneOf("Electronics", "Books")));
    }

    @Test
    void testQueryCategoryById() {
        String query = """
            {
                "query": "{ category(id: \\"cat-electronics\\") { id name description } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.category.id", is("cat-electronics"))
            .body("data.category.name", is("Electronics"))
            .body("data.category.description", is("Electronic devices and accessories"));
    }

    @Test
    void testQueryCategoryById_NotFound() {
        String query = """
            {
                "query": "{ category(id: \\"non-existent\\") { id name } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.category", is(nullValue()));
    }

    @Test
    void testQueryCategoriesWithPartialFields() {
        String query = """
            {
                "query": "{ categories { id name } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.categories", hasSize(2))
            .body("data.categories.find { it.id == 'cat-electronics' }.name", is("Electronics"))
            .body("data.categories.find { it.id == 'cat-books' }.name", is("Books"));
    }

    @Test
    void testGraphQLSchemaEndpoint() {
        // Verify that GraphQL endpoint is accessible
        String introspectionQuery = """
            {
                "query": "{ __schema { types { name } } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(introspectionQuery)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.__schema.types", is(notNullValue()))
            .body("data.__schema.types.name", hasItem("Category"));
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
    void testQueryAfterDataUpdate() {
        // Add a new category in a separate transaction that commits
        addToysCategory();

        String query = """
            {
                "query": "{ categories { id name } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.categories", hasSize(3))
            .body("data.categories.name", hasItems("Electronics", "Books", "Toys"));
    }

    @Transactional
    void addToysCategory() {
        Category newCategory = new Category("cat-toys", "Toys", "Games and toys");
        categoryRepository.persist(newCategory);
    }

    @Test
    void testQueryCategoryWithEmptyDescription() {
        // First, add a category without description
        addCategoryWithoutDescription();

        String query = """
            {
                "query": "{ category(id: \\"cat-misc\\") { id name description } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.category.id", is("cat-misc"))
            .body("data.category.name", is("Miscellaneous"))
            .body("data.category.description", is(nullValue()));
    }

    @Transactional
    void addCategoryWithoutDescription() {
        Category category = new Category("cat-misc", "Miscellaneous", null);
        categoryRepository.persist(category);
    }
}
