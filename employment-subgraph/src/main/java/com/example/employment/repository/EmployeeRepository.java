package com.example.employment.repository;

import com.example.employment.model.Employee;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Employee entities using Panache.
 */
@ApplicationScoped
public class EmployeeRepository implements PanacheRepositoryBase<Employee, String> {

    /**
     * Find an employee by person ID.
     */
    public Optional<Employee> findByPersonId(String personId) {
        return find("personId", personId).firstResultOptional();
    }

    /**
     * Find all employees in a department.
     */
    public List<Employee> findByDepartment(String department) {
        return find("department", department).list();
    }

    /**
     * Find all active employees.
     */
    public List<Employee> findAllActive() {
        return find("active", true).list();
    }

    /**
     * Find employees by title pattern.
     */
    public List<Employee> findByTitle(String title) {
        return find("LOWER(title) LIKE LOWER(?1)", "%" + title + "%").list();
    }
}
