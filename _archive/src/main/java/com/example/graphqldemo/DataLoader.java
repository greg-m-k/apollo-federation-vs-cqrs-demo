package com.example.graphqldemo;

import com.example.graphqldemo.model.Category;
import com.example.graphqldemo.model.Product;
import com.example.graphqldemo.repository.CategoryRepository;
import com.example.graphqldemo.repository.ProductRepository;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Loads sample data at application startup.
 * Creates categories and products for demonstration purposes.
 */
@ApplicationScoped
@Startup
public class DataLoader {

    private static final Logger LOG = Logger.getLogger(DataLoader.class);

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    ProductRepository productRepository;

    @PostConstruct
    void loadData() {
        LOG.info("Loading sample data...");

        // Create categories
        Category electronics = new Category("1", "Electronics", "Gadgets and devices");
        Category books = new Category("2", "Books", "Reading materials");
        Category clothing = new Category("3", "Clothing", "Apparel and accessories");

        categoryRepository.save(electronics);
        categoryRepository.save(books);
        categoryRepository.save(clothing);

        LOG.infof("Loaded %d categories", 3);

        // Create products
        productRepository.save(new Product("1", "Laptop", "High-performance laptop", 999.99, "1"));
        productRepository.save(new Product("2", "Smartphone", "Latest smartphone model", 699.99, "1"));
        productRepository.save(new Product("3", "Java Book", "Learn Java programming", 49.99, "2"));
        productRepository.save(new Product("4", "T-Shirt", "Cotton t-shirt", 29.99, "3"));
        productRepository.save(new Product("5", "Headphones", "Wireless headphones", 199.99, "1"));

        LOG.infof("Loaded %d products", 5);
        LOG.info("Sample data loaded successfully. GraphQL endpoint ready at /graphql");
    }
}
