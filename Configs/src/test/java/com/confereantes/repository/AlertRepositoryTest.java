package com.confereantes.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.confereantes.model.Alert;
import com.confereantes.model.User;
import com.confereantes.model.enums.ContactChannel;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.UserRepository;
import com.confereantes.model.enums.AlertStatus;

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

        // Arrange: cria um alerta associado ao usuário
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);
        alert.setContent("Recebi uma mensagem suspeita de um suposto funcionário do banco.");

        // Act: salva o alerta
        Alert savedAlert = alertRepository.save(alert);

        // Assert: verifica os resultados
        assertNotNull(savedAlert.getId());
        assertEquals(AlertStatus.PENDENTE_ANALISE, savedAlert.getStatus());
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

        // Arrange: cria e salva um alerta
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);
        alert.setContent("Mensagem suspeita para testar a busca.");

        Alert savedAlert = alertRepository.save(alert);

        // Act: busca o alerta pelo ID
        var foundAlert = alertRepository.findById(savedAlert.getId());

        // Assert: verifica se encontrou o alerta
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

        // Arrange: cria dois alertas
        Alert alert1 = new Alert();
        alert1.setUser(savedUser);
        alert1.setContactChannel(ContactChannel.WHATSAPP);
        alert1.setContent("Primeiro alerta.");

        Alert alert2 = new Alert();
        alert2.setUser(savedUser);
        alert2.setContactChannel(ContactChannel.LIGACAO);
        alert2.setContent("Segundo alerta.");

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

        // Arrange: cria e salva um alerta
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);
        alert.setContent("Alerta para testar exclusão.");

        Alert savedAlert = alertRepository.save(alert);

        // Act: exclui o alerta
        alertRepository.deleteById(savedAlert.getId());

        // Assert: verifica se foi excluído
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

        // Arrange: cria e salva um alerta
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);
        alert.setContent("Mensagem para testar o relacionamento.");

        Alert savedAlert = alertRepository.save(alert);

        // Act: busca o alerta novamente
        entityManager.flush();
        entityManager.clear();

        var foundAlert = alertRepository.findById(savedAlert.getId());

        // Assert: verifica o relacionamento
        assertTrue(foundAlert.isPresent());
        assertEquals(savedUser.getId(), foundAlert.get().getUser().getId());
    }

    @Test
    void shouldNotSaveAlertWithoutUser() {
        // Arrange: cria um alerta sem usuário
        Alert alert = new Alert();
        alert.setContactChannel(ContactChannel.WHATSAPP);
        alert.setContent("Alerta sem usuário associado.");

        // Act e Assert: verifica se a persistência falha
        assertThrows(Exception.class, () -> {
            alertRepository.saveAndFlush(alert);
        });
    }

    @Test
    void shouldUpdateAlertContent() {
        // Arrange: cria e salva um usuário
        User user = new User();
        user.setUsername("usuario_atualizacao");
        user.setEmail("atualizacao@email.com");
        user.setPasswordHash("senha_hash");

        User savedUser = userRepository.save(user);

        // Arrange: cria e salva um alerta
        Alert alert = new Alert();
        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);
        alert.setContent("Mensagem original.");

        Alert savedAlert = alertRepository.save(alert);

        // Act: atualiza o conteúdo
        savedAlert.setContent("Mensagem atualizada.");
        alertRepository.save(savedAlert);

        // Assert: verifica a atualização
        var updatedAlert = alertRepository.findById(savedAlert.getId());

        assertTrue(updatedAlert.isPresent());
        assertEquals("Mensagem atualizada.", updatedAlert.get().getContent());
    }
}
