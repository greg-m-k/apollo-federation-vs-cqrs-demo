package com.example.graphqldemo.repository;

import com.example.graphqldemo.model.Category;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for Category entities.
 * Uses ConcurrentHashMap for thread-safe storage.
 */
@ApplicationScoped
public class CategoryRepository {

    private final Map<String, Category> categories = new ConcurrentHashMap<>();

    /**
     * Get all categories.
     *
     * @return list of all categories
     */
    public List<Category> findAll() {
        return new ArrayList<>(categories.values());
    }

    /**
     * Find a category by ID.
     *
     * @param id the category ID
     * @return optional containing the category if found
     */
    public Optional<Category> findById(String id) {
        return Optional.ofNullable(categories.get(id));
    }

    /**
     * Save a category.
     *
     * @param category the category to save
     * @return the saved category
     */
    public Category save(Category category) {
        categories.put(category.getId(), category);
        return category;
    }

    /**
     * Delete all categories (for testing).
     */
    public void deleteAll() {
        categories.clear();
    }
}
