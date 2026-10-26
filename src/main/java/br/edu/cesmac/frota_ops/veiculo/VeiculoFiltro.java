package br.edu.cesmac.frota_ops.veiculo;

/**
 * Critérios de filtro da listagem de veículos. Todos os campos são opcionais: valor nulo
 * ou em branco significa "sem restrição".
 *
 * <ul>
 *   <li>{@code q}: busca textual em placa, modelo e nome do motorista.</li>
 *   <li>{@code status}: status operacional exato ("Disponível", "Em trânsito",
 *       "Em manutenção" ou "Indisponível").</li>
 *   <li>{@code situacao}: situação de manutenção derivada de status + próxima revisão
 *       ("Em manutenção", "Atenção" ou "Em dia"), igual à regra de
 *       {@code VeiculoService.calcularSituacaoManutencao}.</li>
 *   <li>{@code revisao}: recorte de revisão usado pelos indicadores ("vencida" ou
 *       "proxima") — mesmas janelas do dashboard.</li>
 *   <li>{@code vinculo}: vínculo de motorista ("sem" = veículos sem motorista atribuído).</li>
 * </ul>
 */
public record VeiculoFiltro(String q, String status, String situacao, String revisao, String vinculo) {

    /** Indica se há ao menos um critério ativo — usado para exibir os filtros aplicados. */
    public boolean temFiltro() {
        return !emBranco(q) || !emBranco(status) || !emBranco(situacao)
                || !emBranco(revisao) || !emBranco(vinculo);
    }

    private static boolean emBranco(String valor) {
        return valor == null || valor.isBlank();
    }
}
