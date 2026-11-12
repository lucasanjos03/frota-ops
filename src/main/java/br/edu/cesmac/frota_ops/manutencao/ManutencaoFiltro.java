package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;

/**
 * Critérios de filtro do histórico de manutenções. Todos os campos são opcionais: valor nulo
 * ou em branco significa "sem restrição".
 *
 * <ul>
 *   <li>{@code veiculoId}: restringe o histórico a um veículo.</li>
 *   <li>{@code servico}: busca textual em {@code servico_realizado} (trecho do texto).</li>
 *   <li>{@code de}/{@code ate}: intervalo sobre {@code data_manutencao}, ambos inclusivos.</li>
 * </ul>
 *
 * <p>Não há filtro por status nem por tipo de serviço: o modelo de dados não possui esses
 * campos — o histórico registra apenas manutenções já realizadas.</p>
 */
public record ManutencaoFiltro(Long veiculoId, String servico, LocalDate de, LocalDate ate) {

    /** Indica se há ao menos um critério ativo — usado para exibir os filtros aplicados. */
    public boolean temFiltro() {
        return veiculoId != null || !emBranco(servico) || de != null || ate != null;
    }

    private static boolean emBranco(String valor) {
        return valor == null || valor.isBlank();
    }
}
