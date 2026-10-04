package com.smartstock.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Pedidos e respostas dos modelos (padrões) de projeto. */
public final class ModeloProjetoDtos {

    private ModeloProjetoDtos() {
    }

    public record MaterialModelo(@NotNull Long pastilhaId, @Positive int quantidade) {
    }

    public record ModeloRequest(@NotBlank @Size(max = 120) String nome, String descricao, List<String> checklist,
            @Valid List<MaterialModelo> materiais) {
    }

    public record ModeloResposta(Long id, String nome, String descricao, List<String> checklist,
            List<MaterialModelo> materiais, LocalDateTime dataCriacao) {
    }
}
