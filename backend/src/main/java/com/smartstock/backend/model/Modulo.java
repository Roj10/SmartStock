package com.smartstock.backend.model;

/**
 * <b>Em palavras simples:</b> As "abas" do sistema que podem ser liberadas para cada conta. Cada uma vira uma permissão PERM_...
 * conferida nas rotas da API (ex.: PROJETOS libera Progresso e Calendário).
 */
public enum Modulo {
    PASTILHAS,
    MOVIMENTACOES,
    FORNECEDORES,
    USUARIOS,
    PROJETOS,
    FINANCEIRO
}
