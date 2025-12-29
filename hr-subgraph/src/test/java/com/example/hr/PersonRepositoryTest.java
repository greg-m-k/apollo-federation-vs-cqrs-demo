package com.example.hr;

import com.example.hr.model.Person;
import com.example.hr.repository.PersonRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PersonRepository using H2 in-memory database.
 * These tests run without Docker.
 */
@QuarkusTest
class PersonRepositoryTest {

    @Inject
    PersonRepository personRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        personRepository.deleteAll();

        Person person1 = new Person("person-001", "John Doe", "john.doe@example.com",
                LocalDate.of(2020, 1, 15));
        Person person2 = new Person("person-002", "Jane Smith", "jane.smith@example.com",
                LocalDate.of(2021, 3, 20));
        Person person3 = new Person("person-003", "Bob Johnson", "bob.johnson@example.com",
                LocalDate.of(2022, 6, 10));
        person3.active = false;

        personRepository.persist(person1);
        personRepository.persist(person2);
        personRepository.persist(person3);
    }

    @Test
    void testFindById() {
        Person person = personRepository.findById("person-001");

        assertThat(person).isNotNull();
        assertThat(person.name).isEqualTo("John Doe");
        assertThat(person.email).isEqualTo("john.doe@example.com");
        assertThat(person.active).isTrue();
    }

    @Test
    void testFindById_NotFound() {
        Person person = personRepository.findById("non-existent");

        assertThat(person).isNull();
    }

    @Test
    void testListAll() {
        List<Person> persons = personRepository.listAll();

        assertThat(persons).hasSize(3);
        assertThat(persons).extracting(p -> p.name)
                .containsExactlyInAnyOrder("John Doe", "Jane Smith", "Bob Johnson");
    }

    @Test
    void testFindByEmail() {
        Optional<Person> person = personRepository.findByEmail("jane.smith@example.com");

        assertThat(person).isPresent();
        assertThat(person.get().name).isEqualTo("Jane Smith");
    }

    @Test
    void testFindByEmail_NotFound() {
        Optional<Person> person = personRepository.findByEmail("nonexistent@example.com");

        assertThat(person).isEmpty();
    }

    @Test
    void testFindAllActive() {
        List<Person> activePersons = personRepository.findAllActive();

        assertThat(activePersons).hasSize(2);
        assertThat(activePersons).extracting(p -> p.name)
                .containsExactlyInAnyOrder("John Doe", "Jane Smith");
        assertThat(activePersons).allMatch(p -> p.active);
    }

    @Test
    void testSearchByName() {
        List<Person> results = personRepository.searchByName("john");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(p -> p.name)
                .containsExactlyInAnyOrder("John Doe", "Bob Johnson");
    }

    @Test
    void testSearchByName_CaseInsensitive() {
        List<Person> results = personRepository.searchByName("JANE");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name).isEqualTo("Jane Smith");
    }

    @Test
    void testSearchByName_NoResults() {
        List<Person> results = personRepository.searchByName("xyz");

        assertThat(results).isEmpty();
    }

    @Test
    @Transactional
    void testPersistPerson() {
        Person newPerson = new Person("person-004", "Alice Brown", "alice.brown@example.com",
                LocalDate.now());
        personRepository.persist(newPerson);

        Person found = personRepository.findById("person-004");
        assertThat(found).isNotNull();
        assertThat(found.name).isEqualTo("Alice Brown");
        assertThat(found.active).isTrue();
    }

    @Test
    @Transactional
    void testUpdatePerson() {
        Person person = personRepository.findById("person-001");
        person.name = "John Updated";
        personRepository.persist(person);

        Person updated = personRepository.findById("person-001");
        assertThat(updated.name).isEqualTo("John Updated");
    }

    @Test
    @Transactional
    void testDeletePerson() {
        boolean deleted = personRepository.deleteById("person-001");

        assertThat(deleted).isTrue();
        assertThat(personRepository.findById("person-001")).isNull();
        assertThat(personRepository.listAll()).hasSize(2);
    }
}
