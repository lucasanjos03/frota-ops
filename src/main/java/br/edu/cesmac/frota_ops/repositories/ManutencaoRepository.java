package br.edu.cesmac.frota_ops.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.cesmac.frota_ops.entities.Manutencao;

@Repository
public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    List<Manutencao> findByVeiculoIdOrderByDataDesc(Long veiculoId);
}
