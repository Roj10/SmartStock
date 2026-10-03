package com.smartstock.backend.model;

import java.util.List;

/**
 * Ordem de produção: as quatro primeiras etapas formam o quadro de Progresso e
 * seguem em sequência; PEDIDO_ENVIADO e ENTREGUE pertencem ao Calendário.
 */
public enum StatusProjeto {
    AGUARDANDO,
    EM_PRODUCAO,
    EM_ESTOQUE,
    PRONTO_ENTREGA,
    PEDIDO_ENVIADO,
    ENTREGUE;

    public static final List<StatusProjeto> ETAPAS_PROGRESSO =
            List.of(AGUARDANDO, EM_PRODUCAO, EM_ESTOQUE, PRONTO_ENTREGA);

    public static final List<StatusProjeto> ETAPAS_CALENDARIO = List.of(PEDIDO_ENVIADO, ENTREGUE);

    public boolean estaNoProgresso() {
        return ETAPAS_PROGRESSO.contains(this);
    }
}
