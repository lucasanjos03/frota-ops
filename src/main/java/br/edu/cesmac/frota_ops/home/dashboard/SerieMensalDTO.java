package br.edu.cesmac.frota_ops.home.dashboard;

/**
 * Ponto da série mensal de manutenções. {@code competencia} está no formato YYYY-MM e
 * {@code rotulo} no formato legível (ex.: Out/26).
 */
public record SerieMensalDTO(String competencia, String rotulo, long total) {
}
