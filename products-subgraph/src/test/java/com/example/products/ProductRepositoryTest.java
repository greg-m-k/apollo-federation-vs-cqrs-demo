package com.example.products;

import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProductRepository using H2 in-memory database.
 * These tests run without Docker.
 */
@QuarkusTest
class ProductRepositoryTest {

    @Inject
    ProductRepository productRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up and set up test data
        productRepository.deleteAll();

        Product product1 = new Product("prod-001", "Widget", "A useful widget",
                new BigDecimal("19.99"), "cat-electronics");
        Product product2 = new Product("prod-002", "Gadget", "A fancy gadget",
                new BigDecimal("49.99"), "cat-electronics");
        Product product3 = new Product("prod-003", "Book", "An interesting book",
                new BigDecimal("14.99"), "cat-books");

        productRepository.persist(product1);
        productRepository.persist(product2);
        productRepository.persist(product3);
    }

    @Test
    void testFindById() {
        Product product = productRepository.findById("prod-001");

        assertThat(product).isNotNull();
        assertThat(product.name).isEqualTo("Widget");
        assertThat(product.price).isEqualByComparingTo(new BigDecimal("19.99"));
    }

    @Test
    void testFindById_NotFound() {
        Product product = productRepository.findById("non-existent");

        assertThat(product).isNull();
    }

    @Test
    void testListAll() {
        List<Product> products = productRepository.listAll();

        assertThat(products).hasSize(3);
        assertThat(products).extracting(p -> p.name)
                .containsExactlyInAnyOrder("Widget", "Gadget", "Book");
    }

    @Test
    void testFindByCategoryId() {
        List<Product> electronics = productRepository.findByCategoryId("cat-electronics");

        assertThat(electronics).hasSize(2);
        assertThat(electronics).extracting(p -> p.name)
                .containsExactlyInAnyOrder("Widget", "Gadget");
    }

    @Test
    void testFindByCategoryId_EmptyResult() {
        List<Product> products = productRepository.findByCategoryId("cat-nonexistent");

        assertThat(products).isEmpty();
    }

    @Test
    @Transactional
    void testPersistProduct() {
        Product newProduct = new Product("prod-004", "New Item", "Description",
                new BigDecimal("99.99"), "cat-new");
        productRepository.persist(newProduct);

        Product found = productRepository.findById("prod-004");
        assertThat(found).isNotNull();
        assertThat(found.name).isEqualTo("New Item");
    }

    @Test
    @Transactional
    void testDeleteProduct() {
        boolean deleted = productRepository.deleteById("prod-001");

        assertThat(deleted).isTrue();
        assertThat(productRepository.findById("prod-001")).isNull();
        assertThat(productRepository.listAll()).hasSize(2);
    }

    @Test
    void testGetPriceAsDouble() {
        Product product = productRepository.findById("prod-001");

        assertThat(product.getPriceAsDouble()).isEqualTo(19.99);
    }
}
