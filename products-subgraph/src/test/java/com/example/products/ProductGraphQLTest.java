package com.example.products;

import com.example.products.graphql.ProductGraphQL;
import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProductGraphQL using mocked repository.
 * These tests run without Docker.
 */
@QuarkusTest
class ProductGraphQLTest {

    @Inject
    ProductGraphQL productGraphQL;

    @InjectMock
    ProductRepository productRepository;

    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        Mockito.reset(productRepository);

        testProduct1 = new Product("prod-001", "Widget", "A useful widget",
                new BigDecimal("19.99"), "cat-electronics");
        testProduct2 = new Product("prod-002", "Gadget", "A fancy gadget",
                new BigDecimal("49.99"), "cat-electronics");
    }

    @Test
    void testGetAllProducts() {
        when(productRepository.listAll()).thenReturn(Arrays.asList(testProduct1, testProduct2));

        List<Product> products = productGraphQL.getAllProducts();

        assertThat(products).hasSize(2);
        assertThat(products.get(0).name).isEqualTo("Widget");
        assertThat(products.get(1).name).isEqualTo("Gadget");
    }

    @Test
    void testGetAllProducts_Empty() {
        when(productRepository.listAll()).thenReturn(Collections.emptyList());

        List<Product> products = productGraphQL.getAllProducts();

        assertThat(products).isEmpty();
    }

    @Test
    void testGetProduct() {
        when(productRepository.findById("prod-001")).thenReturn(testProduct1);

        Product product = productGraphQL.getProduct("prod-001");

        assertThat(product).isNotNull();
        assertThat(product.id).isEqualTo("prod-001");
        assertThat(product.name).isEqualTo("Widget");
        assertThat(product.price).isEqualByComparingTo(new BigDecimal("19.99"));
    }

    @Test
    void testGetProduct_NotFound() {
        when(productRepository.findById("non-existent")).thenReturn(null);

        Product product = productGraphQL.getProduct("non-existent");

        assertThat(product).isNull();
    }

    @Test
    void testResolveProduct() {
        when(productRepository.findById("prod-002")).thenReturn(testProduct2);

        Product product = productGraphQL.resolveProduct("prod-002");

        assertThat(product).isNotNull();
        assertThat(product.id).isEqualTo("prod-002");
        assertThat(product.name).isEqualTo("Gadget");
    }

    @Test
    void testResolveProduct_NotFound() {
        when(productRepository.findById("invalid")).thenReturn(null);

        Product product = productGraphQL.resolveProduct("invalid");

        assertThat(product).isNull();
    }
}
