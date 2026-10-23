package br.edu.cesmac.frota_ops.motorista;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    /**
     * Motoristas cuja CNH vence até a data limite (inclui as já vencidas). O serviço separa
     * vencidas de "a vencer" conforme a janela definida.
     */
    List<Motorista> findByValidadeCnhLessThanEqualOrderByValidadeCnhAsc(LocalDate limite);
}
