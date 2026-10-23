package br.edu.cesmac.frota_ops.veiculo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    /**
     * Projeção de contagem agrupada por status. Alimenta o card de total e o gráfico de rosca
     * sem materializar as linhas da tabela.
     */
    interface ContagemPorStatus {
        String getStatus();

        long getTotal();
    }

    /** Usada pelos testes/verificações para conferir card a card contra o banco. */
    long countByStatus(String status);

    /** Contagem de veículos por status em um único SELECT agregado (escopo global). */
    @Query("select v.status as status, count(v) as total from Veiculo v group by v.status")
    List<ContagemPorStatus> contarAgrupadoPorStatus();

    /** Mesmo agregado, restrito aos veículos de um motorista (escopo do perfil MOTORISTA). */
    @Query("select v.status as status, count(v) as total from Veiculo v "
            + "where v.motorista.id = :motoristaId group by v.status")
    List<ContagemPorStatus> contarAgrupadoPorStatusDoMotorista(@Param("motoristaId") Long motoristaId);

    long countByMotoristaId(Long motoristaId);

    /** Revisões vencidas: proxima_revisao anterior a hoje. */
    long countByProximaRevisaoBefore(LocalDate data);

    /** Revisões na janela [inicio, fim] — usada para o indicador de revisões próximas. */
    long countByProximaRevisaoBetween(LocalDate inicio, LocalDate fim);

    long countByMotoristaIdAndProximaRevisaoBefore(Long motoristaId, LocalDate data);

    long countByMotoristaIdAndProximaRevisaoBetween(Long motoristaId, LocalDate inicio, LocalDate fim);

    /** Veículos com revisão agendada, ordenados da mais crítica para a menos crítica. */
    @EntityGraph(attributePaths = "motorista")
    List<Veiculo> findByProximaRevisaoIsNotNullOrderByProximaRevisaoAsc();

    @EntityGraph(attributePaths = "motorista")
    List<Veiculo> findByMotoristaIdAndProximaRevisaoIsNotNullOrderByProximaRevisaoAsc(Long motoristaId);
}
