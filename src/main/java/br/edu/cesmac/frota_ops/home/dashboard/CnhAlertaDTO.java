package br.edu.cesmac.frota_ops.home.dashboard;

import java.time.LocalDate;

/**
 * Motorista com CNH vencida ou próxima do vencimento. {@code situacao} assume "Vencida" ou
 * "A vencer".
 */
public record CnhAlertaDTO(
        Long motoristaId,
        String nome,
        String numeroCnh,
        LocalDate validade,
        long diasRestantes,
        String situacao) {
}
