package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    /**
     * Projeção de contagem mensal. A competência é o mês de {@code data_manutencao}
     * (a data real da manutenção), não a data de inserção do registro.
     */
    interface ContagemMensal {
        String getMes();

        long getTotal();
    }

    /**
     * Projeção do ranking de veículos por volume de registros de manutenção.
     *
     * <p>Conta <strong>registros</strong> de manutenção por veículo — não "falhas" e não
     * gravidade. Um veículo com muitos registros pode simplesmente ter recebido mais
     * manutenções preventivas.</p>
     */
    interface ContagemPorVeiculo {
        Long getVeiculoId();

        String getPlaca();

        String getModelo();

        String getMotorista();

        long getTotal();
    }

    List<Manutencao> findAllByOrderByDataDesc();

    List<Manutencao> findByVeiculoIdOrderByDataDesc(Long veiculoId);

    /**
     * Total de registros visível ao usuário: {@code motoristaId} nulo significa frota
     * completa (ADMIN/OPERADOR); preenchido, restringe aos veículos do motorista.
     */
    @Query("select count(m) from Manutencao m "
            + "where (:motoristaId is null or m.veiculo.motorista.id = :motoristaId)")
    long contarEscopado(@Param("motoristaId") Long motoristaId);

    /** Registros com {@code data_manutencao} dentro de {@code [inicio, fim]}, no escopo do perfil. */
    @Query("select count(m) from Manutencao m "
            + "where m.data between :inicio and :fim "
            + "and (:motoristaId is null or m.veiculo.motorista.id = :motoristaId)")
    long contarNoIntervalo(@Param("inicio") LocalDate inicio,
                           @Param("fim") LocalDate fim,
                           @Param("motoristaId") Long motoristaId);

    /** Manutenções por mês (escopo global), agregadas no banco por data_manutencao. */
    @Query(value = "select to_char(date_trunc('month', m.data_manutencao), 'YYYY-MM') as mes, "
            + "count(*) as total "
            + "from manutencao m "
            + "where m.data_manutencao between :inicio and :fim "
            + "group by date_trunc('month', m.data_manutencao) "
            + "order by date_trunc('month', m.data_manutencao)", nativeQuery = true)
    List<ContagemMensal> contarPorMes(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** Manutenções por mês dos veículos de um motorista (escopo do perfil MOTORISTA). */
    @Query(value = "select to_char(date_trunc('month', m.data_manutencao), 'YYYY-MM') as mes, "
            + "count(*) as total "
            + "from manutencao m "
            + "join veiculo v on v.id = m.veiculo_id "
            + "where m.data_manutencao between :inicio and :fim and v.id_motorista = :motoristaId "
            + "group by date_trunc('month', m.data_manutencao) "
            + "order by date_trunc('month', m.data_manutencao)", nativeQuery = true)
    List<ContagemMensal> contarPorMesDoMotorista(@Param("inicio") LocalDate inicio,
                                                 @Param("fim") LocalDate fim,
                                                 @Param("motoristaId") Long motoristaId);

    /**
     * Ranking de veículos por número de registros de manutenção, do maior para o menor.
     * Empate é desfeito pela placa para manter a ordenação determinística. O limite é
     * aplicado pelo {@code Pageable} (o {@code order by} já vem na consulta).
     *
     * <p>O {@code left join} explícito com {@code motorista} é obrigatório: um veículo sem
     * motorista atribuído continua tendo registros de manutenção e não pode sumir do ranking
     * (o {@code join} implícito de {@code v.motorista.nome} o descartaria).</p>
     */
    @Query("select v.id as veiculoId, v.placa as placa, v.modelo as modelo, "
            + "mtor.nome as motorista, count(m) as total "
            + "from Manutencao m "
            + "join m.veiculo v "
            + "left join v.motorista mtor "
            + "where (:motoristaId is null or mtor.id = :motoristaId) "
            + "group by v.id, v.placa, v.modelo, mtor.nome "
            + "order by count(m) desc, v.placa asc")
    List<ContagemPorVeiculo> contarAgrupadoPorVeiculo(@Param("motoristaId") Long motoristaId,
                                                      Pageable pageable);

    /**
     * Histórico de manutenções com os filtros da tela.
     *
     * <p>Todos os critérios são opcionais: {@code null} desativa o critério correspondente.
     * A ordenação é cronológica decrescente (a mais recente primeiro); o {@code id} entra
     * como critério de desempate para registros da mesma data.</p>
     *
     * <ul>
     *   <li>{@code veiculoId}: restringe a um veículo específico.</li>
     *   <li>{@code servico}: busca textual em {@code servico_realizado} — casa trecho do
     *       texto, não substitui uma taxonomia de serviços (que não existe no modelo).</li>
     *   <li>{@code de}/{@code ate}: intervalo sobre {@code data_manutencao}, ambos inclusivos.</li>
     * </ul>
     *
     * <p>O intervalo usa {@code m.data >= coalesce(:de, m.data)} em vez de
     * {@code (:de is null or m.data >= :de)}: a comparação direta fornece o tipo do parâmetro
     * ao banco. Sem esse contexto, o PostgreSQL não consegue inferir o tipo do parâmetro
     * nulo e recusa a consulta.</p>
     */
    @Query("""
            select m from Manutencao m
            join fetch m.veiculo v
            left join fetch v.motorista
            where (:motoristaId is null or v.motorista.id = :motoristaId)
              and (:veiculoId is null or v.id = :veiculoId)
              and (:servico is null or lower(m.servicoRealizado) like :servico)
              and m.data >= coalesce(:de, m.data)
              and m.data <= coalesce(:ate, m.data)
            order by m.data desc, m.id desc
            """)
    List<Manutencao> buscarComFiltros(@Param("motoristaId") Long motoristaId,
                                      @Param("veiculoId") Long veiculoId,
                                      @Param("servico") String servico,
                                      @Param("de") LocalDate de,
                                      @Param("ate") LocalDate ate);
}
