package com.wild.ecommerce.auth.repository;

import com.wild.ecommerce.auth.model.VerificationToken;
import com.wild.ecommerce.user.model.Role;
import com.wild.ecommerce.user.model.User;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(TestAuditorConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class VerificationTokenRepositoryTest {

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
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private VerificationToken verificationToken;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.USER);
        user = entityManager.persistAndFlush(user);

        verificationToken = new VerificationToken();
        verificationToken.setToken("test-token-12345");
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        verificationToken.setUser(user);
    }

    @AfterEach
    void tearDown() {
        verificationTokenRepository.deleteAll();
        entityManager.clear();
    }

    @Test
    void save_ShouldPersistVerificationToken() {
        // Act
        VerificationToken savedToken = verificationTokenRepository.save(verificationToken);

        // Assert
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getToken()).isEqualTo("test-token-12345");
        assertThat(savedToken.getUser()).isEqualTo(user);
    }

    @Test
    void save_ShouldUpdateVerificationTokenFields_WhenModified() {
        // Arrange
        VerificationToken savedToken = verificationTokenRepository.save(verificationToken);
        entityManager.flush();

        // Act
        savedToken.setUsed(true);
        savedToken.setRevoked(true);
        VerificationToken updatedToken = verificationTokenRepository.save(savedToken);
        entityManager.flush();

        // Assert
        Optional<VerificationToken> found = verificationTokenRepository.findById(updatedToken.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isUsed()).isTrue();
        assertThat(found.get().isRevoked()).isTrue();
    }

    @Test
    void findAll_ShouldReturnAllSavedVerificationTokens() {
        // Arrange
        verificationTokenRepository.save(verificationToken);

        VerificationToken anotherToken = new VerificationToken();
        anotherToken.setToken("another-token-67890");
        anotherToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        anotherToken.setUser(user);
        verificationTokenRepository.save(anotherToken);

        entityManager.flush();

        // Act
        List<VerificationToken> allTokens = verificationTokenRepository.findAll();

        // Assert
        assertThat(allTokens).hasSize(2);
    }

    @Test
    void findByToken_ShouldReturnMatchingVerificationToken_WhenTokenExists() {
        // Arrange
        verificationTokenRepository.save(verificationToken);
        entityManager.flush();

        // Act
        Optional<VerificationToken> found = verificationTokenRepository.findByToken("test-token-12345");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("test-token-12345");
        assertThat(found.get().getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByToken_ShouldReturnEmpty_WhenTokenDoesNotExist() {
        // Act
        Optional<VerificationToken> found = verificationTokenRepository.findByToken("non-existent-token");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void delete_ShouldRemoveVerificationTokenFromDatabase() {
        // Arrange
        VerificationToken savedToken = verificationTokenRepository.save(verificationToken);
        UUID tokenId = savedToken.getId();
        entityManager.flush();

        // Act
        verificationTokenRepository.delete(savedToken);
        entityManager.flush();

        // Assert
        Optional<VerificationToken> found = verificationTokenRepository.findById(tokenId);
        assertThat(found).isEmpty();
    }

    @Test
    void isExpired_ShouldReturnTrue_WhenExpiryDateIsInPast() {
        // Arrange
        verificationToken.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        VerificationToken _ = verificationTokenRepository.save(verificationToken);
        entityManager.flush();

        // Act
        Optional<VerificationToken> found = verificationTokenRepository.findByToken("test-token-12345");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().isExpired()).isTrue();
    }

    @Test
    void isExpired_ShouldReturnFalse_WhenExpiryDateIsInFuture() {
        // Arrange
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        VerificationToken _ = verificationTokenRepository.save(verificationToken);
        entityManager.flush();

        // Act
        Optional<VerificationToken> found = verificationTokenRepository.findByToken("test-token-12345");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().isExpired()).isFalse();
    }
}
