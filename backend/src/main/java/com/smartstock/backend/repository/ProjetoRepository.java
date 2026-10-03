package com.smartstock.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.StatusProjeto;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    List<Projeto> findByStatusInOrderByDataCriacaoDesc(List<StatusProjeto> status);

    // Fila de produção: o projeto mais antigo aparece primeiro.
    List<Projeto> findByStatusInOrderByDataCriacaoAscIdAsc(List<StatusProjeto> status);
}
