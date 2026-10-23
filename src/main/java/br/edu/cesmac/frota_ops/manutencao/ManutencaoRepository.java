package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;
import java.util.List;

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

    List<Manutencao> findAllByOrderByDataDesc();

    List<Manutencao> findByVeiculoIdOrderByDataDesc(Long veiculoId);

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
}
