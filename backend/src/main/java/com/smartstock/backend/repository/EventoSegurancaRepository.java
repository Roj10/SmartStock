package com.smartstock.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.EventoSeguranca;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de eventos de segurança.
 *
 * Um "repository" é quem conversa com o banco. Basta declarar o nome do método (ex.:
 * findTop200ByOrderByDataHoraDescIdDesc = "os 200 mais recentes") e o Spring Data monta a consulta SQL sozinho.
 */
public interface EventoSegurancaRepository extends JpaRepository<EventoSeguranca, Long> {

    List<EventoSeguranca> findTop200ByOrderByDataHoraDescIdDesc();
}
