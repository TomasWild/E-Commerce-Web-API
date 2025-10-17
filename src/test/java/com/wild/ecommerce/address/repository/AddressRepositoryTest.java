package com.wild.ecommerce.address.repository;

import com.wild.ecommerce.address.model.Address;
import com.wild.ecommerce.util.TestAuditorConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(TestAuditorConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AddressRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("testDB")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Address address1;
    private Address address2;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();

        address1 = new Address();
        address1.setCountry("USA");
        address1.setState("California");
        address1.setCity("San Francisco");
        address1.setStreet("123 Market St");
        address1.setPostalCode("94102");

        address2 = new Address();
        address2.setCountry("USA");
        address2.setState("New York");
        address2.setCity("New York");
        address2.setStreet("456 Broadway");
        address2.setPostalCode("10013");
    }

    @Test
    void shouldPersistAddressSuccessfully() {
        // When
        Address savedAddress = addressRepository.save(address1);
        entityManager.flush();

        // Then
        assertThat(savedAddress).isNotNull();
        assertThat(savedAddress.getId()).isNotNull();
        assertThat(savedAddress.getCountry()).isEqualTo("USA");
        assertThat(savedAddress.getState()).isEqualTo("California");
        assertThat(savedAddress.getCity()).isEqualTo("San Francisco");
    }

    @Test
    void shouldRetrieveAddressByIdWhenExists() {
        // Given
        Address savedAddress = addressRepository.save(address1);
        entityManager.flush();

        // When
        Optional<Address> foundAddress = addressRepository.findById(savedAddress.getId());

        // Then
        assertThat(foundAddress).isPresent();
        assertThat(foundAddress.get().getId()).isEqualTo(savedAddress.getId());
        assertThat(foundAddress.get().getCountry()).isEqualTo("USA");
    }

    @Test
    void shouldReturnEmptyOptionalWhenAddressDoesNotExist() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<Address> foundAddress = addressRepository.findById(nonExistentId);

        // Then
        assertThat(foundAddress).isEmpty();
    }

    @Test
    void shouldRetrieveAllSavedAddresses() {
        // Given
        addressRepository.save(address1);
        addressRepository.save(address2);
        entityManager.flush();

        // When
        List<Address> addresses = addressRepository.findAll();

        // Then
        assertThat(addresses).hasSize(2);
        assertThat(addresses).extracting(Address::getCity)
                .containsExactlyInAnyOrder("San Francisco", "New York");
    }

    @Test
    void shouldUpdateExistingAddressFields() {
        // Given
        Address savedAddress = addressRepository.save(address1);
        entityManager.flush();
        entityManager.clear();

        // When
        savedAddress.setCity("Los Angeles");
        savedAddress.setPostalCode("90001");
        Address updatedAddress = addressRepository.save(savedAddress);
        entityManager.flush();

        // Then
        assertThat(updatedAddress.getCity()).isEqualTo("Los Angeles");
        assertThat(updatedAddress.getPostalCode()).isEqualTo("90001");
        assertThat(addressRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldDeleteAddressEntitySuccessfully() {
        // Given
        Address savedAddress = addressRepository.save(address1);
        entityManager.flush();
        UUID addressId = savedAddress.getId();

        // When
        addressRepository.delete(savedAddress);
        entityManager.flush();

        // Then
        Optional<Address> deletedAddress = addressRepository.findById(addressId);
        assertThat(deletedAddress).isEmpty();
        assertThat(addressRepository.count()).isEqualTo(0);
    }

    @Test
    void shouldDeleteAddressByIdSuccessfully() {
        // Given
        Address savedAddress = addressRepository.save(address1);
        entityManager.flush();
        UUID addressId = savedAddress.getId();

        // When
        addressRepository.deleteById(addressId);
        entityManager.flush();

        // Then
        assertThat(addressRepository.findById(addressId)).isEmpty();
    }

    @Test
    void shouldReturnTrueIfAddressExistsOtherwiseFalse() {
        // Given
        Address savedAddress = addressRepository.save(address1);
        entityManager.flush();

        // When & Then
        assertThat(addressRepository.existsById(savedAddress.getId())).isTrue();
        assertThat(addressRepository.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldReturnTotalAddressCount() {
        // Given
        addressRepository.save(address1);
        addressRepository.save(address2);
        entityManager.flush();

        // When
        long count = addressRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindAddressesMatchingSingleSpecification() {
        // Given
        addressRepository.save(address1);
        addressRepository.save(address2);
        entityManager.flush();

        // When
        Specification<Address> spec = (root, _, cb) ->
                cb.equal(root.get("state"), "California");
        List<Address> addresses = addressRepository.findAll(spec);

        // Then
        assertThat(addresses).hasSize(1);
        assertThat(addresses.getFirst().getCity()).isEqualTo("San Francisco");
    }

    @Test
    void shouldFindAddressesMatchingMultipleSpecifications() {
        // Given
        addressRepository.save(address1);
        addressRepository.save(address2);
        entityManager.flush();

        // When
        Specification<Address> spec = (root, _, cb) ->
                cb.and(
                        cb.equal(root.get("country"), "USA"),
                        cb.equal(root.get("state"), "New York")
                );
        List<Address> addresses = addressRepository.findAll(spec);

        // Then
        assertThat(addresses).hasSize(1);
        assertThat(addresses.getFirst().getCity()).isEqualTo("New York");
    }

    @Test
    void shouldRetrieveSingleAddressMatchingSpecification() {
        // Given
        Address savedAddress = addressRepository.save(address1);
        entityManager.flush();

        // When
        Specification<Address> spec = (root, _, cb) ->
                cb.equal(root.get("postalCode"), "94102");
        Optional<Address> foundAddress = addressRepository.findOne(spec);

        // Then
        assertThat(foundAddress).isPresent();
        assertThat(foundAddress.get().getId()).isEqualTo(savedAddress.getId());
    }

    @Test
    void shouldCountAddressesMatchingSpecification() {
        // Given
        addressRepository.save(address1);
        addressRepository.save(address2);
        entityManager.flush();

        // When
        Specification<Address> spec = (root, _, cb) ->
                cb.equal(root.get("country"), "USA");
        long count = addressRepository.count(spec);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldDeleteAllAddressesSuccessfully() {
        // Given
        addressRepository.save(address1);
        addressRepository.save(address2);
        entityManager.flush();

        // When
        addressRepository.deleteAll();
        entityManager.flush();

        // Then
        assertThat(addressRepository.count()).isEqualTo(0);
    }
}
