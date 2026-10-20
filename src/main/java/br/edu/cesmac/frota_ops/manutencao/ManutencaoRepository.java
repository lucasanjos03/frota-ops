package br.edu.cesmac.frota_ops.manutencao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    List<Manutencao> findAllByOrderByDataDesc();

    List<Manutencao> findByVeiculoIdOrderByDataDesc(Long veiculoId);

}
