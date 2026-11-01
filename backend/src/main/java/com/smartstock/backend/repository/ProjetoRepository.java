package com.smartstock.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.StatusProjeto;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de projetos (por etapa, para montar as abas Progresso e Calendário).
 */
public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    List<Projeto> findByStatusInOrderByDataCriacaoDesc(List<StatusProjeto> status);

    // Fila de produção: o projeto mais antigo aparece primeiro.
    List<Projeto> findByStatusInOrderByDataCriacaoAscIdAsc(List<StatusProjeto> status);
}
