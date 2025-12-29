package com.example.products;

import com.example.products.dto.CreateProduct;
import com.example.products.graphql.ProductMutationGraphQL;
import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductMutationGraphQL using mocked repository.
 * These tests run without Docker.
 */
@QuarkusTest
class ProductMutationGraphQLTest {

    @Inject
    ProductMutationGraphQL productMutationGraphQL;

    @InjectMock
    ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        Mockito.reset(productRepository);

        testProduct = new Product("prod-001", "Widget", "A useful widget",
                new BigDecimal("19.99"), "cat-electronics");
    }

    @Test
    void testCreateProduct() {
        CreateProduct input = new CreateProduct();
        input.name = "New Product";
        input.description = "New description";
        input.price = new BigDecimal("49.99");
        input.categoryId = "cat-new";

        doNothing().when(productRepository).persist(any(Product.class));

        Product result = productMutationGraphQL.createProduct(input);

        assertThat(result).isNotNull();
        assertThat(result.id).isNotNull();
        assertThat(result.name).isEqualTo("New Product");
        assertThat(result.description).isEqualTo("New description");
        assertThat(result.price).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(result.categoryId).isEqualTo("cat-new");

        verify(productRepository).persist(any(Product.class));
    }

    @Test
    void testCreateProductWithMinimalFields() {
        CreateProduct input = new CreateProduct();
        input.name = "Minimal Product";
        input.price = new BigDecimal("9.99");

        doNothing().when(productRepository).persist(any(Product.class));

        Product result = productMutationGraphQL.createProduct(input);

        assertThat(result).isNotNull();
        assertThat(result.id).isNotNull();
        assertThat(result.name).isEqualTo("Minimal Product");
        assertThat(result.description).isNull();
        assertThat(result.price).isEqualByComparingTo(new BigDecimal("9.99"));
        assertThat(result.categoryId).isNull();

        verify(productRepository).persist(any(Product.class));
    }

    @Test
    void testUpdateProduct() {
        when(productRepository.findById("prod-001")).thenReturn(testProduct);

        CreateProduct input = new CreateProduct();
        input.name = "Updated Widget";
        input.description = "Updated description";
        input.price = new BigDecimal("29.99");
        input.categoryId = "cat-updated";

        Product result = productMutationGraphQL.updateProduct("prod-001", input);

        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo("prod-001");
        assertThat(result.name).isEqualTo("Updated Widget");
        assertThat(result.description).isEqualTo("Updated description");
        assertThat(result.price).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(result.categoryId).isEqualTo("cat-updated");
    }

    @Test
    void testUpdateProductPartial_OnlyName() {
        when(productRepository.findById("prod-001")).thenReturn(testProduct);

        CreateProduct input = new CreateProduct();
        input.name = "Only Name Changed";
        // Other fields are null

        Product result = productMutationGraphQL.updateProduct("prod-001", input);

        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("Only Name Changed");
        // Original values should be preserved
        assertThat(result.description).isEqualTo("A useful widget");
        assertThat(result.price).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(result.categoryId).isEqualTo("cat-electronics");
    }

    @Test
    void testUpdateProductNotFound() {
        when(productRepository.findById("non-existent")).thenReturn(null);

        CreateProduct input = new CreateProduct();
        input.name = "Updated Name";

        assertThatThrownBy(() -> productMutationGraphQL.updateProduct("non-existent", input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void testDeleteProduct() {
        when(productRepository.findById("prod-001")).thenReturn(testProduct);
        doNothing().when(productRepository).delete(testProduct);

        Boolean result = productMutationGraphQL.deleteProduct("prod-001");

        assertThat(result).isTrue();
        verify(productRepository).delete(testProduct);
    }

    @Test
    void testDeleteProductNotFound() {
        when(productRepository.findById("non-existent")).thenReturn(null);

        Boolean result = productMutationGraphQL.deleteProduct("non-existent");

        assertThat(result).isFalse();
        verify(productRepository, never()).delete(any());
    }

    @Test
    void testResolveProduct() {
        when(productRepository.findById("prod-001")).thenReturn(testProduct);

        Product result = productMutationGraphQL.resolveProduct("prod-001");

        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo("prod-001");
        assertThat(result.name).isEqualTo("Widget");
    }

    @Test
    void testResolveProductNotFound() {
        when(productRepository.findById("non-existent")).thenReturn(null);

        Product result = productMutationGraphQL.resolveProduct("non-existent");

        assertThat(result).isNull();
    }
}
