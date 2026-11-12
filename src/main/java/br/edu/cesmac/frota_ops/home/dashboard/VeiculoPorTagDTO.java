package br.edu.cesmac.frota_ops.home.dashboard;

/**
 * Contagem de veículos por tag. As tags se sobrepõem: um mesmo veículo pode ser contado em
 * mais de uma tag, portanto esta série não é um particionamento da frota.
 */
public record VeiculoPorTagDTO(String tag, long total) {
}
