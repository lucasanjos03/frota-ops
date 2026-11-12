package br.edu.cesmac.frota_ops.motorista;

/**
 * Contagem de motoristas por categoria de CNH. Não existe tabela de referência de
 * categorias no modelo: o agrupamento reflete apenas o que está cadastrado.
 */
public record MotoristaCategoriaDTO(String categoria, long total) {
}
