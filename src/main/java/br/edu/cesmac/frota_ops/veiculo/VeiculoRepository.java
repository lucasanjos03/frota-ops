package br.edu.cesmac.frota_ops.veiculo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Veículos atribuídos a um motorista. A cardinalidade é 1:N (um motorista, N veículos). */
    List<Veiculo> findByMotoristaIdOrderByPlacaAsc(Long motoristaId);

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

    /** Veículos sem motorista atribuído (id_motorista nulo). */
    long countByMotoristaIsNull();

    /** Os cinco veículos de maior quilometragem atual (escopo global). */
    @EntityGraph(attributePaths = "motorista")
    List<Veiculo> findTop5ByOrderByQuilometragemDesc();

    /** Os cinco veículos de maior quilometragem atual, restritos a um motorista. */
    @EntityGraph(attributePaths = "motorista")
    List<Veiculo> findTop5ByMotoristaIdOrderByQuilometragemDesc(Long motoristaId);

    /**
     * Listagem paginada com todos os filtros da tela de veículos.
     *
     * <p>Todos os critérios são opcionais: passar {@code null} (ou {@code "sem"}/{@code "vencida"}
     * ausentes) desativa o critério correspondente. O {@code left join} explícito com
     * {@code motorista} é obrigatório para não descartar veículos sem motorista atribuído.</p>
     *
     * <ul>
     *   <li>{@code situacao} reproduz {@code VeiculoService.calcularSituacaoManutencao}:
     *       "Em manutenção" (status), "Atenção" (revisão até {@code limiteRevisao}) ou
     *       "Em dia" (sem revisão agendada ou além da janela).</li>
     *   <li>{@code revisao} = "vencida" (revisão anterior a hoje) ou "proxima"
     *       (revisão entre hoje e {@code limiteRevisao}), nas mesmas janelas do dashboard.</li>
     *   <li>{@code vinculo} = "sem" (sem motorista) ou "com" (com motorista).</li>
     * </ul>
     */
    @Query("""
            select v from Veiculo v
            left join v.motorista m
            where (:motoristaId is null or m.id = :motoristaId)
              and (:status is null or v.status = :status)
              and (:busca is null
                   or lower(v.placa) like :busca
                   or lower(v.modelo) like :busca
                   or lower(m.nome) like :busca)
              and (:situacao is null
                   or (:situacao = 'Em manutenção' and v.status = 'Em manutenção')
                   or (:situacao = 'Atenção' and v.status <> 'Em manutenção'
                       and v.proximaRevisao is not null and v.proximaRevisao <= :limiteRevisao)
                   or (:situacao = 'Em dia' and v.status <> 'Em manutenção'
                       and (v.proximaRevisao is null or v.proximaRevisao > :limiteRevisao)))
              and (:revisao is null
                   or (:revisao = 'vencida' and v.proximaRevisao is not null and v.proximaRevisao < :hoje)
                   or (:revisao = 'proxima' and v.proximaRevisao is not null
                       and v.proximaRevisao >= :hoje and v.proximaRevisao <= :limiteRevisao))
              and (:vinculo is null
                   or (:vinculo = 'sem' and m.id is null)
                   or (:vinculo = 'com' and m.id is not null))
            """)
    @EntityGraph(attributePaths = "motorista")
    Page<Veiculo> buscarComFiltros(@Param("motoristaId") Long motoristaId,
                                   @Param("busca") String busca,
                                   @Param("status") String status,
                                   @Param("situacao") String situacao,
                                   @Param("revisao") String revisao,
                                   @Param("vinculo") String vinculo,
                                   @Param("hoje") LocalDate hoje,
                                   @Param("limiteRevisao") LocalDate limiteRevisao,
                                   Pageable pageable);
}
