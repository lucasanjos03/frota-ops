package br.edu.cesmac.frota_ops.motorista;

import java.time.LocalDate;
import java.util.List;

/**
 * Resumo analítico da tela de motoristas: indicadores, janela de antecedência da CNH,
 * distribuição por categoria e os documentos que exigem atenção.
 *
 * <p>{@code referencia} é a data usada em todos os cálculos da página — a mesma passada à
 * listagem filtrada, garantindo que cartões, painel e tabela contem a mesma história.</p>
 */
public record MotoristaPaginaDTO(
        int janelaCnhDias,
        LocalDate referencia,
        MotoristaIndicadoresDTO indicadores,
        List<MotoristaCategoriaDTO> porCategoria,
        List<MotoristaDocumentoDTO> documentosAtencao) {
}
