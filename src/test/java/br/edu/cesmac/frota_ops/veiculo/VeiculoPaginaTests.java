package br.edu.cesmac.frota_ops.veiculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.cesmac.frota_ops.manutencao.ManutencaoRepository;
import br.edu.cesmac.frota_ops.motorista.Motorista;
import br.edu.cesmac.frota_ops.motorista.MotoristaRepository;
import br.edu.cesmac.frota_ops.motorista.MotoristaService;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;

/**
 * Testes da página de veículos (visão híbrida de análise e gestão).
 *
 * <p>Cobre a listagem, a busca, cada filtro, a paginação server-side com deep-link, a
 * consistência dos indicadores em relação ao banco, o ranking de quilometragem, o escopo
 * do perfil MOTORISTA e a preservação das operações CRUD. As expectativas usam
 * {@link LocalDate#now()} para não dependerem das datas semeadas pelas migrations.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VeiculoPaginaTests {

    @Autowired
    private VeiculoService veiculoService;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------
    // Consistência das métricas
    // ------------------------------------------------------------------

    @Test
    void indicadoresDevemCorresponderAsConsultasDoBanco() {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario alocado = criarMotorista("kpi.alocado", "Motorista Alocado", "101.202.303-40", "10120230340",
                LocalDate.now().plusYears(3));
        LocalDate hoje = LocalDate.now();

        criarVeiculo("KPI0001", "Disponível", hoje.minusDays(10), alocado.getMotorista());
        criarVeiculo("KPI0002", "Em trânsito", hoje.plusDays(5), alocado.getMotorista());
        criarVeiculo("KPI0003", "Em manutenção", null, null);
        criarVeiculo("KPI0004", "Indisponível", hoje.plusDays(60), null);

        VeiculoPaginaDTO resumo = veiculoService.montarResumo(null);
        VeiculoIndicadoresDTO indicadores = resumo.indicadores();

        assertThat(indicadores.total()).isEqualTo(veiculoRepository.count()).isEqualTo(4);
        assertThat(indicadores.disponiveis()).isEqualTo(veiculoRepository.countByStatus("Disponível")).isEqualTo(1);
        assertThat(indicadores.emTransito()).isEqualTo(veiculoRepository.countByStatus("Em trânsito")).isEqualTo(1);
        assertThat(indicadores.emManutencao())
                .isEqualTo(veiculoRepository.countByStatus("Em manutenção")).isEqualTo(1);
        assertThat(indicadores.indisponiveis())
                .isEqualTo(veiculoRepository.countByStatus("Indisponível")).isEqualTo(1);

        // Os cards de status fecham com o total.
        assertThat(indicadores.disponiveis() + indicadores.emTransito()
                + indicadores.emManutencao() + indicadores.indisponiveis()).isEqualTo(indicadores.total());

        assertThat(indicadores.revisoesVencidas())
                .isEqualTo(veiculoRepository.countByProximaRevisaoBefore(hoje)).isEqualTo(1);
        assertThat(indicadores.revisoesProximas()).isEqualTo(veiculoRepository
                .countByProximaRevisaoBetween(hoje, hoje.plusDays(VeiculoService.JANELA_REVISAO_DIAS))).isEqualTo(1);
        assertThat(indicadores.semMotorista()).isEqualTo(veiculoRepository.countByMotoristaIsNull()).isEqualTo(2);

        assertThat(resumo.escopoRestrito()).isFalse();
        assertThat(resumo.escopoDescricao()).isEqualTo("Frota completa");
        assertThat(resumo.janelaRevisaoDias()).isEqualTo(VeiculoService.JANELA_REVISAO_DIAS);
    }

    @Test
    void deveClassificarSituacaoDeManutencaoIgualAoFiltroDerivado() {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        LocalDate hoje = LocalDate.now();
        criarVeiculo("SIT0001", "Disponível", hoje.plusDays(30), null);
        criarVeiculo("SIT0002", "Disponível", hoje.plusDays(10), null);
        criarVeiculo("SIT0003", "Em manutenção", hoje.minusDays(5), null);
        criarVeiculo("SIT0004", "Indisponível", null, null);

        // "Em manutenção" vem do status e prevalece sobre a data de revisão.
        assertThat(placas(filtro(null, null, "Em manutenção", null, null)))
                .containsExactly("SIT0003");
        // "Atenção" = revisão dentro da janela de 15 dias, exceto veículos já em manutenção.
        assertThat(placas(filtro(null, null, "Atenção", null, null)))
                .containsExactly("SIT0002");
        // "Em dia" = sem revisão agendada ou além da janela.
        assertThat(placas(filtro(null, null, "Em dia", null, null)))
                .containsExactly("SIT0001", "SIT0004");

        // O indicador de situação é coerente com o filtro derivado do dashboard.
        assertThat(veiculoService.montarResumo(null).indicadores().revisoesProximas()).isEqualTo(1);
        assertThat(veiculoService.montarResumo(null).indicadores().revisoesVencidas()).isEqualTo(1);
    }

    @Test
    void deveOrdenarMaioresQuilometragensEDevolverNoMaximoCinco() {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        for (int i = 1; i <= 6; i++) {
            criarVeiculo("KMT000" + i, "Modelo KM" + i, i * 1000L, "Disponível", null, null);
        }

        List<VeiculoQuilometragemDTO> ranking = veiculoService.montarResumo(null).maioresQuilometragens();

        assertThat(ranking).hasSize(5);
        assertThat(ranking).extracting(VeiculoQuilometragemDTO::placa)
                .containsExactly("KMT0006", "KMT0005", "KMT0004", "KMT0003", "KMT0002");
        assertThat(ranking).extracting(VeiculoQuilometragemDTO::quilometragem)
                .containsExactly(6000L, 5000L, 4000L, 3000L, 2000L);
    }

    // ------------------------------------------------------------------
    // Busca e filtros
    // ------------------------------------------------------------------

    @Test
    void deveBuscarPorPlacaModeloEMotorista() {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario alvo = criarMotorista("busca.alvo", "Busca Alvo", "202.303.404-50", "20230340450",
                LocalDate.now().plusYears(3));
        criarVeiculo("BUS0001", "Mercedes Actros", 1500L, "Disponível", null, alvo.getMotorista());
        criarVeiculo("ZZZ0009", "Outro Modelo", 900L, "Disponível", null, null);

        // Busca por placa, insensível a caixa e a espaços nas extremidades.
        assertThat(placas(filtro("  bus0001  ", null, null, null, null))).containsExactly("BUS0001");
        // Busca por modelo.
        assertThat(placas(filtro("actros", null, null, null, null))).containsExactly("BUS0001");
        // Busca por nome do motorista.
        assertThat(placas(filtro("busca alvo", null, null, null, null))).containsExactly("BUS0001");
        // Sem correspondência: estado vazio, não erro.
        assertThat(placas(filtro("inexistente", null, null, null, null))).isEmpty();
    }

    @Test
    void deveFiltrarPorStatusRevisaoEVinculo() {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario alocado = criarMotorista("filtro.alocado", "Filtro Alocado", "303.404.505-60", "30340450560",
                LocalDate.now().plusYears(3));
        LocalDate hoje = LocalDate.now();

        criarVeiculo("FIL0001", "Disponível", hoje.minusDays(2), alocado.getMotorista());
        criarVeiculo("FIL0002", "Em trânsito", hoje.plusDays(7), null);
        criarVeiculo("FIL0003", "Em trânsito", hoje.plusDays(90), null);

        assertThat(placas(filtro(null, "Em trânsito", null, null, null)))
                .containsExactly("FIL0002", "FIL0003");
        assertThat(placas(filtro(null, null, null, "vencida", null))).containsExactly("FIL0001");
        assertThat(placas(filtro(null, null, null, "proxima", null))).containsExactly("FIL0002");
        assertThat(placas(filtro(null, null, null, null, "com"))).containsExactly("FIL0001");
        assertThat(placas(filtro(null, null, null, null, "sem"))).containsExactly("FIL0002", "FIL0003");

        // Filtros combinados: status + vínculo.
        assertThat(placas(filtro(null, "Em trânsito", null, null, "sem")))
                .containsExactly("FIL0002", "FIL0003");
    }

    @Test
    void devePaginarNoServidorComDezRegistrosPorPagina() {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        for (int i = 1; i <= 12; i++) {
            criarVeiculo("PAG" + String.format("%04d", i), "Disponível", null, null);
        }

        VeiculoFiltro filtro = filtro(null, "Disponível", null, null, null);

        Page<Veiculo> primeira = veiculoService.listar(null, filtro, 1);
        assertThat(VeiculoService.TAMANHO_PAGINA).isEqualTo(10);
        assertThat(primeira.getTotalElements()).isEqualTo(12);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(primeira.getContent()).hasSize(10);
        assertThat(primeira.hasNext()).isTrue();
        assertThat(primeira.hasPrevious()).isFalse();

        Page<Veiculo> segunda = veiculoService.listar(null, filtro, 2);
        assertThat(segunda.getContent()).hasSize(2);
        assertThat(segunda.hasPrevious()).isTrue();
        assertThat(segunda.hasNext()).isFalse();

        // Sem sobreposição e ordenação estável por id.
        List<Long> idsPrimeira = primeira.getContent().stream().map(Veiculo::getId).collect(Collectors.toList());
        List<Long> idsSegunda = segunda.getContent().stream().map(Veiculo::getId).collect(Collectors.toList());
        assertThat(idsPrimeira).isSorted();
        assertThat(idsSegunda).isSorted();
        assertThat(idsPrimeira).doesNotContainAnyElementsOf(idsSegunda);
    }

    // ------------------------------------------------------------------
    // Renderização
    // ------------------------------------------------------------------

    @Test
    void deveRenderizarIndicadoresGraficoEFiltrosPreservandoATabela() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        criarVeiculo("REND001", "Ford Cargo", 1000L, "Disponível", LocalDate.now().minusDays(3), null);

        mockMvc.perform(get("/veiculos"))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/list"))
                // Cards de indicadores
                .andExpect(content().string(containsString("Total da frota")))
                .andExpect(content().string(containsString("Disponíveis")))
                .andExpect(content().string(containsString("Em trânsito")))
                .andExpect(content().string(containsString("Em manutenção")))
                .andExpect(content().string(containsString("Indisponíveis")))
                .andExpect(content().string(containsString("Revisões vencidas")))
                .andExpect(content().string(containsString("Revisões próximas")))
                .andExpect(content().string(containsString("Sem condutor atribuído")))
                // Gráfico horizontal de quilometragem e seu aviso de escopo
                .andExpect(content().string(containsString("Maiores quilometragens")))
                .andExpect(content().string(containsString("chart-quilometragem")))
                .andExpect(content().string(containsString("window.FLEET_VEICULOS")))
                .andExpect(content().string(containsString("Hodômetro atual do cadastro")))
                // Formatação de quilometragem (separador de milhar)
                .andExpect(content().string(containsString("1.000 km")))
                // Filtros
                .andExpect(content().string(containsString("id=\"status\"")))
                .andExpect(content().string(containsString("id=\"situacao\"")))
                .andExpect(content().string(containsString("id=\"revisao\"")))
                .andExpect(content().string(containsString("id=\"vinculo\"")))
                // A tabela continua sendo a base da listagem
                .andExpect(content().string(containsString("data-table--frota")))
                .andExpect(content().string(containsString("class=\"plate\">REND001")));
    }

    @Test
    void devePreservarFiltrosNosLinksDePaginacao() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        // 11 veículos sem motorista: duas páginas com o filtro vinculo=sem.
        for (int i = 1; i <= 11; i++) {
            criarVeiculo("PAG" + String.format("%04d", i), "Disponível", null, null);
        }

        mockMvc.perform(get("/veiculos").param("vinculo", "sem"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página")))
                .andExpect(content().string(containsString("Próxima")))
                // Deep-link: o filtro acompanha os links de navegação.
                .andExpect(content().string(containsString("vinculo=sem")))
                .andExpect(content().string(containsString("class=\"plate\">PAG0001")))
                .andExpect(content().string(not(containsString("class=\"plate\">PAG0011"))));

        mockMvc.perform(get("/veiculos").param("vinculo", "sem").param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"plate\">PAG0011")))
                .andExpect(content().string(not(containsString("class=\"plate\">PAG0001"))));
    }

    @Test
    void deveExibirEstadosVaziosSemDadosESemCorrespondenciaDeFiltro() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        // Frota vazia: sem tabela e sem canvas de gráfico.
        mockMvc.perform(get("/veiculos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum veículo encontrado")))
                .andExpect(content().string(containsString("Não há veículos no escopo exibido.")))
                .andExpect(content().string(containsString("Sem dados de quilometragem")))
                .andExpect(content().string(not(containsString("id=\"chart-quilometragem\""))));

        criarVeiculo("UNICO01", "Disponível", null, null);

        // Filtro sem correspondência: mensagem específica, chip do filtro e atalho para limpar.
        mockMvc.perform(get("/veiculos").param("q", "ZZZZZZ"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum veículo corresponde aos filtros atuais.")))
                .andExpect(content().string(containsString("Filtros aplicados")))
                .andExpect(content().string(containsString("Busca: ZZZZZZ")))
                .andExpect(content().string(not(containsString("class=\"plate\">UNICO01"))));
    }

    // ------------------------------------------------------------------
    // Escopo do perfil
    // ------------------------------------------------------------------

    @Test
    void deveRestringirListagemAosVeiculosDoMotoristaLogado() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario logado = criarMotorista("escopo.veiculo", "Escopo Veiculo", "404.505.606-70", "40450560670",
                LocalDate.now().plusYears(3));
        Usuario terceiro = criarMotorista("escopo.outro", "Escopo Outro", "505.606.707-80", "50560670780",
                LocalDate.now().plusYears(3));

        criarVeiculo("PROP001", "Disponível", LocalDate.now().minusDays(4), logado.getMotorista());
        criarVeiculo("PROP002", "Em trânsito", null, logado.getMotorista());
        criarVeiculo("FORA001", "Disponível", LocalDate.now().minusDays(4), terceiro.getMotorista());

        mockMvc.perform(get("/veiculos").sessionAttr("usuarioLogado", logado))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sua frota")))
                .andExpect(content().string(containsString("class=\"plate\">PROP001")))
                .andExpect(content().string(containsString("class=\"plate\">PROP002")))
                .andExpect(content().string(not(containsString("FORA001"))))
                // O indicador de veículos sem motorista é irrelevante no escopo restrito.
                .andExpect(content().string(not(containsString("Sem condutor atribuído"))));

        // O mesmo recorte, visto por um perfil administrativo, enxerga a frota inteira.
        mockMvc.perform(get("/veiculos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Frota completa")))
                .andExpect(content().string(containsString("FORA001")));
    }

    // ------------------------------------------------------------------
    // Operações CRUD preservadas
    // ------------------------------------------------------------------

    @Test
    void devePreservarDetalhesEdicaoEExclusaoAutorizada() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario admin = admin();
        Veiculo veiculo = criarVeiculo("CRUD001", "Scania R450", 4200L, "Disponível", null, null);
        Long id = veiculo.getId();

        mockMvc.perform(get("/veiculos/detalhes/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/detalhes"))
                .andExpect(content().string(containsString("CRUD001")));

        mockMvc.perform(get("/veiculos/editar/" + id).sessionAttr("usuarioLogado", admin))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/editar"))
                .andExpect(content().string(containsString("CRUD001")));

        mockMvc.perform(post("/veiculos/excluir/" + id).sessionAttr("usuarioLogado", admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"))
                .andExpect(flash().attribute("sucesso", "Veículo excluído com sucesso!"));

        assertThat(veiculoRepository.findById(id)).isEmpty();
    }

    @Test
    void deveBloquearCadastroEExclusaoParaPerfilNaoAdministrador() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario motorista = criarMotorista("sem.permissao", "Sem Permissao", "606.707.808-90", "60670780890",
                LocalDate.now().plusYears(3));
        Veiculo proprio = criarVeiculo("PERM001", "Disponível", null, motorista.getMotorista());

        mockMvc.perform(get("/veiculos/novo").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"))
                .andExpect(flash().attribute("erro",
                        "Acesso negado: apenas administradores podem cadastrar novos veículos."));

        mockMvc.perform(post("/veiculos/excluir/" + proprio.getId()).sessionAttr("usuarioLogado", motorista))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"))
                .andExpect(flash().attribute("erro",
                        "Acesso negado: apenas administradores podem excluir veículos."));

        assertThat(veiculoRepository.findById(proprio.getId())).isPresent();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Usuario admin() {
        return new Usuario(null, "admin.pagina", "Administrador Pagina", "admin.pagina@fleetops.com",
                "123456", "ADMIN", true);
    }

    private VeiculoFiltro filtro(String q, String status, String situacao, String revisao, String vinculo) {
        return new VeiculoFiltro(q, status, situacao, revisao, vinculo);
    }

    /** Placas da primeira página do resultado, na ordem devolvida pelo banco. */
    private List<String> placas(VeiculoFiltro filtro) {
        return veiculoService.listar(null, filtro, 1).getContent().stream()
                .map(Veiculo::getPlaca)
                .collect(Collectors.toList());
    }

    /** Remove o histórico e a frota para que as contagens do teste sejam determinísticas. */
    private void limparFrota() {
        manutencaoRepository.deleteAllInBatch();
        veiculoRepository.deleteAllInBatch();
    }

    private Usuario criarMotorista(String username, String nome, String cpf, String numeroCnh,
                                   LocalDate validadeCnh) {
        Usuario usuario = new Usuario(null, username, nome, username + "@fleetops.com", "123456", "OPERADOR", true);
        usuarioService.salvar(usuario);

        Motorista motorista = new Motorista(null, nome, cpf, numeroCnh, "B", validadeCnh, null);
        motorista.setUsuario(usuario);
        motoristaService.salvar(motorista);

        // Mantém o vínculo bidirecional em memória: em um único contexto de persistência
        // (teste transacional) o lado inverso Usuario.motorista não é preenchido pelo JPA.
        usuario.setMotorista(motorista);

        // Recarrega pelo login para obter o perfil MOTORISTA e o vínculo já resolvidos.
        return usuarioService.autenticar(username, "123456").orElseThrow();
    }

    private Veiculo criarVeiculo(String placa, String status, LocalDate proximaRevisao, Motorista motorista) {
        return criarVeiculo(placa, "Modelo " + placa, 1000L, status, proximaRevisao, motorista);
    }

    private Veiculo criarVeiculo(String placa, String modelo, long quilometragem, String status,
                                 LocalDate proximaRevisao, Motorista motorista) {
        Veiculo veiculo = new Veiculo(null, placa, modelo, 2024, quilometragem, status, null,
                proximaRevisao, null, null, "Em dia");
        veiculo.setMotorista(motorista);
        return veiculoRepository.saveAndFlush(veiculo);
    }
}
