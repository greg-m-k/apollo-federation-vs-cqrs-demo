package com.example.products.graphql;

import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import io.smallrye.graphql.api.federation.Resolver;

import java.util.List;

/**
 * GraphQL API for Products subgraph.
 * This subgraph owns the Product entity.
 */
@GraphQLApi
@ApplicationScoped
public class ProductGraphQL {

    @Inject
    ProductRepository productRepository;

    @Query("products")
    @Description("Get all products")
    public List<Product> getAllProducts() {
        return productRepository.listAll();
    }

    @Query("product")
    @Description("Get a product by ID")
    public Product getProduct(@Name("id") String id) {
        return productRepository.findById(id);
    }

    /**
     * Federation entity resolver.
     * Called by Apollo Router to resolve Product references from other subgraphs.
     */
    @Resolver
    public Product resolveProduct(@Name("id") String id) {
        return productRepository.findById(id);
    }
}
