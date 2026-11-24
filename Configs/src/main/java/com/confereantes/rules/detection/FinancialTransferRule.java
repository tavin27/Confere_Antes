
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
public class FinancialTransferRule implements DetectionRule {

    
private static final Pattern FINANCIAL_PATTERN = Pattern.compile(
        "\\b(pix|ted|doc|dinheiro|transferencia|transferir|"
                + "deposito|depositar|pagamento|pagar|"
                + "enviar\\s+dinheiro|"
                + "fazer\\s+um\\s+pagamento)\\b"
);


    private static final Pattern REQUEST_PATTERN = Pattern.compile(
            "\\b(faca|realize|efetue|"
                    + "transfira|transfere|"
                    + "envie|enviar|"
                    + "deposite|depositar|"
                    + "pague|pagar|"
                    + "mande|mandar|"
                    + "precisa|necessario|"
                    + "solicitamos|solicito)\\b"
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

            boolean mentionsFinancialOperation =
                    FINANCIAL_PATTERN.matcher(content).find();

            boolean containsRequest =
                    REQUEST_PATTERN.matcher(content).find();

            if (mentionsFinancialOperation && containsRequest) {

                matches.add(new RuleMatch(
                        RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA,
                        "O contato solicitou uma operação ou transferência financeira."
                ));
            }
        }

        return matches;
    }
}
