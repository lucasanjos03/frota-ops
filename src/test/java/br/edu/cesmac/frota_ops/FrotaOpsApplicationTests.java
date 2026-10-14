package br.edu.cesmac.frota_ops;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import br.edu.cesmac.frota_ops.entities.Veiculo;
import br.edu.cesmac.frota_ops.services.VeiculoService;

@SpringBootTest
class FrotaOpsApplicationTests {

	@Autowired
	private VeiculoService veiculoService;

	@Test
	void contextLoads() {
		assertTrue(veiculoService.contarTotal() >= 0);
		List<Veiculo> todos = veiculoService.listarTodos();
		assertFalse(todos.isEmpty(), "Deveria carregar veículos do banco de dados");
	}

}
