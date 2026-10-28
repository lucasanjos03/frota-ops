package br.edu.cesmac.frota_ops.motorista;

/**
 * Indicadores da tela de motoristas. Todos derivam de consultas agregadas no banco e
 * descrevem a equipe completa (a listagem de motoristas não é recortada por perfil).
 * Não são afetados pelos filtros da tabela.
 *
 * <p>{@code cnhVencidas} conta validades estritamente anteriores à data de referência;
 * {@code cnhProximas} conta a janela que <em>inclui</em> o vencimento no próprio dia.</p>
 */
public record MotoristaIndicadoresDTO(
        long total,
        long ativos,
        long inativos,
        long cnhVencidas,
        long cnhProximas,
        long semVeiculo) {
}
