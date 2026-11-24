package com.confereantes.rules;

public enum RuleType {

    SOLICITACAO_SENHA(true),

    SOLICITACAO_CODIGO_AUTENTICACAO(true),

    ORIENTACAO_DIGITAR_CREDENCIAL(false),

    SOLICITACAO_TRANSFERENCIA_FINANCEIRA(false),

    AMEACA_BLOQUEIO_CONTA(false),

    PRESSAO_URGENCIA(false);

    private final boolean immediateAlert;

    RuleType(boolean immediateAlert) {
        this.immediateAlert = immediateAlert;
    }

    public boolean triggersImmediateAlert() {
        return immediateAlert;
    }
}