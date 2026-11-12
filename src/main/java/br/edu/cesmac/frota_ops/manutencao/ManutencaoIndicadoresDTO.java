package br.edu.cesmac.frota_ops.manutencao;

/**
 * Indicadores da tela de manutenções. Todos derivam de consultas agregadas no banco e
 * descrevem o escopo do perfil (frota completa para ADMIN/OPERADOR; apenas os veículos do
 * motorista para o perfil MOTORISTA). Não são afetados pelos filtros da tabela.
 *
 * <p>As duas séries são <strong>distintas e não somáveis</strong>:</p>
 * <ul>
 *   <li>{@code totalRegistros} e {@code registrosNoMes}: histórico — manutenções já
 *       realizadas, contadas por {@code data_manutencao}. O recorte do mês vai do primeiro
 *       dia do mês corrente até a data de referência.</li>
 *   <li>{@code revisoesAtrasadas} e {@code revisoesPrevistas}: previsão — derivadas de
 *       {@code veiculo.proxima_revisao}, não do histórico de manutenções. Atrasadas são as
 *       com data prevista anterior à referência; previstas são as que vencem entre a
 *       referência e a referência + 15 dias.</li>
 * </ul>
 */
public record ManutencaoIndicadoresDTO(
        long totalRegistros,
        long registrosNoMes,
        long revisoesAtrasadas,
        long revisoesPrevistas) {
}
