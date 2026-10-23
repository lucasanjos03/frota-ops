package br.edu.cesmac.frota_ops.home.dashboard;

import java.util.List;

/**
 * Agregado completo exibido no dashboard operacional. Reúne apenas dados já calculados
 * pelo {@link DashboardService}; nenhuma entidade JPA trafega para a view ou para o JavaScript.
 */
public record DashboardDTO(
        String escopoDescricao,
        boolean escopoRestrito,
        String janelaRevisoes,
        String janelaCnh,
        String janelaManutencoes,
        IndicadoresDTO indicadores,
        List<DistribuicaoStatusDTO> distribuicaoStatus,
        List<SerieMensalDTO> manutencoesPorMes,
        boolean exibirGraficoManutencoes,
        List<VeiculoPorTagDTO> veiculosPorTag,
        boolean exibirGraficoTags,
        List<AlertaDTO> alertas,
        List<RevisaoVeiculoDTO> proximasRevisoes,
        List<CnhAlertaDTO> cnhsEmAlerta) {
}
