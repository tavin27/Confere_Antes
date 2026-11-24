
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

class UrgencyRuleTest {

    private UrgencyRule urgencyRule;

    @BeforeEach
    void setUp() {
        urgencyRule = new UrgencyRule();
    }

    private Message createMessage(String content, MessageSender sender) {
        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        return message;
    }

    @Test
    void shouldDetectImmediateUrgency() {
        Message message = createMessage(
                "Você precisa resolver isso imediatamente.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                urgencyRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
        assertEquals(
                RuleType.PRESSAO_URGENCIA,
                matches.get(0).ruleType()
        );
    }

    @Test
    void shouldDetectUrgencyWithTimeLimit() {
        Message message = createMessage(
                "Você tem apenas 10 minutos para confirmar.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                urgencyRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
    }

    @Test
    void shouldDetectUrgencyUsingToday() {
        Message message = createMessage(
                "Você precisa fazer isso ainda hoje.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                urgencyRule.evaluate(List.of(message));

        assertEquals(1, matches.size());
    }

    @Test
    void shouldNotDetectNeutralMessage() {
        Message message = createMessage(
                "Quando puder, entre em contato conosco.",
                MessageSender.CONTATO
        );

        List<RuleMatch> matches =
                urgencyRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldNotDetectUrgencyFromUser() {
        Message message = createMessage(
                "Preciso resolver isso agora!",
                MessageSender.USUARIO
        );

        List<RuleMatch> matches =
                urgencyRule.evaluate(List.of(message));

        assertTrue(matches.isEmpty());
    }
}

