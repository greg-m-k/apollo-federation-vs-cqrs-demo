package com.example.hr.repository;

import com.example.hr.model.Person;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Person entities using Panache.
 */
@ApplicationScoped
public class PersonRepository implements PanacheRepositoryBase<Person, String> {

    /**
     * Find a person by email.
     */
    public Optional<Person> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    /**
     * Find all active persons.
     */
    public List<Person> findAllActive() {
        return find("active", true).list();
    }

    /**
     * Find persons by name pattern (case-insensitive).
     */
    public List<Person> searchByName(String namePattern) {
        return find("LOWER(name) LIKE LOWER(?1)", "%" + namePattern + "%").list();
    }
}
