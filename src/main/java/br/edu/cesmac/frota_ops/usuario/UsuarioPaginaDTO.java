package br.edu.cesmac.frota_ops.usuario;

/**
 * Resumo de usuários ativos por perfil. Exibido apenas para ADMIN — é a única visão que
 * enxerga a base inteira de contas. Não transporta credenciais nem dados pessoais.
 */
public record UsuarioPaginaDTO(
        long total,
        long administradores,
        long operadores,
        long motoristas) {
}
