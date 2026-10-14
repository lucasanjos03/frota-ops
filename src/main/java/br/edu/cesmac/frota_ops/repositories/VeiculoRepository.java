package br.edu.cesmac.frota_ops.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.cesmac.frota_ops.entities.Veiculo;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    @Query("SELECT v FROM Veiculo v WHERE " +
           "(:q IS NULL OR :q = '' OR LOWER(v.placa) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR LOWER(v.status) = LOWER(:status))")
    List<Veiculo> filtrar(@Param("q") String q, @Param("status") String status);

    List<Veiculo> findByProximaRevisaoIsNotNullOrderByProximaRevisaoAsc();

    long countByStatusIgnoreCase(String status);
}
