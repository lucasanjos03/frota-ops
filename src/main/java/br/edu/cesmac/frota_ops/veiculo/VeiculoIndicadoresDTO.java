package br.edu.cesmac.frota_ops.veiculo;

/**
 * Indicadores da tela de veículos. Todos derivam de consultas agregadas no banco e
 * respeitam o escopo do perfil (frota completa para ADMIN/OPERADOR, apenas os veículos
 * do próprio cadastro para MOTORISTA). Não são afetados pelos filtros da tabela.
 */
public record VeiculoIndicadoresDTO(
        long total,
        long disponiveis,
        long emTransito,
        long emManutencao,
        long indisponiveis,
        long revisoesVencidas,
        long revisoesProximas,
        long semMotorista) {
}
