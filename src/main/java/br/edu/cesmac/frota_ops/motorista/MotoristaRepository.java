package br.edu.cesmac.frota_ops.motorista;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    /**
     * Projeção de contagem agrupada por status. Alimenta os cartões de ativos/inativos sem
     * materializar as linhas da tabela.
     */
    interface ContagemPorStatus {
        String getStatus();

        long getTotal();
    }

    /** Projeção de contagem agrupada por categoria de CNH — alimenta o gráfico. */
    interface ContagemPorCategoria {
        String getCategoria();

        long getTotal();
    }

    /**
     * Motoristas cuja CNH vence até a data limite (inclui as já vencidas). O serviço separa
     * vencidas de "a vencer" conforme a janela definida.
     */
    List<Motorista> findByValidadeCnhLessThanEqualOrderByValidadeCnhAsc(LocalDate limite);

    /** Usada pelos testes/verificações para conferir cartão a cartão contra o banco. */
    long countByStatus(String status);

    /** CNHs já vencidas na data de referência (validade estritamente anterior). */
    long countByValidadeCnhBefore(LocalDate referencia);

    /** CNHs dentro da janela [referencia, limite] — inclui o vencimento no próprio dia. */
    long countByValidadeCnhBetween(LocalDate referencia, LocalDate limite);

    /**
     * Motoristas sem nenhum veículo atribuído. A cardinalidade real é 1:N do lado do
     * motorista (vários veículos apontam para o mesmo motorista), portanto "sem veículo"
     * significa nenhuma linha em {@code veiculo} referenciando o motorista.
     */
    @Query("select count(m) from Motorista m "
            + "where not exists (select 1 from Veiculo v where v.motorista.id = m.id)")
    long contarSemVeiculo();

    /** Contagem de motoristas por status em um único SELECT agregado. */
    @Query("select m.status as status, count(m) as total from Motorista m group by m.status")
    List<ContagemPorStatus> contarAgrupadoPorStatus();

    /** Contagem de motoristas por categoria de CNH em um único SELECT agregado. */
    @Query("select m.categoriaCnh as categoria, count(m) as total from Motorista m "
            + "group by m.categoriaCnh order by m.categoriaCnh")
    List<ContagemPorCategoria> contarAgrupadoPorCategoria();

    /** Categorias de CNH existentes no cadastro — opções do filtro, sem tabela de referência. */
    @Query("select distinct m.categoriaCnh from Motorista m order by m.categoriaCnh")
    List<String> listarCategorias();

    /**
     * Listagem filtrada da tela de motoristas.
     *
     * <p>Todos os critérios são opcionais: {@code null} desativa o critério correspondente.</p>
     *
     * <ul>
     *   <li>{@code busca}: nome, CPF, número da CNH ou username do usuário vinculado.</li>
     *   <li>{@code status}: status do cadastro ("Ativo"/"Inativo").</li>
     *   <li>{@code categoria}: categoria da CNH exata.</li>
     *   <li>{@code cnh}: situação derivada de {@code validadeCnh} — "vencida"
     *       (validade anterior a {@code referencia}), "proxima" (entre {@code referencia} e
     *       {@code limiteCnh}) ou "valida" (posterior a {@code limiteCnh}).</li>
     *   <li>{@code vinculo}: "sem" (nenhum veículo atribuído) ou "com" (ao menos um).</li>
     * </ul>
     */
    @Query("""
            select m from Motorista m
            where (:busca is null
                   or lower(m.nome) like :busca
                   or lower(m.cpf) like :busca
                   or lower(m.numeroCnh) like :busca
                   or lower(m.usuario.username) like :busca)
              and (:status is null or m.status = :status)
              and (:categoria is null or m.categoriaCnh = :categoria)
              and (:cnh is null
                   or (:cnh = 'vencida' and m.validadeCnh < :referencia)
                   or (:cnh = 'proxima' and m.validadeCnh >= :referencia and m.validadeCnh <= :limiteCnh)
                   or (:cnh = 'valida' and m.validadeCnh > :limiteCnh))
              and (:vinculo is null
                   or (:vinculo = 'sem' and not exists (select 1 from Veiculo v where v.motorista.id = m.id))
                   or (:vinculo = 'com' and exists (select 1 from Veiculo v where v.motorista.id = m.id)))
            order by m.nome asc
            """)
    @EntityGraph(attributePaths = "usuario")
    List<Motorista> buscarComFiltros(@Param("busca") String busca,
                                     @Param("status") String status,
                                     @Param("categoria") String categoria,
                                     @Param("cnh") String cnh,
                                     @Param("vinculo") String vinculo,
                                     @Param("referencia") LocalDate referencia,
                                     @Param("limiteCnh") LocalDate limiteCnh);
}
