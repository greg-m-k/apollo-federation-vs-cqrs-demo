package com.example.products.graphql;

import com.example.products.dto.CreateProduct;
import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
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
 * GraphQL API for Product mutations.
 * This subgraph owns Product mutation operations.
 */
@GraphQLApi
@ApplicationScoped
public class ProductMutationGraphQL {

    @Inject
    ProductRepository productRepository;

    @Mutation("createProduct")
    @Description("Create a new product")
    @Transactional
    public Product createProduct(@Name("input") CreateProduct input) {
        Product product = new Product();
        product.id = UUID.randomUUID().toString();
        product.name = input.name;
        product.description = input.description;
        product.price = input.price;
        product.categoryId = input.categoryId;

        productRepository.persist(product);
        return product;
    }

    @Mutation("updateProduct")
    @Description("Update an existing product")
    @Transactional
    public Product updateProduct(@Name("id") String id, @Name("input") CreateProduct input) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new RuntimeException("Product not found: " + id);
        }

        if (input.name != null) {
            product.name = input.name;
        }
        if (input.description != null) {
            product.description = input.description;
        }
        if (input.price != null) {
            product.price = input.price;
        }
        if (input.categoryId != null) {
            product.categoryId = input.categoryId;
        }

        return product;
    }

    @Mutation("deleteProduct")
    @Description("Delete a product by ID")
    @Transactional
    public Boolean deleteProduct(@Name("id") String id) {
        Product product = productRepository.findById(id);
        if (product == null) {
            return false;
        }

        productRepository.delete(product);
        return true;
    }

    /**
     * Federation entity resolver.
     * Called by Apollo Router to resolve Product references.
     */
    @Resolver
    public Product resolveProduct(@Name("id") String id) {
        return productRepository.findById(id);
    }
}
