package br.edu.cesmac.frota_ops.home.dashboard;

/**
 * Fatia da distribuição da frota por status. {@code cor} alimenta o gráfico de rosca e
 * {@code badgeClasse} reutiliza as classes de badge do design system.
 */
public record DistribuicaoStatusDTO(String status, long total, String cor, String badgeClasse) {
}
