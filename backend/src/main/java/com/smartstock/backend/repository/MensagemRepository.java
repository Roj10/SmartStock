package com.smartstock.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.model.Mensagem;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    /** Mensagens trocadas entre duas contas, da mais recente para a mais antiga. */
    @Query("""
            select m from Mensagem m
            where (m.remetente.id = :a and m.destinatario.id = :b)
               or (m.remetente.id = :b and m.destinatario.id = :a)
            order by m.dataHora desc, m.id desc
            """)
    List<Mensagem> conversa(@Param("a") Long a, @Param("b") Long b, Pageable pageable);

    long countByDestinatarioIdAndLidaFalse(Long destinatarioId);

    Optional<Mensagem> findFirstByDestinatarioIdAndLidaFalseOrderByDataHoraDescIdDesc(Long destinatarioId);

    long countByDestinatarioIdAndRemetenteIdAndLidaFalse(Long destinatarioId, Long remetenteId);

    @Transactional
    @Modifying
    @Query("update Mensagem m set m.lida = true where m.destinatario.id = :destinatario and m.remetente.id = :remetente and m.lida = false")
    int marcarComoLidas(@Param("destinatario") Long destinatario, @Param("remetente") Long remetente);

    @Query("select m from Mensagem m where m.imagemArquivo is not null and (m.remetente.id = :usuario or m.destinatario.id = :usuario)")
    List<Mensagem> comImagemDoUsuario(@Param("usuario") Long usuario);

    @Transactional
    @Modifying
    @Query("delete from Mensagem m where m.remetente.id = :usuario or m.destinatario.id = :usuario")
    void excluirDoUsuario(@Param("usuario") Long usuario);
}
