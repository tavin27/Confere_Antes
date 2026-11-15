package com.confereantes.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.confereantes.model.Alert;
import com.confereantes.model.User;
import com.confereantes.model.enums.ContactChannel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldSaveAlertSuccessfully() {
        // Arrange: cria e salva um usuário
        User user = new User();
        user.setUsername("usuario_teste");
        user.setEmail("teste@email.com");
        user.setPasswordHash("senha_hash");

        User savedUser = userRepository.save(user);

        // Arrange: cria uma sessão de análise associada ao usuário
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);

        // Act: salva a sessão
        Alert savedAlert = alertRepository.save(alert);

        // Assert: verifica os resultados
        assertNotNull(savedAlert.getId());
        assertNull(savedAlert.getOverallRiskScore());
        assertNotNull(savedAlert.getCreatedAt());
        assertEquals(savedUser.getId(), savedAlert.getUser().getId());
    }

    @Test
    void shouldFindAlertById() {
        // Arrange: cria e salva um usuário
        User user = new User();
        user.setUsername("usuario_busca");
        user.setEmail("busca@email.com");
        user.setPasswordHash("senha_hash");

        User savedUser = userRepository.save(user);

        // Arrange: cria e salva uma sessão
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);

        Alert savedAlert = alertRepository.save(alert);

        // Act: busca a sessão pelo ID
        var foundAlert = alertRepository.findById(savedAlert.getId());

        // Assert: verifica se encontrou a sessão
        assertTrue(foundAlert.isPresent());
        assertEquals(savedAlert.getId(), foundAlert.get().getId());
    }

    @Test
    void shouldFindAllAlerts() {
        // Arrange: cria e salva um usuário
        User user = new User();
        user.setUsername("usuario_lista");
        user.setEmail("lista@email.com");
        user.setPasswordHash("senha_hash");

        User savedUser = userRepository.save(user);

        // Arrange: cria duas sessões
        Alert alert1 = new Alert();
        alert1.setUser(savedUser);
        alert1.setContactChannel(ContactChannel.WHATSAPP);

        Alert alert2 = new Alert();
        alert2.setUser(savedUser);
        alert2.setContactChannel(ContactChannel.LIGACAO);

        alertRepository.save(alert1);
        alertRepository.save(alert2);

        // Act
        var alerts = alertRepository.findAll();

        // Assert
        assertEquals(2, alerts.size());
    }

    @Test
    void shouldDeleteAlertById() {
        // Arrange: cria e salva um usuário
        User user = new User();
        user.setUsername("usuario_exclusao");
        user.setEmail("exclusao@email.com");
        user.setPasswordHash("senha_hash");

        User savedUser = userRepository.save(user);

        // Arrange: cria e salva uma sessão
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);

        Alert savedAlert = alertRepository.save(alert);

        // Act: exclui a sessão
        alertRepository.deleteById(savedAlert.getId());

        // Assert: verifica se foi excluída
        assertFalse(alertRepository.findById(savedAlert.getId()).isPresent());
    }

    @Test
    void shouldMaintainUserRelationship() {
        // Arrange: cria e salva um usuário
        User user = new User();
        user.setUsername("usuario_relacionamento");
        user.setEmail("relacionamento@email.com");
        user.setPasswordHash("senha_hash");

        User savedUser = userRepository.save(user);

        // Arrange: cria e salva uma sessão
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);

        Alert savedAlert = alertRepository.save(alert);

        // Act: busca a sessão novamente
        entityManager.flush();
        entityManager.clear();

        var foundAlert = alertRepository.findById(savedAlert.getId());

        // Assert: verifica o relacionamento
        assertTrue(foundAlert.isPresent());
        assertEquals(savedUser.getId(), foundAlert.get().getUser().getId());
    }

    @Test
    void shouldNotSaveAlertWithoutUser() {
        // Arrange: cria uma sessão sem usuário
        Alert alert = new Alert();
        alert.setContactChannel(ContactChannel.WHATSAPP);

        // Act e Assert: verifica se a persistência falha
        assertThrows(Exception.class, () -> {
            alertRepository.saveAndFlush(alert);
        });
    }

    @Test
    void shouldUpdateAlertOverallRiskScore() {
        // Arrange: cria e salva um usuário
        User user = new User();
        user.setUsername("usuario_atualizacao");
        user.setEmail("atualizacao@email.com");
        user.setPasswordHash("senha_hash");

        User savedUser = userRepository.save(user);

        // Arrange: cria e salva uma sessão
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);

        Alert savedAlert = alertRepository.save(alert);

        // Act: atualiza a pontuação geral
        savedAlert.setOverallRiskScore(75);
        alertRepository.save(savedAlert);

        entityManager.flush();
        entityManager.clear();

        // Assert: verifica a atualização
        var updatedAlert = alertRepository.findById(savedAlert.getId());

        assertTrue(updatedAlert.isPresent());
        assertEquals(75, updatedAlert.get().getOverallRiskScore());
    }
}
