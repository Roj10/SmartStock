package com.smartstock.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.EventoSeguranca;

public interface EventoSegurancaRepository extends JpaRepository<EventoSeguranca, Long> {

    List<EventoSeguranca> findTop200ByOrderByDataHoraDescIdDesc();
}
