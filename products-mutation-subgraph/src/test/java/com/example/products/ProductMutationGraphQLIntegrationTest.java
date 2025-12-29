package com.example.products;

import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
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
 * Integration tests for GraphQL mutation endpoints using real H2 database.
 * These tests run without Docker.
 */
@QuarkusTest
class ProductMutationGraphQLIntegrationTest {

    @Inject
    ProductRepository productRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void testCreateProduct() {
        String mutation = """
            {
                "query": "mutation { createProduct(input: { name: \\"Test Widget\\", description: \\"A test widget\\", price: 29.99, categoryId: \\"cat-001\\" }) { id name description price categoryId } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.createProduct.name", is("Test Widget"))
            .body("data.createProduct.description", is("A test widget"))
            .body("data.createProduct.price", is(29.99f))
            .body("data.createProduct.categoryId", is("cat-001"))
            .body("data.createProduct.id", notNullValue());
    }

    @Test
    void testCreateProductWithMinimalFields() {
        String mutation = """
            {
                "query": "mutation { createProduct(input: { name: \\"Minimal Product\\", price: 9.99 }) { id name price } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.createProduct.name", is("Minimal Product"))
            .body("data.createProduct.price", is(9.99f))
            .body("data.createProduct.id", notNullValue());
    }

    @Test
    void testUpdateProduct() {
        // First, create a product to update in a committed transaction
        createProductForUpdate();

        String mutation = """
            {
                "query": "mutation { updateProduct(id: \\"prod-update-001\\", input: { name: \\"Updated Name\\", description: \\"Updated description\\", price: 39.99, categoryId: \\"cat-updated\\" }) { id name description price categoryId } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.updateProduct.id", is("prod-update-001"))
            .body("data.updateProduct.name", is("Updated Name"))
            .body("data.updateProduct.description", is("Updated description"))
            .body("data.updateProduct.price", is(39.99f))
            .body("data.updateProduct.categoryId", is("cat-updated"));
    }

    @Transactional
    void createProductForUpdate() {
        Product product = new Product("prod-update-001", "Original Name", "Original description",
                new BigDecimal("19.99"), "cat-original");
        productRepository.persist(product);
    }

    @Test
    void testUpdateProductPartial() {
        // Create a product with all fields in a committed transaction
        createProductForPartialUpdate();

        // Update only the name
        String mutation = """
            {
                "query": "mutation { updateProduct(id: \\"prod-partial-001\\", input: { name: \\"New Name Only\\" }) { id name description price categoryId } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.updateProduct.id", is("prod-partial-001"))
            .body("data.updateProduct.name", is("New Name Only"))
            .body("data.updateProduct.description", is("Original description"))
            .body("data.updateProduct.price", is(19.99f))
            .body("data.updateProduct.categoryId", is("cat-original"));
    }

    @Transactional
    void createProductForPartialUpdate() {
        Product product = new Product("prod-partial-001", "Original Name", "Original description",
                new BigDecimal("19.99"), "cat-original");
        productRepository.persist(product);
    }

    @Test
    void testUpdateProductNotFound() {
        String mutation = """
            {
                "query": "mutation { updateProduct(id: \\"non-existent\\", input: { name: \\"Updated\\" }) { id name } }"
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
            .body("data.updateProduct", is(nullValue()));
    }

    @Test
    void testDeleteProduct() {
        // Create a product to delete in a committed transaction
        createProductForDelete();

        String mutation = """
            {
                "query": "mutation { deleteProduct(id: \\"prod-delete-001\\") }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.deleteProduct", is(true));
    }

    @Transactional
    void createProductForDelete() {
        Product product = new Product("prod-delete-001", "To Be Deleted", "Will be deleted",
                new BigDecimal("99.99"), "cat-delete");
        productRepository.persist(product);
    }

    @Test
    void testDeleteProductNotFound() {
        String mutation = """
            {
                "query": "mutation { deleteProduct(id: \\"non-existent\\") }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(mutation)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.deleteProduct", is(false));
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
