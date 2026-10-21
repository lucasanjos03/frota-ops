package br.edu.cesmac.frota_ops.veiculo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    @Query("""
        SELECT DISTINCT v FROM Veiculo v
        LEFT JOIN FETCH v.motorista m
        LEFT JOIN FETCH v.tags t
        WHERE (:termo IS NULL OR :termo = ''
               OR LOWER(v.placa) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(m.nome) LIKE LOWER(CONCAT('%', :termo, '%')))
          AND (:status IS NULL OR :status = '' OR LOWER(v.status) = LOWER(:status))
        ORDER BY v.id ASC
    """)
    List<Veiculo> filtrarVeiculosDaFrota(
            @Param("termo") String termo,
            @Param("status") String status);

    List<Veiculo> findByMotoristaId(Long motoristaId);

    List<Veiculo> findByProximaRevisaoIsNotNullOrderByProximaRevisaoAsc();

    long countByStatusIgnoreCase(String status);
}
