
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

class FinancialTransferRuleTest {

    private FinancialTransferRule financialTransferRule;

    @BeforeEach
    void setUp() {
        financialTransferRule = new FinancialTransferRule();
    }

    private Message createMessage(String content, MessageSender sender) {
        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        return message;
    }

    @Test
    void shouldDetectPixTransferRequest() {
        Message message = createMessage(
                "Faça uma transferência via PIX para regularizar sua conta.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                financialTransferRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
        assertEquals(
                RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA,
                matches.get(0).ruleType()
        );
    }

    @Test
    void shouldDetectMoneyTransferRequest() {
        Message message = createMessage(
                "Envie dinheiro para esta conta imediatamente.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                financialTransferRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
    }

    @Test
    void shouldDetectPaymentRequest() {
        Message message = createMessage(
                "Realize o pagamento para liberar seu acesso.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                financialTransferRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
    }

    @Test
    void shouldNotDetectNeutralFinancialInformation() {
        Message message = createMessage(
                "O PIX é um meio de pagamento instantâneo.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                financialTransferRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldNotDetectFinancialRequestFromUser() {
        Message message = createMessage(
                "Faça uma transferência via PIX.",
                MessageSender.USUARIO
        );

        List<RuleMatch> matches =
                financialTransferRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }
}
