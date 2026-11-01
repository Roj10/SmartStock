package com.smartstock.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.ProjetoAnexo;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de arquivos anexados aos projetos.
 */
public interface ProjetoAnexoRepository extends JpaRepository<ProjetoAnexo, Long> {

    Optional<ProjetoAnexo> findByIdAndProjetoId(Long id, Long projetoId);
}
