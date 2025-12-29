package com.example.categories.graphql;

import com.example.categories.dto.CategoryInput;
import com.example.categories.model.Category;
import com.example.categories.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import io.smallrye.graphql.api.federation.Resolver;

import java.util.UUID;

/**
 * GraphQL API for Category mutations.
 * This subgraph owns Category mutation operations.
 */
@GraphQLApi
@ApplicationScoped
public class CategoryMutationGraphQL {

    @Inject
    CategoryRepository categoryRepository;

    @Mutation("createCategory")
    @Description("Create a new category")
    @Transactional
    public Category createCategory(@Name("input") CategoryInput input) {
        Category category = new Category();
        category.id = UUID.randomUUID().toString();
        category.name = input.name;
        category.description = input.description;

        categoryRepository.persist(category);
        return category;
    }

    @Mutation("updateCategory")
    @Description("Update an existing category")
    @Transactional
    public Category updateCategory(@Name("id") String id, @Name("input") CategoryInput input) {
        Category category = categoryRepository.findById(id);
        if (category == null) {
            throw new RuntimeException("Category not found: " + id);
        }

        if (input.name != null) {
            category.name = input.name;
        }
        if (input.description != null) {
            category.description = input.description;
        }

        return category;
    }

    @Mutation("deleteCategory")
    @Description("Delete a category by ID")
    @Transactional
    public Boolean deleteCategory(@Name("id") String id) {
        Category category = categoryRepository.findById(id);
        if (category == null) {
            return false;
        }

        categoryRepository.delete(category);
        return true;
    }

    /**
     * Federation entity resolver.
     * Called by Apollo Router to resolve Category references.
     */
    @Resolver
    public Category resolveCategory(@Name("id") String id) {
        return categoryRepository.findById(id);
    }
}
