package com.smartstock.backend.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    public DashboardResponse gerar(Authentication authentication) {
        boolean podeVerPastilhas = possui(authentication, "PERM_PASTILHAS");
        boolean podeVerFornecedores = possui(authentication, "PERM_FORNECEDORES");
        boolean podeVerMovimentacoes = possui(authentication, "PERM_MOVIMENTACOES");

        Long totalPastilhas = null;
        Long itensAbaixoDoMinimo = null;
        List<Pastilha> alertas = null;
        if (podeVerPastilhas) {
            alertas = pastilhaRepository.findAlertasEstoqueMinimo();
            totalPastilhas = pastilhaRepository.count();
            itensAbaixoDoMinimo = (long) alertas.size();
        }

        Long totalFornecedores = podeVerFornecedores ? fornecedorRepository.count() : null;

        List<Movimentacao> ultimas = null;
        if (podeVerMovimentacoes) {
            ultimas = movimentacaoRepository
                    .findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataHora")))
                    .getContent();
        }

        return new DashboardResponse(podeVerPastilhas, podeVerFornecedores, podeVerMovimentacoes,
                totalPastilhas, totalFornecedores, itensAbaixoDoMinimo, alertas, ultimas);
    }

    private boolean possui(Authentication authentication, String autoridade) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority(autoridade));
    }
}
