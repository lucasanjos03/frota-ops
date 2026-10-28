package br.edu.cesmac.frota_ops.manutencao;

/**
 * Veículo posicionado no ranking de manutenções. {@code total} é a quantidade de
 * <strong>registros</strong> de manutenção do veículo no escopo do perfil — não representa
 * número de falhas, gravidade ou custo, dados que o modelo não possui.
 */
public record ManutencaoRankingVeiculoDTO(
        Long veiculoId,
        String placa,
        String modelo,
        String motorista,
        long total) {
}
