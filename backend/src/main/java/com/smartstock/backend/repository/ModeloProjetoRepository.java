package com.smartstock.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.ModeloProjeto;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de modelos de projeto (busca também pelo nome, para não duplicar).
 */
public interface ModeloProjetoRepository extends JpaRepository<ModeloProjeto, Long> {

    List<ModeloProjeto> findAllByOrderByNomeAsc();

    Optional<ModeloProjeto> findFirstByNomeIgnoreCase(String nome);
}
