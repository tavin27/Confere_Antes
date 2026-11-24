
package com.confereantes.rules.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.confereantes.model.Message;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.rules.DetectionRule;
import com.confereantes.rules.RuleMatch;
import com.confereantes.rules.RuleType;
import com.confereantes.rules.TextNormalizer;

@Component
public class BlockingThreatRule implements DetectionRule {

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
            "\\b(conta|cartao|acesso|cadastro)\\b"
    );

    private static final Pattern BLOCKING_PATTERN = Pattern.compile(
            "\\b(bloquead\\w*|bloquear|bloqueio|"
                    + "suspens\\w*|cancelad\\w*|"
                    + "encerrad\\w*|desativad\\w*)\\b"
    );

    private static final Pattern NEGATION_PATTERN = Pattern.compile(
            "\\b(nao|nunca|jamais)\\s+"
                    + "(sera|vai|iremos|vamos|"
                    + "bloquear|suspender|cancelar)\\b"
    );

    @Override
    public List<RuleMatch> evaluate(List<Message> messages) {

        List<RuleMatch> matches = new ArrayList<>();

        for (Message message : messages) {

            if (message.getSender() != MessageSender.CONTATO) {
                continue;
            }

            String content = TextNormalizer.normalize(message.getContent());

            if (content.isBlank()) {
                continue;
            }

            boolean mentionsAccount =
                    ACCOUNT_PATTERN.matcher(content).find();

            boolean mentionsBlocking =
                    BLOCKING_PATTERN.matcher(content).find();

            boolean containsNegation =
                    NEGATION_PATTERN.matcher(content).find();

            if (mentionsAccount && mentionsBlocking && !containsNegation) {

                matches.add(new RuleMatch(
                        RuleType.AMEACA_BLOQUEIO_CONTA,
                        "O contato mencionou bloqueio, suspensão ou cancelamento de conta."
                ));
            }
        }

        return matches;
    }
}
