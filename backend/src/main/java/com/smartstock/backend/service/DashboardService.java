package com.smartstock.backend.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.smartstock.backend.dto.DashboardResponse;
import com.smartstock.backend.model.Movimentacao;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.repository.FornecedorRepository;
import com.smartstock.backend.repository.MovimentacaoRepository;
import com.smartstock.backend.repository.PastilhaRepository;

@Service
public class DashboardService {

    private final PastilhaRepository pastilhaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public DashboardService(PastilhaRepository pastilhaRepository, FornecedorRepository fornecedorRepository,
            MovimentacaoRepository movimentacaoRepository) {
        this.pastilhaRepository = pastilhaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public DashboardResponse gerar() {
        List<Pastilha> alertas = pastilhaRepository.findAlertasEstoqueMinimo();
        long totalPastilhas = pastilhaRepository.count();
        long totalFornecedores = fornecedorRepository.count();

        List<Movimentacao> ultimas = movimentacaoRepository
                .findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataHora")))
                .getContent();

        return new DashboardResponse(totalPastilhas, totalFornecedores, alertas.size(), alertas, ultimas);
    }
}
