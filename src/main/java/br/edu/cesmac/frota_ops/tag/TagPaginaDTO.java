package br.edu.cesmac.frota_ops.tag;

import java.util.Map;

/**
 * Resumo da tela de tags: totalizadores compactos e a contagem de veículos por tag.
 *
 * <p>A contagem respeita o escopo do perfil: ADMIN e OPERADOR enxergam toda a frota;
 * MOTORISTA enxerga apenas os veículos vinculados ao próprio cadastro. Tags se sobrepõem
 * por natureza — um veículo pode pertencer a várias —, então a soma da série pode ser
 * maior que o total da frota.</p>
 *
 * @param veiculosPorTag contagem indexada pelo nome da tag (tags fora do escopo ficam de fora)
 */
public record TagPaginaDTO(
        long totalTags,
        long tagsEmUso,
        long tagsSemVeiculos,
        boolean escopoRestrito,
        Map<String, Long> veiculosPorTag) {
}
