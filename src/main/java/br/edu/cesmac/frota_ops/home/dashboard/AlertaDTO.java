package br.edu.cesmac.frota_ops.home.dashboard;

/**
 * Alerta operacional priorizado. {@code nivel} assume ALTA, MEDIA ou INFO e define a ordem
 * de exibição; {@code url} sempre aponta para uma rota existente do sistema.
 */
public record AlertaDTO(String nivel, String titulo, String descricao, String url, String rotuloAcao) {
}
