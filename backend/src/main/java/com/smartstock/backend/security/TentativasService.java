package com.smartstock.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.smartstock.backend.exception.MuitasTentativasException;

/**
 * Limita tentativas erradas seguidas (contra quem tenta adivinhar senhas ou códigos): depois de
 * {@value #LIMITE} erros na mesma chave, ela fica bloqueada por 15 minutos. Um acerto zera a contagem.
 * A contagem fica na memória do servidor (zera se o backend reiniciar).
 */
@Service
public class TentativasService {

    public static final int LIMITE = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private record Estado(int falhas, Instant bloqueadoAte) {
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    /** Lança erro (429) se a chave estiver bloqueada. */
    public void verificar(String chave) {
        Estado e = estados.get(chave);
        if (e != null && e.bloqueadoAte() != null) {
            if (Instant.now().isBefore(e.bloqueadoAte())) {
                long min = Math.max(1, Duration.between(Instant.now(), e.bloqueadoAte()).toMinutes() + 1);
                throw new MuitasTentativasException("Muitas tentativas erradas. Tente de novo em " + min
                        + (min == 1 ? " minuto." : " minutos."));
            }
            estados.remove(chave);
        }
    }

    /** Registra um erro; devolve true se esse erro causou o bloqueio. */
    public boolean falhou(String chave) {
        Estado novo = estados.merge(chave, new Estado(1, null), (a, b) -> {
            int f = a.falhas() + 1;
            return new Estado(f, f >= LIMITE ? Instant.now().plus(BLOQUEIO) : null);
        });
        return novo.bloqueadoAte() != null;
    }

    public void acertou(String chave) {
        estados.remove(chave);
    }
}
