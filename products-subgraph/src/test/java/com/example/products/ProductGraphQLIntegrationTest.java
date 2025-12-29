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
 * Integration tests for GraphQL endpoint using real H2 database.
 * These tests run without Docker.
 */
@QuarkusTest
class ProductGraphQLIntegrationTest {

    @Inject
    ProductRepository productRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        productRepository.deleteAll();

        Product product1 = new Product("prod-001", "Widget", "A useful widget",
                new BigDecimal("19.99"), "cat-electronics");
        Product product2 = new Product("prod-002", "Gadget", "A fancy gadget",
                new BigDecimal("49.99"), "cat-electronics");

        productRepository.persist(product1);
        productRepository.persist(product2);
    }

    @Test
    void testQueryAllProducts() {
        String query = """
            {
                "query": "{ products { id name price description } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.products", hasSize(2))
            .body("data.products[0].name", is(oneOf("Widget", "Gadget")))
            .body("data.products[1].name", is(oneOf("Widget", "Gadget")));
    }

    @Test
    void testQueryProductById() {
        String query = """
            {
                "query": "{ product(id: \\"prod-001\\") { id name price description categoryId } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.product.id", is("prod-001"))
            .body("data.product.name", is("Widget"))
            .body("data.product.price", is(19.99f))
            .body("data.product.categoryId", is("cat-electronics"));
    }

    @Test
    void testQueryProductById_NotFound() {
        String query = """
            {
                "query": "{ product(id: \\"non-existent\\") { id name } }"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("data.product", is(nullValue()));
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
