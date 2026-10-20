package com.smartstock.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// os testes usam o banco em memória (perfil h2), sem precisar da senha do Supabase
@SpringBootTest
@ActiveProfiles("h2")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
