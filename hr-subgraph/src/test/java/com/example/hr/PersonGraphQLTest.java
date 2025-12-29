package com.example.hr;

import com.example.hr.graphql.PersonGraphQL;
import com.example.hr.model.Person;
import com.example.hr.repository.PersonRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PersonGraphQL using mocked repository.
 * These tests run without Docker.
 */
@QuarkusTest
class PersonGraphQLTest {

    @Inject
    PersonGraphQL personGraphQL;

    @InjectMock
    PersonRepository personRepository;

    private Person testPerson1;
    private Person testPerson2;

    @BeforeEach
    void setUp() {
        Mockito.reset(personRepository);

        testPerson1 = new Person("person-001", "John Doe", "john.doe@example.com",
                LocalDate.of(2020, 1, 15));
        testPerson2 = new Person("person-002", "Jane Smith", "jane.smith@example.com",
                LocalDate.of(2021, 3, 20));
    }

    @Test
    void testGetAllPersons() {
        when(personRepository.listAll()).thenReturn(Arrays.asList(testPerson1, testPerson2));

        List<Person> persons = personGraphQL.getAllPersons();

        assertThat(persons).hasSize(2);
        assertThat(persons.get(0).name).isEqualTo("John Doe");
        assertThat(persons.get(1).name).isEqualTo("Jane Smith");
    }

    @Test
    void testGetAllPersons_Empty() {
        when(personRepository.listAll()).thenReturn(Collections.emptyList());

        List<Person> persons = personGraphQL.getAllPersons();

        assertThat(persons).isEmpty();
    }

    @Test
    void testGetPerson() {
        when(personRepository.findById("person-001")).thenReturn(testPerson1);

        Person person = personGraphQL.getPerson("person-001");

        assertThat(person).isNotNull();
        assertThat(person.id).isEqualTo("person-001");
        assertThat(person.name).isEqualTo("John Doe");
        assertThat(person.email).isEqualTo("john.doe@example.com");
    }

    @Test
    void testGetPerson_NotFound() {
        when(personRepository.findById("non-existent")).thenReturn(null);

        Person person = personGraphQL.getPerson("non-existent");

        assertThat(person).isNull();
    }

    @Test
    void testGetPersonByEmail() {
        when(personRepository.findByEmail("jane.smith@example.com"))
                .thenReturn(Optional.of(testPerson2));

        Person person = personGraphQL.getPersonByEmail("jane.smith@example.com");

        assertThat(person).isNotNull();
        assertThat(person.name).isEqualTo("Jane Smith");
    }

    @Test
    void testGetPersonByEmail_NotFound() {
        when(personRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        Person person = personGraphQL.getPersonByEmail("nonexistent@example.com");

        assertThat(person).isNull();
    }

    @Test
    void testGetActivePersons() {
        when(personRepository.findAllActive()).thenReturn(Arrays.asList(testPerson1, testPerson2));

        List<Person> activePersons = personGraphQL.getActivePersons();

        assertThat(activePersons).hasSize(2);
    }

    @Test
    void testSearchPersons() {
        when(personRepository.searchByName("john")).thenReturn(Collections.singletonList(testPerson1));

        List<Person> results = personGraphQL.searchPersons("john");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name).isEqualTo("John Doe");
    }

    @Test
    void testResolvePerson() {
        when(personRepository.findById("person-002")).thenReturn(testPerson2);

        Person person = personGraphQL.resolvePerson("person-002");

        assertThat(person).isNotNull();
        assertThat(person.id).isEqualTo("person-002");
        assertThat(person.name).isEqualTo("Jane Smith");
    }
}
