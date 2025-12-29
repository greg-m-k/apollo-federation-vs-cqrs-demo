package com.example.categories;

import com.example.categories.model.Category;
import com.example.categories.repository.CategoryRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CategoryRepository using H2 in-memory database.
 * These tests run without Docker.
 */
@QuarkusTest
class CategoryRepositoryTest {

    @Inject
    CategoryRepository categoryRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up and set up test data
        categoryRepository.deleteAll();

        Category category1 = new Category("cat-001", "Electronics", "Electronic devices and accessories");
        Category category2 = new Category("cat-002", "Books", "Books and reading materials");
        Category category3 = new Category("cat-003", "Clothing", "Apparel and fashion items");

        categoryRepository.persist(category1);
        categoryRepository.persist(category2);
        categoryRepository.persist(category3);
    }

    @Test
    void testFindById() {
        Category category = categoryRepository.findById("cat-001");

        assertThat(category).isNotNull();
        assertThat(category.name).isEqualTo("Electronics");
        assertThat(category.description).isEqualTo("Electronic devices and accessories");
    }

    @Test
    void testFindById_NotFound() {
        Category category = categoryRepository.findById("non-existent");

        assertThat(category).isNull();
    }

    @Test
    void testListAll() {
        List<Category> categories = categoryRepository.listAll();

        assertThat(categories).hasSize(3);
        assertThat(categories).extracting(c -> c.name)
                .containsExactlyInAnyOrder("Electronics", "Books", "Clothing");
    }

    @Test
    void testFindByName() {
        Optional<Category> category = categoryRepository.findByName("Electronics");

        assertThat(category).isPresent();
        assertThat(category.get().id).isEqualTo("cat-001");
        assertThat(category.get().description).isEqualTo("Electronic devices and accessories");
    }

    @Test
    void testFindByName_NotFound() {
        Optional<Category> category = categoryRepository.findByName("Non-existent");

        assertThat(category).isEmpty();
    }

    @Test
    @Transactional
    void testPersistCategory() {
        Category newCategory = new Category("cat-004", "Home & Garden", "Home improvement and garden supplies");
        categoryRepository.persist(newCategory);

        Category found = categoryRepository.findById("cat-004");
        assertThat(found).isNotNull();
        assertThat(found.name).isEqualTo("Home & Garden");
        assertThat(found.description).isEqualTo("Home improvement and garden supplies");
    }

    @Test
    @Transactional
    void testDeleteCategory() {
        boolean deleted = categoryRepository.deleteById("cat-001");

        assertThat(deleted).isTrue();
        assertThat(categoryRepository.findById("cat-001")).isNull();
        assertThat(categoryRepository.listAll()).hasSize(2);
    }

    @Test
    @Transactional
    void testDeleteCategoryById_NotFound() {
        boolean deleted = categoryRepository.deleteById("non-existent");

        assertThat(deleted).isFalse();
        assertThat(categoryRepository.listAll()).hasSize(3);
    }

    @Test
    @Transactional
    void testUpdateCategory() {
        Category category = categoryRepository.findById("cat-001");
        category.name = "Updated Electronics";
        category.description = "Updated description";

        // In Panache, updates are automatically persisted within transaction
        Category updated = categoryRepository.findById("cat-001");
        assertThat(updated.name).isEqualTo("Updated Electronics");
        assertThat(updated.description).isEqualTo("Updated description");
    }

    @Test
    void testCount() {
        long count = categoryRepository.count();

        assertThat(count).isEqualTo(3);
    }
}
