package br.edu.cesmac.frota_ops.motorista;

/**
 * Critérios de filtro da listagem de motoristas. Todos os campos são opcionais: valor nulo
 * ou em branco significa "sem restrição".
 *
 * <ul>
 *   <li>{@code q}: busca textual em nome, CPF, número da CNH e username vinculado.</li>
 *   <li>{@code status}: status do cadastro ("Ativo"/"Inativo").</li>
 *   <li>{@code categoria}: categoria da CNH exata ("B", "C", "D", "E", ...).</li>
 *   <li>{@code cnh}: situação da CNH ("vencida", "proxima" ou "valida") derivada de
 *       {@code validadeCnh} e da data de referência.</li>
 *   <li>{@code vinculo}: "sem" (nenhum veículo atribuído) ou "com" (ao menos um).</li>
 * </ul>
 */
public record MotoristaFiltro(String q, String status, String categoria, String cnh, String vinculo) {

    /** Indica se há ao menos um critério ativo — usado para exibir os filtros aplicados. */
    public boolean temFiltro() {
        return !emBranco(q) || !emBranco(status) || !emBranco(categoria)
                || !emBranco(cnh) || !emBranco(vinculo);
    }

    private static boolean emBranco(String valor) {
        return valor == null || valor.isBlank();
    }
}
