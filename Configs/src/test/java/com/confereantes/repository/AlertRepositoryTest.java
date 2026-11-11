package com.confereantes.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.confereantes.model.Alert;
import com.confereantes.model.User;
import com.confereantes.model.enums.ContactChannel;
import com.confereantes.model.enums.AlertStatus;

@DataJpaTest
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private UserRepository userRepository;

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
}
