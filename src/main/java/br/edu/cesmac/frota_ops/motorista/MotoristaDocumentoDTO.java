package br.edu.cesmac.frota_ops.motorista;

import java.time.LocalDate;

/**
 * Documento (CNH) que exige atenção: vencido ou com vencimento dentro da janela de
 * antecedência. {@code diasRestantes} é negativo quando já venceu; zero indica vencimento
 * no próprio dia de referência (ainda válido, tratado como "A vencer").
 */
public record MotoristaDocumentoDTO(
        Long motoristaId,
        String nome,
        String numeroCnh,
        LocalDate validade,
        long diasRestantes,
        String situacao) {
}
