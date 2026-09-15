package com.smartstock.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.Fornecedor;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
}
