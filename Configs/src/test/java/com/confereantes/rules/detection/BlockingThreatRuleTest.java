

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

class BlockingThreatRuleTest {

    private BlockingThreatRule blockingThreatRule;

    @BeforeEach
    void setUp() {
        blockingThreatRule = new BlockingThreatRule();
    }

    private Message createMessage(String content, MessageSender sender) {
        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        return message;
    }

    @Test
    void shouldDetectAccountBlockingThreat() {
        Message message = createMessage(
                "Sua conta será bloqueada se não confirmar seus dados.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                blockingThreatRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
        assertEquals(
                RuleType.AMEACA_BLOQUEIO_CONTA,
                matches.get(0).ruleType()
        );
    }

    @Test
    void shouldDetectAccountSuspensionThreat() {
        Message message = createMessage(
                "Seu acesso será suspenso.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                blockingThreatRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
    }

    @Test
    void shouldDetectCardCancellationThreat() {
        Message message = createMessage(
                "Seu cartão será cancelado.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                blockingThreatRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
    }

    @Test
    void shouldNotDetectMessageWithoutBlockingThreat() {
        Message message = createMessage(
                "Sua conta está ativa e segura.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                blockingThreatRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldNotDetectMessageFromUser() {
        Message message = createMessage(
                "Sua conta será bloqueada.",
                MessageSender.USUARIO
        );

        List<RuleMatch> matches =
                blockingThreatRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldNotDetectExplicitNegation() {
        Message message = createMessage(
                "Não vamos bloquear sua conta.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                blockingThreatRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }
}

