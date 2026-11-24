
package com.confereantes.rules.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.confereantes.model.Message;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.rules.RuleMatch;
import com.confereantes.rules.RuleType;

class CredentialRuleTest {

    private CredentialRule credentialRule;

    @BeforeEach
    void setUp() {
        credentialRule = new CredentialRule();
    }

    private Message createMessage(String content, MessageSender sender) {
        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        return message;
    }

    @Test
    void shouldDetectDirectPasswordRequest() {
        Message message = createMessage(
                "Informe sua senha para confirmar sua identidade.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
        assertEquals(
                RuleType.SOLICITACAO_SENHA,
                matches.get(0).ruleType()
        );
    }

    @Test
    void shouldDetectPasswordRequestWithBlockingThreat() {
        Message message = createMessage(
                "Informe sua senha ou sua conta será bloqueada.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertTrue(matches.stream().anyMatch(
                match -> match.ruleType() == RuleType.SOLICITACAO_SENHA
        ));
    }

    @Test
    void shouldNotDetectSecurityGuidanceAsPasswordRequest() {
        Message message = createMessage(
                "Nunca informe sua senha para terceiros.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldDetectAccessGuidance() {
        Message message = createMessage(
                "Digite sua senha de acesso para continuar.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertTrue(matches.stream().anyMatch(
                match -> match.ruleType() == RuleType.SOLICITACAO_SENHA
        ));
    }

    @Test
    void shouldDetectDirectAuthenticationCodeRequest() {
        Message message = createMessage(
                "Informe o código de autenticação recebido.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertTrue(matches.stream().anyMatch(
                match -> match.ruleType()
                        == RuleType.SOLICITACAO_CODIGO_AUTENTICACAO
        ));
    }

    @Test
    void shouldDetectTokenRequestReceivedOnPhone() {
        Message message = createMessage(
                "Me passa o token que chegou no seu celular.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertTrue(matches.stream().anyMatch(
                match -> match.ruleType()
                        == RuleType.SOLICITACAO_CODIGO_AUTENTICACAO
        ));
    }

    @Test
    void shouldNotDetectSecurityGuidanceAsCodeRequest() {
        Message message = createMessage(
                "Nunca compartilhe seu código de autenticação.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldNotDetectNotificationThatCodeWasSent() {
        Message message = createMessage(
                "O código foi enviado para seu celular.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldNotDetectCodeRequestFromUser() {
        Message message = createMessage(
                "Me fala os números que chegaram no seu celular.",
                MessageSender.USUARIO
        );

        List<RuleMatch> matches =
                credentialRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }
}
