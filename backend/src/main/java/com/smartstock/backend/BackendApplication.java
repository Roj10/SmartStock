package com.smartstock.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <b>Em palavras simples:</b> Ponto de partida do backend: é este arquivo que "liga" o sistema.
 *
 * Ao rodar, o Spring Boot procura todas as peças do projeto (controllers, services, repositórios,
 * configurações), conecta umas às outras, abre a conexão com o banco, aplica as migrations e começa a
 * atender pedidos na porta 8080. Não há regra de negócio aqui.
 */
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
