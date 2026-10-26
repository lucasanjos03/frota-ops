package br.edu.cesmac.frota_ops.veiculo;

/**
 * Veículo posicionado no ranking de quilometragem. Usado pelo gráfico horizontal dos
 * cinco maiores valores de hodômetro. O modelo guarda apenas a quilometragem atual —
 * não existe histórico periódico, portanto este dado não representa km rodados no mês.
 */
public record VeiculoQuilometragemDTO(Long id, String placa, String modelo, String motorista,
                                      long quilometragem) {
}
