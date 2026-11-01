package com.smartstock.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smartstock.backend.model.Pastilha;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de itens de estoque (inclui a busca dos itens em alerta e a checagem de código repetido).
 */
public interface PastilhaRepository extends JpaRepository<Pastilha, Long> {
    boolean existsByCodigo(String codigo);

    @Query("select p from Pastilha p where p.quantidadeAtual <= p.estoqueMinimo")
    List<Pastilha> findAlertasEstoqueMinimo();
}
