package com.smartstock.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.ModeloProjeto;

public interface ModeloProjetoRepository extends JpaRepository<ModeloProjeto, Long> {

    List<ModeloProjeto> findAllByOrderByNomeAsc();

    Optional<ModeloProjeto> findFirstByNomeIgnoreCase(String nome);
}
