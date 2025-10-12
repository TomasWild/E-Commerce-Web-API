package com.wild.ecommerce.user;

import com.wild.ecommerce.authentication.VerificationToken;
import com.wild.ecommerce.util.TestAuditorConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(TestAuditorConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

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
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test.user@example.com");
        user.setPassword("secret");
        user.setRole(Role.USER);
        user.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void save_ShouldPersistUser() {
        // Act
        User saved = userRepository.save(user);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test.user@example.com");
    }

    @Test
    void findById_ShouldReturnUser_WhenExists() {
        // Arrange
        User saved = entityManager.persistAndFlush(user);

        // Act
        Optional<User> found = userRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test.user@example.com");
    }

    @Test
    void delete_ShouldRemoveUser() {
        // Arrange
        User saved = entityManager.persistAndFlush(user);

        // Act
        userRepository.delete(saved);
        userRepository.flush();

        // Assert
        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void update_ShouldModifyUserDetails() {
        // Arrange
        User saved = entityManager.persistAndFlush(user);

        // Act
        saved.setFirstName("Jane");
        saved.setLastName("Doe");
        userRepository.saveAndFlush(saved);
        entityManager.clear();

        // Assert
        Optional<User> updated = userRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getFirstName()).isEqualTo("Jane");
        assertThat(updated.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        // Arrange
        entityManager.persistAndFlush(user);

        // Act
        Optional<User> found = userRepository.findByEmailIgnoreCase("test.user@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Test");
        assertThat(found.get().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenUserDoesNotExist() {
        // Act
        Optional<User> found = userRepository.findByEmailIgnoreCase("notfound@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void save_ShouldCascadeVerificationTokens() {
        VerificationToken token = new VerificationToken();
        token.setToken("12345");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        user.setVerificationTokens(List.of(token));

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getVerificationTokens()).hasSize(1);
        assertThat(saved.getVerificationTokens().getFirst().getId()).isNotNull();
    }

    @Test
    void removeVerificationToken_ShouldOrphanDelete() {
        VerificationToken token = new VerificationToken();
        token.setToken("12345");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        user.setVerificationTokens(new ArrayList<>(List.of(token)));

        User saved = userRepository.saveAndFlush(user);

        saved.getVerificationTokens().clear();
        userRepository.saveAndFlush(saved);

        assertThat(entityManager.find(VerificationToken.class, token.getId())).isNull();
    }
}
