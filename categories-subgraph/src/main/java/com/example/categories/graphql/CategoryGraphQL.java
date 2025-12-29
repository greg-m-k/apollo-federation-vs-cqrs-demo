package com.example.categories.graphql;

import com.example.categories.model.Category;
import com.example.categories.model.Product;
import com.example.categories.repository.CategoryRepository;
import io.smallrye.graphql.api.federation.FieldSet;
import io.smallrye.graphql.api.federation.Requires;
import io.smallrye.graphql.api.federation.Resolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.List;

/**
 * GraphQL API for Categories subgraph.
 * This subgraph owns Category and extends Product with category field.
 */
@GraphQLApi
@ApplicationScoped
public class CategoryGraphQL {

    @Inject
    CategoryRepository categoryRepository;

    @Query("categories")
    @Description("Get all categories")
    public List<Category> getAllCategories() {
        return categoryRepository.listAll();
    }

    @Query("category")
    @Description("Get a category by ID")
    public Category getCategory(@Name("id") String id) {
        return categoryRepository.findById(id);
    }

    /**
     * Federation entity resolver for Category.
     * Called when other subgraphs reference a Category.
     */
    @Resolver
    public Category resolveCategory(@Name("id") String id) {
        return categoryRepository.findById(id);
    }

    /**
     * Federation entity resolver for Product.
     * Resolves Product entities when the router needs category information.
     * This makes Product visible as an entity in this subgraph.
     */
    @Resolver
    public Product resolveProduct(@Name("id") String id, @Name("categoryId") String categoryId) {
        Product product = new Product(id);
        product.setCategoryId(categoryId);
        return product;
    }

    /**
     * Field resolver that adds 'category' field to Product.
     * This is resolved by the categories-subgraph when a Product is queried.
     * @Requires tells the router to fetch categoryId from products subgraph first.
     */
    @Requires(fields = @FieldSet("categoryId"))
    @Description("The category this product belongs to")
    public Category category(@Source Product product) {
        if (product.getCategoryId() == null) {
            return null;
        }
        return categoryRepository.findById(product.getCategoryId());
    }
}
