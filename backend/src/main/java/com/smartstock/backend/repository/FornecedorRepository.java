package com.smartstock.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.Fornecedor;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de fornecedores. Os métodos básicos (salvar, buscar, listar, excluir) já vêm prontos do JpaRepository.
 */
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
}
