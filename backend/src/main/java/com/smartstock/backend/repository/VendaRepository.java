package com.smartstock.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.Venda;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de vendas e planos.
 */
public interface VendaRepository extends JpaRepository<Venda, Long> {

    List<Venda> findAllByOrderByDataCriacaoDescIdDesc();

    List<Venda> findByProjetoId(Long projetoId);
}
