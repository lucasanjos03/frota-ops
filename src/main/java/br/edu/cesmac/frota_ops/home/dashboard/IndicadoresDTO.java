package br.edu.cesmac.frota_ops.home.dashboard;

/**
 * Indicadores numéricos do dashboard. Todos derivam de consultas agregadas no banco;
 * nenhum valor é estimado ou fixado em código.
 */
public record IndicadoresDTO(
        long totalVeiculos,
        long disponiveis,
        long emTransito,
        long emManutencao,
        long indisponiveis,
        long revisoesVencidas,
        long revisoesProximas,
        long revisoesEmDia,
        long cnhsVencidas,
        long cnhsVencendo) {
}
