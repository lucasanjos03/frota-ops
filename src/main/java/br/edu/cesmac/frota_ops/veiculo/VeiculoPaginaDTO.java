package br.edu.cesmac.frota_ops.veiculo;

import java.util.List;

/**
 * Resumo analítico da tela de veículos: indicadores, recorte de escopo e o ranking de
 * quilometragem. Carrega apenas dados agregados; a tabela paginada trafega separadamente.
 */
public record VeiculoPaginaDTO(
        String escopoDescricao,
        boolean escopoRestrito,
        int janelaRevisaoDias,
        VeiculoIndicadoresDTO indicadores,
        List<VeiculoQuilometragemDTO> maioresQuilometragens) {
}
