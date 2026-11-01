package com.smartstock.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartstock.backend.model.Movimentacao;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de movimentações (histórico geral, por item e as mais recentes para o painel).
 */
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findAllByOrderByDataHoraDesc();

    @Query("select m from Movimentacao m where m.pastilha.id = :pastilhaId order by m.dataHora desc")
    List<Movimentacao> findByPastilhaIdOrderByDataHoraDesc(@Param("pastilhaId") Long pastilhaId);

    @Query("select m from Movimentacao m where m.dataHora between :inicio and :fim order by m.dataHora desc")
    List<Movimentacao> findByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
