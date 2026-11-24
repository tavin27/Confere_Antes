
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
public class CredentialRule implements DetectionRule {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "\\b(senha|password|credencial|"
                    + "senha\\s+de\\s+acesso)\\b"
    );

    private static final Pattern REQUEST_PATTERN = Pattern.compile(
            "\\b(inform\\w*|pass\\w*|envie|enviar|"
                    + "compartilh\\w*|confirm\\w*|"
                    + "fornec\\w*|diga|dizer|"
                    + "fal\\w*|encaminh\\w*|dig\\w*)\\b"
    );

    private static final Pattern CODE_PATTERN = Pattern.compile(
            "\\b(codigo|token|codigo\\s+de\\s+acesso|"
                    + "codigo\\s+de\\s+verificacao|"
                    + "codigo\\s+de\\s+autenticacao)\\b"
    );

    private static final Pattern CODE_REQUEST_PATTERN = Pattern.compile(
            "\\b(inform\\w*|pass\\w*|envie|enviar|"
                    + "compartilh\\w*|confirm\\w*|"
                    + "fornec\\w*|diga|dizer|"
                    + "fal[ae]|falar|encaminh\\w*|"
                    + "dig\\w*)\\b"
    );

    private static final Pattern NEGATED_REQUEST_PATTERN = Pattern.compile(
            "\\b(nao|nunca|jamais)\\s+"
                    + "(inform\\w*|pass\\w*|envie|enviar|"
                    + "compartilh\\w*|confirm\\w*|"
                    + "fornec\\w*|diga|dizer|"
                    + "fal\\w*|encaminh\\w*|dig\\w*)\\b"
    );

    private static final Pattern CONDITIONAL_NEGATION_PATTERN = Pattern.compile(
            "\\b(se|caso)\\s+(voce\\s+)?nao\\s+"
                    + "(inform\\w*|pass\\w*|envi\\w*|"
                    + "compartilh\\w*|confirm\\w*|"
                    + "fornec\\w*|diz\\w*|fal\\w*|"
                    + "encaminh\\w*|dig\\w*)\\b"
    );

    @Override
    public List<RuleMatch> evaluate(List<Message> messages) {

        List<RuleMatch> matches = new ArrayList<>();

        for (Message message : messages) {

            if (message.getSender() != MessageSender.CONTATO) {
                continue;
            }

            String content = TextNormalizer.normalize(message.getContent());

            if (content.isBlank() || isNegatedRequest(content)) {
                continue;
            }

            boolean mentionsPassword =
                    PASSWORD_PATTERN.matcher(content).find();

            boolean mentionsCode =
                    CODE_PATTERN.matcher(content).find();

            if (mentionsPassword && REQUEST_PATTERN.matcher(content).find()) {
                matches.add(new RuleMatch(
                        RuleType.SOLICITACAO_SENHA,
                        "O contato solicitou uma senha ou credencial de acesso."
                ));
            }

            if (mentionsCode && CODE_REQUEST_PATTERN.matcher(content).find()) {
                matches.add(new RuleMatch(
                        RuleType.SOLICITACAO_CODIGO_AUTENTICACAO,
                        "O contato solicitou um código ou token de autenticação."
                ));
            }
        }

        return matches;
    }

    private boolean isNegatedRequest(String content) {

        boolean containsNegatedRequest =
                NEGATED_REQUEST_PATTERN.matcher(content).find();

        boolean containsConditionalNegation =
                CONDITIONAL_NEGATION_PATTERN.matcher(content).find();

        return containsNegatedRequest && !containsConditionalNegation;
    }
}
