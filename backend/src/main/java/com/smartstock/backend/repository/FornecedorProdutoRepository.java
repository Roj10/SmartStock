package com.smartstock.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.FornecedorProduto;

public interface FornecedorProdutoRepository extends JpaRepository<FornecedorProduto, Long> {

    List<FornecedorProduto> findByFornecedorIdOrderByPastilhaCodigoAsc(Long fornecedorId);

    List<FornecedorProduto> findByPastilhaId(Long pastilhaId);

    void deleteByFornecedorId(Long fornecedorId);

    void deleteByPastilhaId(Long pastilhaId);
}
