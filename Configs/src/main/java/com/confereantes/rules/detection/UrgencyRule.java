
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
public class UrgencyRule implements DetectionRule {

    
private static final Pattern URGENCY_PATTERN = Pattern.compile(
        "\\b(urgente|imediatamente|agora|"
                + "com\\s+urgencia|"
                + "prazo\\s+curto|"
                + "ultimas?\\s+horas?|"
                + "dentro\\s+de\\s+\\d+\\s+minutos?|"
                + "em\\s+\\d+\\s+minutos?|"
                + "tem\\s+(apenas\\s+)?\\d+\\s+minutos?|"
                + "rapidamente|"
                + "sem\\s+demora|"
                + "ainda\\s+hoje)\\b"
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

            if (URGENCY_PATTERN.matcher(content).find()) {

                matches.add(new RuleMatch(
                        RuleType.PRESSAO_URGENCIA,
                        "O contato utilizou expressões de urgência ou pressão temporal."
                ));
            }
        }

        return matches;
    }
}
