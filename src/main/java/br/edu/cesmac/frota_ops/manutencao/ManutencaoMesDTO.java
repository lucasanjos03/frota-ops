package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;

/**
 * Mês da série de manutenções realizadas, com o intervalo exato que ele representa.
 *
 * <p>Carregar {@code inicio}/{@code fim} junto do total permite que cada mês do gráfico linke
 * para <em>exatamente</em> os registros que compõem aquele ponto da série — sem recalcular o
 * intervalo na view e sem risco de divergir da contagem.</p>
 *
 * <p>{@code competencia} é o mês no formato {@code YYYY-MM} (mesmo contrato de
 * {@code SerieMensalDTO} do dashboard) e {@code rotulo} é a forma abreviada exibida no eixo.</p>
 */
public record ManutencaoMesDTO(
        String competencia,
        String rotulo,
        long total,
        LocalDate inicio,
        LocalDate fim) {
}
