package com.example.categories;

import com.example.categories.dto.CategoryInput;
import com.example.categories.graphql.CategoryMutationGraphQL;
import com.example.categories.model.Category;
import com.example.categories.repository.CategoryRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryMutationGraphQL using mocked repository.
 * These tests run without Docker.
 */
@QuarkusTest
class CategoryMutationGraphQLTest {

    @Inject
    CategoryMutationGraphQL categoryMutationGraphQL;

    @InjectMock
    CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        Mockito.reset(categoryRepository);

        testCategory = new Category("cat-001", "Electronics", "Electronic devices and accessories");
    }

    @Test
    void testCreateCategory() {
        CategoryInput input = new CategoryInput();
        input.name = "New Category";
        input.description = "New category description";

        doNothing().when(categoryRepository).persist(any(Category.class));

        Category result = categoryMutationGraphQL.createCategory(input);

        assertThat(result).isNotNull();
        assertThat(result.id).isNotNull();
        assertThat(result.name).isEqualTo("New Category");
        assertThat(result.description).isEqualTo("New category description");

        verify(categoryRepository).persist(any(Category.class));
    }

    @Test
    void testCreateCategoryWithMinimalFields() {
        CategoryInput input = new CategoryInput();
        input.name = "Minimal Category";

        doNothing().when(categoryRepository).persist(any(Category.class));

        Category result = categoryMutationGraphQL.createCategory(input);

        assertThat(result).isNotNull();
        assertThat(result.id).isNotNull();
        assertThat(result.name).isEqualTo("Minimal Category");
        assertThat(result.description).isNull();

        verify(categoryRepository).persist(any(Category.class));
    }

    @Test
    void testUpdateCategory() {
        when(categoryRepository.findById("cat-001")).thenReturn(testCategory);

        CategoryInput input = new CategoryInput();
        input.name = "Updated Electronics";
        input.description = "Updated description";

        Category result = categoryMutationGraphQL.updateCategory("cat-001", input);

        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo("cat-001");
        assertThat(result.name).isEqualTo("Updated Electronics");
        assertThat(result.description).isEqualTo("Updated description");
    }

    @Test
    void testUpdateCategoryPartial_OnlyName() {
        when(categoryRepository.findById("cat-001")).thenReturn(testCategory);

        CategoryInput input = new CategoryInput();
        input.name = "Only Name Changed";
        // Description is null

        Category result = categoryMutationGraphQL.updateCategory("cat-001", input);

        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("Only Name Changed");
        // Original description should be preserved
        assertThat(result.description).isEqualTo("Electronic devices and accessories");
    }

    @Test
    void testUpdateCategoryPartial_OnlyDescription() {
        when(categoryRepository.findById("cat-001")).thenReturn(testCategory);

        CategoryInput input = new CategoryInput();
        input.description = "Only Description Changed";
        // Name is null

        Category result = categoryMutationGraphQL.updateCategory("cat-001", input);

        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("Electronics");
        assertThat(result.description).isEqualTo("Only Description Changed");
    }

    @Test
    void testUpdateCategoryNotFound() {
        when(categoryRepository.findById("non-existent")).thenReturn(null);

        CategoryInput input = new CategoryInput();
        input.name = "Updated Name";

        assertThatThrownBy(() -> categoryMutationGraphQL.updateCategory("non-existent", input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void testDeleteCategory() {
        when(categoryRepository.findById("cat-001")).thenReturn(testCategory);
        doNothing().when(categoryRepository).delete(testCategory);

        Boolean result = categoryMutationGraphQL.deleteCategory("cat-001");

        assertThat(result).isTrue();
        verify(categoryRepository).delete(testCategory);
    }

    @Test
    void testDeleteCategoryNotFound() {
        when(categoryRepository.findById("non-existent")).thenReturn(null);

        Boolean result = categoryMutationGraphQL.deleteCategory("non-existent");

        assertThat(result).isFalse();
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void testResolveCategory() {
        when(categoryRepository.findById("cat-001")).thenReturn(testCategory);

        Category result = categoryMutationGraphQL.resolveCategory("cat-001");

        assertThat(result).isNotNull();
        assertThat(result.id).isEqualTo("cat-001");
        assertThat(result.name).isEqualTo("Electronics");
    }

    @Test
    void testResolveCategoryNotFound() {
        when(categoryRepository.findById("non-existent")).thenReturn(null);

        Category result = categoryMutationGraphQL.resolveCategory("non-existent");

        assertThat(result).isNull();
    }
}
