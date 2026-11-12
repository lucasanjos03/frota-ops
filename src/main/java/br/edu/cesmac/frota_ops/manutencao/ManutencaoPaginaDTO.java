package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;
import java.util.List;

import br.edu.cesmac.frota_ops.home.dashboard.RevisaoVeiculoDTO;

/**
 * Resumo analítico da tela de manutenções: indicadores do histórico, série mensal, ranking
 * de veículos e o painel de revisões. Carrega apenas dados agregados; o histórico filtrado
 * trafega separadamente.
 *
 * <p>{@code referencia} é a data usada em todos os cálculos — a mesma passada ao histórico
 * filtrado, garantindo que cartões, gráfico, painel e tabela contem a mesma história.
 * {@code inicioMes} delimita o recorte de "realizadas no mês".</p>
 *
 * <p>Cada mês de {@code manutencoesPorMes} carrega o próprio intervalo
 * ({@code inicio}/{@code fim}), o que permite ligar cada ponto do gráfico exatamente aos
 * registros que o compõem.</p>
 *
 * <p>As duas séries do painel de revisões vêm do cadastro de veículos
 * ({@code proxima_revisao}) e representam <strong>previsão</strong>: são mantidas em listas
 * separadas de propósito, para que registros históricos e datas futuras nunca sejam somados
 * no mesmo indicador.</p>
 *
 * <p>Limitações conhecidas do modelo, que a tela não tenta contornar:</p>
 * <ul>
 *   <li>Não existem status de ordem de serviço (aberta/concluída/cancelada), custo, nem datas
 *       de abertura e conclusão — logo não há tempo médio de reparo.</li>
 *   <li>Não existe categorização do serviço (preventiva/corretiva): um registro é apenas uma
 *       manutenção realizada, sem juízo sobre falha.</li>
 *   <li>A quilometragem de cada registro é o hodômetro no momento do serviço; somá-la não
 *       representa distância percorrida.</li>
 * </ul>
 */
public record ManutencaoPaginaDTO(
        String escopoDescricao,
        boolean escopoRestrito,
        LocalDate referencia,
        LocalDate inicioMes,
        int mesesGrafico,
        int janelaRevisoesDias,
        ManutencaoIndicadoresDTO indicadores,
        List<ManutencaoMesDTO> manutencoesPorMes,
        List<ManutencaoRankingVeiculoDTO> rankingVeiculos,
        List<RevisaoVeiculoDTO> revisoesAtrasadas,
        List<RevisaoVeiculoDTO> revisoesPrevistas) {

    /** O gráfico mensal só é montado quando há ao menos um mês com registros. */
    public boolean exibirGraficoMensal() {
        return manutencoesPorMes.stream().anyMatch(mes -> mes.total() > 0);
    }
}
