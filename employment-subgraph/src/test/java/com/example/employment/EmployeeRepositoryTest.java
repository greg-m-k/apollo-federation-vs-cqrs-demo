package com.example.employment;

import com.example.employment.model.Employee;
import com.example.employment.repository.EmployeeRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EmployeeRepository using H2 in-memory database.
 * These tests run without Docker.
 */
@QuarkusTest
class EmployeeRepositoryTest {

    @Inject
    EmployeeRepository employeeRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        employeeRepository.deleteAll();

        Employee emp1 = new Employee("emp-001", "person-001", "Software Engineer",
                "Engineering", new BigDecimal("85000.00"));
        Employee emp2 = new Employee("emp-002", "person-002", "Senior Engineer",
                "Engineering", new BigDecimal("120000.00"));
        Employee emp3 = new Employee("emp-003", "person-003", "Product Manager",
                "Product", new BigDecimal("110000.00"));
        emp3.active = false;

        employeeRepository.persist(emp1);
        employeeRepository.persist(emp2);
        employeeRepository.persist(emp3);
    }

    @Test
    void testFindById() {
        Employee employee = employeeRepository.findById("emp-001");

        assertThat(employee).isNotNull();
        assertThat(employee.title).isEqualTo("Software Engineer");
        assertThat(employee.department).isEqualTo("Engineering");
        assertThat(employee.salary).isEqualByComparingTo(new BigDecimal("85000.00"));
        assertThat(employee.active).isTrue();
    }

    @Test
    void testFindById_NotFound() {
        Employee employee = employeeRepository.findById("non-existent");

        assertThat(employee).isNull();
    }

    @Test
    void testListAll() {
        List<Employee> employees = employeeRepository.listAll();

        assertThat(employees).hasSize(3);
        assertThat(employees).extracting(e -> e.id)
                .containsExactlyInAnyOrder("emp-001", "emp-002", "emp-003");
    }

    @Test
    void testFindByPersonId() {
        Optional<Employee> employee = employeeRepository.findByPersonId("person-002");

        assertThat(employee).isPresent();
        assertThat(employee.get().id).isEqualTo("emp-002");
        assertThat(employee.get().title).isEqualTo("Senior Engineer");
    }

    @Test
    void testFindByPersonId_NotFound() {
        Optional<Employee> employee = employeeRepository.findByPersonId("person-999");

        assertThat(employee).isEmpty();
    }

    @Test
    void testFindByDepartment() {
        List<Employee> engineers = employeeRepository.findByDepartment("Engineering");

        assertThat(engineers).hasSize(2);
        assertThat(engineers).extracting(e -> e.title)
                .containsExactlyInAnyOrder("Software Engineer", "Senior Engineer");
    }

    @Test
    void testFindByDepartment_Empty() {
        List<Employee> employees = employeeRepository.findByDepartment("NonExistent");

        assertThat(employees).isEmpty();
    }

    @Test
    void testFindAllActive() {
        List<Employee> activeEmployees = employeeRepository.findAllActive();

        assertThat(activeEmployees).hasSize(2);
        assertThat(activeEmployees).extracting(e -> e.id)
                .containsExactlyInAnyOrder("emp-001", "emp-002");
        assertThat(activeEmployees).allMatch(e -> e.active);
    }

    @Test
    void testFindByTitle() {
        List<Employee> results = employeeRepository.findByTitle("engineer");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(e -> e.title)
                .containsExactlyInAnyOrder("Software Engineer", "Senior Engineer");
    }

    @Test
    void testFindByTitle_CaseInsensitive() {
        List<Employee> results = employeeRepository.findByTitle("MANAGER");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title).isEqualTo("Product Manager");
    }

    @Test
    void testFindByTitle_NoResults() {
        List<Employee> results = employeeRepository.findByTitle("xyz");

        assertThat(results).isEmpty();
    }

    @Test
    @Transactional
    void testPersistEmployee() {
        Employee newEmployee = new Employee("emp-004", "person-004", "Data Scientist",
                "Data", new BigDecimal("95000.00"));
        employeeRepository.persist(newEmployee);

        Employee found = employeeRepository.findById("emp-004");
        assertThat(found).isNotNull();
        assertThat(found.title).isEqualTo("Data Scientist");
        assertThat(found.department).isEqualTo("Data");
        assertThat(found.active).isTrue();
    }

    @Test
    @Transactional
    void testUpdateEmployee() {
        Employee employee = employeeRepository.findById("emp-001");
        employee.title = "Staff Engineer";
        employee.salary = new BigDecimal("150000.00");
        employeeRepository.persist(employee);

        Employee updated = employeeRepository.findById("emp-001");
        assertThat(updated.title).isEqualTo("Staff Engineer");
        assertThat(updated.salary).isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    @Test
    @Transactional
    void testDeleteEmployee() {
        boolean deleted = employeeRepository.deleteById("emp-001");

        assertThat(deleted).isTrue();
        assertThat(employeeRepository.findById("emp-001")).isNull();
        assertThat(employeeRepository.listAll()).hasSize(2);
    }

    @Test
    @Transactional
    void testTerminateEmployee() {
        Employee employee = employeeRepository.findById("emp-001");
        employee.active = false;
        employeeRepository.persist(employee);

        Employee terminated = employeeRepository.findById("emp-001");
        assertThat(terminated.active).isFalse();

        List<Employee> activeEmployees = employeeRepository.findAllActive();
        assertThat(activeEmployees).hasSize(1);
        assertThat(activeEmployees.get(0).id).isEqualTo("emp-002");
    }
}
