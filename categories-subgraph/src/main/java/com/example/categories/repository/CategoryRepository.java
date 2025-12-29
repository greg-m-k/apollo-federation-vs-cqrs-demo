package com.example.categories.repository;

import com.example.categories.model.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Repository for Category entities using Panache.
 */
@ApplicationScoped
public class CategoryRepository implements PanacheRepositoryBase<Category, String> {

    /**
     * Find a category by name.
     */
    public Optional<Category> findByName(String name) {
        return find("name", name).firstResultOptional();
    }
}
