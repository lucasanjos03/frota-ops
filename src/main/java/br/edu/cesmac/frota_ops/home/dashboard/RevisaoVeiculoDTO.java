package br.edu.cesmac.frota_ops.home.dashboard;

import java.time.LocalDate;

/**
 * Linha da lista de próximas revisões. {@code diasRestantes} é negativo quando a revisão
 * já venceu; {@code situacao} assume "Vencida" ou "Próxima".
 */
public record RevisaoVeiculoDTO(
        Long veiculoId,
        String placa,
        String modelo,
        String motorista,
        LocalDate proximaRevisao,
        long diasRestantes,
        String situacao) {
}
