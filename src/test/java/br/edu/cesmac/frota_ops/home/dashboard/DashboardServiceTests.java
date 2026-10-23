package br.edu.cesmac.frota_ops.home.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.cesmac.frota_ops.manutencao.Manutencao;
import br.edu.cesmac.frota_ops.manutencao.ManutencaoRepository;
import br.edu.cesmac.frota_ops.motorista.Motorista;
import br.edu.cesmac.frota_ops.motorista.MotoristaRepository;
import br.edu.cesmac.frota_ops.motorista.MotoristaService;
import br.edu.cesmac.frota_ops.tag.TagRepository;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoRepository;

/**
 * Testes do dashboard operacional. As expectativas são construídas em relação a
 * {@link LocalDate#now()} para não dependerem das datas semeadas pelas migrations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardServiceTests {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------
    // Agregação
    // ------------------------------------------------------------------

    @Test
    void cadaCardDeveCorresponderAsConsultasAgregadas() {
        DashboardDTO dashboard = dashboardService.montar(admin());
        IndicadoresDTO indicadores = dashboard.indicadores();

        long totalReal = veiculoRepository.count();
        assertThat(indicadores.totalVeiculos()).isEqualTo(totalReal);
        assertThat(indicadores.disponiveis()).isEqualTo(veiculoRepository.countByStatus("Disponível"));
        assertThat(indicadores.emTransito()).isEqualTo(veiculoRepository.countByStatus("Em trânsito"));
        assertThat(indicadores.emManutencao()).isEqualTo(veiculoRepository.countByStatus("Em manutenção"));
        assertThat(indicadores.indisponiveis()).isEqualTo(veiculoRepository.countByStatus("Indisponível"));

        long somaPorStatus = indicadores.disponiveis() + indicadores.emTransito()
                + indicadores.emManutencao() + indicadores.indisponiveis();
        assertThat(somaPorStatus).isEqualTo(totalReal);

        // A distribuição do gráfico de rosca tem os quatro status do domínio e fecha com o total.
        assertThat(dashboard.distribuicaoStatus()).hasSize(4);
        assertThat(dashboard.distribuicaoStatus()).extracting(DistribuicaoStatusDTO::status)
                .containsExactly("Disponível", "Em trânsito", "Em manutenção", "Indisponível");
        assertThat(dashboard.distribuicaoStatus().stream().mapToLong(DistribuicaoStatusDTO::total).sum())
                .isEqualTo(totalReal);
        assertThat(dashboard.distribuicaoStatus()).allSatisfy(item -> {
            assertThat(item.cor()).startsWith("#");
            assertThat(item.badgeClasse()).startsWith("status-");
        });

        LocalDate hoje = LocalDate.now();
        assertThat(indicadores.revisoesVencidas())
                .isEqualTo(veiculoRepository.countByProximaRevisaoBefore(hoje));
        assertThat(indicadores.revisoesProximas()).isEqualTo(veiculoRepository.countByProximaRevisaoBetween(
                hoje, hoje.plusDays(DashboardService.JANELA_REVISAO_DIAS)));
        assertThat(indicadores.revisoesEmDia()).isEqualTo(
                totalReal - indicadores.revisoesVencidas() - indicadores.revisoesProximas());

        // A série mensal sempre tem os 6 buckets, mesmo com meses zerados.
        assertThat(dashboard.manutencoesPorMes()).hasSize(DashboardService.MESES_MANUTENCOES);
        assertThat(dashboard.manutencoesPorMes()).extracting(SerieMensalDTO::total)
                .allMatch(total -> total >= 0);

        assertThat(dashboard.proximasRevisoes()).hasSizeLessThanOrEqualTo(DashboardService.LIMITE_REVISOES);
        assertThat(dashboard.escopoRestrito()).isFalse();
        assertThat(dashboard.escopoDescricao()).isEqualTo("Frota completa");
        assertThat(dashboard.janelaRevisoes()).contains(String.valueOf(DashboardService.JANELA_REVISAO_DIAS));
        assertThat(dashboard.janelaCnh()).contains(String.valueOf(DashboardService.JANELA_CNH_DIAS));
        assertThat(dashboard.janelaManutencoes()).contains(String.valueOf(DashboardService.MESES_MANUTENCOES));
    }

    @Test
    void deveClassificarRevisaoVencidaProximaEFutura() {
        limparFrota();

        LocalDate hoje = LocalDate.now();
        criarVeiculo("REV0001", "Disponível", hoje.minusDays(1), null);
        criarVeiculo("REV0002", "Disponível", hoje, null);
        criarVeiculo("REV0003", "Disponível", hoje.plusDays(DashboardService.JANELA_REVISAO_DIAS), null);
        criarVeiculo("REV0004", "Disponível", hoje.plusDays(DashboardService.JANELA_REVISAO_DIAS + 1L), null);
        criarVeiculo("REV0005", "Disponível", null, null);

        DashboardDTO dashboard = dashboardService.montar(admin());
        IndicadoresDTO indicadores = dashboard.indicadores();

        assertThat(indicadores.totalVeiculos()).isEqualTo(5);
        assertThat(indicadores.revisoesVencidas()).isEqualTo(1);
        assertThat(indicadores.revisoesProximas()).isEqualTo(2);
        // "Em dia" = revisão futura fora da janela + veículo sem revisão agendada.
        assertThat(indicadores.revisoesEmDia()).isEqualTo(2);

        assertThat(dashboard.proximasRevisoes()).extracting(RevisaoVeiculoDTO::placa)
                .containsExactly("REV0001", "REV0002", "REV0003");
        assertThat(dashboard.proximasRevisoes()).extracting(RevisaoVeiculoDTO::situacao)
                .containsExactly("Vencida", "Próxima", "Próxima");
        assertThat(dashboard.proximasRevisoes().get(0).diasRestantes()).isEqualTo(-1);

        // Revisão futura fora da janela não entra na lista nem nos alertas.
        assertThat(dashboard.proximasRevisoes()).extracting(RevisaoVeiculoDTO::placa).doesNotContain("REV0004");
        assertThat(dashboard.alertas()).extracting(AlertaDTO::titulo)
                .contains("Revisões vencidas", "Revisões próximas");

        AlertaDTO vencidas = alertaPorTitulo(dashboard, "Revisões vencidas");
        assertThat(vencidas.nivel()).isEqualTo("ALTA");
        assertThat(vencidas.descricao()).isEqualTo("1 veículo está com a revisão vencida");

        AlertaDTO proximas = alertaPorTitulo(dashboard, "Revisões próximas");
        assertThat(proximas.nivel()).isEqualTo("MEDIA");
        assertThat(proximas.descricao()).isEqualTo("2 veículos têm revisão nos próximos 15 dias");

        // Sem manutenções na janela e sem tags com veículos: os gráficos não são exibidos.
        assertThat(dashboard.exibirGraficoManutencoes()).isFalse();
        assertThat(dashboard.exibirGraficoTags()).isFalse();
    }

    @Test
    void deveContarManutencoesPelaDataRealDoRegistro() {
        limparFrota();

        LocalDate hoje = LocalDate.now();
        Veiculo veiculo = criarVeiculo("MAN0001", "Disponível", null, null);

        criarManutencao(veiculo, hoje);
        criarManutencao(veiculo, hoje.withDayOfMonth(1));
        criarManutencao(veiculo, hoje.minusMonths(DashboardService.MESES_MANUTENCOES - 1).withDayOfMonth(1));
        // Fora da janela de 6 meses: não deve ser contabilizada.
        criarManutencao(veiculo, hoje.minusMonths(DashboardService.MESES_MANUTENCOES).withDayOfMonth(1));

        DashboardDTO dashboard = dashboardService.montar(admin());

        assertThat(dashboard.manutencoesPorMes()).hasSize(DashboardService.MESES_MANUTENCOES);
        assertThat(dashboard.manutencoesPorMes().stream().mapToLong(SerieMensalDTO::total).sum()).isEqualTo(3);
        assertThat(dashboard.exibirGraficoManutencoes()).isTrue();
        assertThat(dashboard.manutencoesPorMes()).extracting(SerieMensalDTO::rotulo)
                .allMatch(rotulo -> rotulo.matches("[A-Za-z]{3}/\\d{2}"));
    }

    @Test
    void deveExibirGraficoDeTagsSomenteComVolumeQueJustifique() {
        limparFrota();

        // Uma única tag com veículo não justifica o gráfico (que compara categorias).
        Veiculo veiculo = criarVeiculoComTags("TAG0001", "Rota Urbana");
        assertThat(veiculo.getId()).isNotNull();

        DashboardDTO dashboard = dashboardService.montar(admin());
        assertThat(dashboard.exibirGraficoTags()).isFalse();

        criarVeiculoComTags("TAG0002", "Rastreado GPS");
        dashboard = dashboardService.montar(admin());

        assertThat(dashboard.exibirGraficoTags()).isTrue();
        assertThat(dashboard.veiculosPorTag()).filteredOn(tag -> tag.total() > 0)
                .extracting(VeiculoPorTagDTO::tag)
                .containsExactlyInAnyOrder("Rota Urbana", "Rastreado GPS");
    }

    // ------------------------------------------------------------------
    // Banco vazio
    // ------------------------------------------------------------------

    @Test
    void deveRetornarEstadoVazioQuandoNaoHaDados() {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        DashboardDTO dashboard = dashboardService.montar(admin());
        IndicadoresDTO indicadores = dashboard.indicadores();

        assertThat(indicadores.totalVeiculos()).isZero();
        assertThat(indicadores.disponiveis()).isZero();
        assertThat(indicadores.emTransito()).isZero();
        assertThat(indicadores.emManutencao()).isZero();
        assertThat(indicadores.indisponiveis()).isZero();
        assertThat(indicadores.revisoesVencidas()).isZero();
        assertThat(indicadores.revisoesProximas()).isZero();
        assertThat(indicadores.revisoesEmDia()).isZero();
        assertThat(indicadores.cnhsVencidas()).isZero();
        assertThat(indicadores.cnhsVencendo()).isZero();

        assertThat(dashboard.distribuicaoStatus()).hasSize(4);
        assertThat(dashboard.distribuicaoStatus()).allSatisfy(item -> assertThat(item.total()).isZero());

        assertThat(dashboard.manutencoesPorMes()).hasSize(DashboardService.MESES_MANUTENCOES);
        assertThat(dashboard.manutencoesPorMes()).allSatisfy(mes -> assertThat(mes.total()).isZero());

        assertThat(dashboard.exibirGraficoManutencoes()).isFalse();
        assertThat(dashboard.exibirGraficoTags()).isFalse();
        assertThat(dashboard.alertas()).isEmpty();
        assertThat(dashboard.proximasRevisoes()).isEmpty();
        assertThat(dashboard.cnhsEmAlerta()).isEmpty();
    }

    // ------------------------------------------------------------------
    // Perfis
    // ------------------------------------------------------------------

    @Test
    void deveRestringirDadosAoEscopoDoPerfilMotorista() {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario motoristaA = criarMotorista("motorista.a", "Alice Motorista", "555.666.777-88", "55566677788",
                LocalDate.now().plusYears(3));
        Usuario motoristaB = criarMotorista("motorista.b", "Bruno Motorista", "999.888.777-66", "99988877766",
                LocalDate.now().plusYears(3));

        criarVeiculo("SCOPE01", "Disponível", LocalDate.now().minusDays(3), motoristaA.getMotorista());
        criarVeiculo("SCOPE02", "Em trânsito", LocalDate.now().plusDays(40), motoristaA.getMotorista());
        criarVeiculo("OUTRO01", "Disponível", LocalDate.now().minusDays(3), motoristaB.getMotorista());

        DashboardDTO restrito = dashboardService.montar(motoristaA);

        assertThat(restrito.escopoRestrito()).isTrue();
        assertThat(restrito.escopoDescricao()).contains("Sua frota");
        assertThat(restrito.indicadores().totalVeiculos()).isEqualTo(2);
        assertThat(restrito.indicadores().disponiveis()).isEqualTo(1);
        assertThat(restrito.indicadores().emTransito()).isEqualTo(1);
        assertThat(restrito.indicadores().emManutencao()).isZero();
        assertThat(restrito.indicadores().revisoesVencidas()).isEqualTo(1);
        assertThat(restrito.indicadores().revisoesProximas()).isZero();
        assertThat(restrito.proximasRevisoes()).extracting(RevisaoVeiculoDTO::placa)
                .containsExactly("SCOPE01");
        assertThat(restrito.proximasRevisoes()).extracting(RevisaoVeiculoDTO::placa)
                .doesNotContain("OUTRO01");

        // O mesmo agregado, visto por um perfil administrativo, enxerga a frota inteira.
        DashboardDTO completo = dashboardService.montar(admin());
        assertThat(completo.escopoRestrito()).isFalse();
        assertThat(completo.indicadores().totalVeiculos()).isEqualTo(3);
        assertThat(completo.indicadores().revisoesVencidas()).isEqualTo(2);
    }

    @Test
    void deveZerarDashboardDoMotoristaSemCadastroVinculado() {
        limparFrota();

        Usuario semCadastro = new Usuario(null, "sem.cadastro", "Sem Cadastro", "sem.cadastro@fleetops.com",
                "123456", "MOTORISTA", true);

        DashboardDTO dashboard = dashboardService.montar(semCadastro);

        assertThat(dashboard.escopoRestrito()).isTrue();
        assertThat(dashboard.escopoDescricao()).isEqualTo("Sem frota vinculada ao seu cadastro");
        assertThat(dashboard.indicadores().totalVeiculos()).isZero();
        assertThat(dashboard.proximasRevisoes()).isEmpty();
        assertThat(dashboard.alertas()).isEmpty();
    }

    @Test
    void deveAlertarSomenteCnhVencidaOuDentroDaJanela() {
        motoristaRepository.deleteAllInBatch();

        criarMotorista("cnh.vencida", "Cnh Vencida", "111.111.111-11", "11111111111",
                LocalDate.now().minusDays(10));
        criarMotorista("cnh.avencer", "Cnh AVencer", "222.222.222-22", "22222222222",
                LocalDate.now().plusDays(30));
        criarMotorista("cnh.ok", "Cnh Fora da Janela", "333.333.333-33", "33333333333",
                LocalDate.now().plusDays(DashboardService.JANELA_CNH_DIAS + 1L));

        DashboardDTO dashboard = dashboardService.montar(admin());

        assertThat(dashboard.indicadores().cnhsVencidas()).isEqualTo(1);
        assertThat(dashboard.indicadores().cnhsVencendo()).isEqualTo(1);
        assertThat(dashboard.cnhsEmAlerta()).hasSize(2);
        assertThat(dashboard.cnhsEmAlerta()).extracting(CnhAlertaDTO::nome)
                .containsExactly("Cnh Vencida", "Cnh AVencer");
        assertThat(dashboard.cnhsEmAlerta()).extracting(CnhAlertaDTO::situacao)
                .containsExactly("Vencida", "A vencer");
        assertThat(dashboard.alertas()).extracting(AlertaDTO::titulo)
                .contains("CNHs vencidas", "CNHs a vencer");
    }

    // ------------------------------------------------------------------
    // Renderização
    // ------------------------------------------------------------------

    @Test
    void deveRenderizarDashboardComKpisGraficosEAlertas() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario admin = new Usuario(null, "render.admin", "Admin Render", "render.admin@fleetops.com",
                "123456", "ADMIN", true);
        usuarioService.salvar(admin);

        // Dados mínimos para que os três gráficos existam no HTML.
        Veiculo primeiro = criarVeiculo("REND001", "Disponível", LocalDate.now().minusDays(2), null);
        Veiculo segundo = criarVeiculo("REND002", "Em trânsito", null, null);
        adicionarTag(primeiro, "Rota Urbana");
        adicionarTag(segundo, "Rastreado GPS");
        criarManutencao(primeiro, LocalDate.now());

        mockMvc.perform(get("/").sessionAttr("usuarioLogado", admin))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(content().string(containsString("Visão Geral da Frota")))
                .andExpect(content().string(containsString("Alertas operacionais")))
                .andExpect(content().string(containsString("Revisões vencidas")))
                .andExpect(content().string(containsString("Habilitações em alerta")))
                .andExpect(content().string(containsString("Próximas revisões")))
                .andExpect(content().string(containsString("Frota completa")))
                .andExpect(content().string(containsString("REND001")))
                .andExpect(content().string(containsString("chart-distribuicao")))
                .andExpect(content().string(containsString("chart-manutencoes")))
                .andExpect(content().string(containsString("chart-tags")))
                .andExpect(content().string(containsString("window.FLEET_DASHBOARD")))
                .andExpect(content().string(containsString("/js/vendor/chart.umd.min.js")));
    }

    @Test
    void deveRenderizarEstadosVaziosAcessiveisQuandoNaoHaDados() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum alerta operacional")))
                .andExpect(content().string(containsString("Sem veículos cadastrados")))
                .andExpect(content().string(containsString("Nenhuma manutenção registrada")))
                .andExpect(content().string(containsString("Nenhuma CNH em alerta")))
                .andExpect(content().string(containsString("Nenhuma revisão dentro da janela de alerta")))
                // Sem dados, nenhuma <canvas> é renderizada. A checagem usa o id do canvas
                // para não colidir com os ids de título (ex.: "titulo-chart-manutencoes").
                .andExpect(content().string(not(containsString("id=\"chart-distribuicao\""))))
                .andExpect(content().string(not(containsString("id=\"chart-manutencoes\""))))
                .andExpect(content().string(not(containsString("id=\"chart-tags\""))));
    }

    @Test
    void naoDeveExporVeiculosDeTerceirosNoDashboardDoMotorista() throws Exception {
        limparFrota();
        motoristaRepository.deleteAllInBatch();

        Usuario logado = criarMotorista("escopo.ui", "Escopo UI", "444.444.444-44", "44444444444",
                LocalDate.now().plusYears(3));
        Usuario terceiro = criarMotorista("escopo.terceiro", "Escopo Terceiro", "777.777.777-77", "77777777777",
                LocalDate.now().plusYears(3));

        criarVeiculo("UI00001", "Disponível", LocalDate.now().minusDays(5), logado.getMotorista());
        criarVeiculo("UI00002", "Disponível", LocalDate.now().minusDays(5), logado.getMotorista());
        criarVeiculo("FORA001", "Disponível", LocalDate.now().minusDays(5), terceiro.getMotorista());

        mockMvc.perform(get("/").sessionAttr("usuarioLogado", logado))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sua frota")))
                .andExpect(content().string(containsString("UI00001")))
                .andExpect(content().string(containsString("UI00002")))
                .andExpect(content().string(not(containsString("FORA001"))))
                .andExpect(content().string(not(containsString("Frota completa"))));
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Usuario admin() {
        return new Usuario(null, "admin.teste", "Administrador Teste", "admin.teste@fleetops.com",
                "123456", "ADMIN", true);
    }

    private AlertaDTO alertaPorTitulo(DashboardDTO dashboard, String titulo) {
        return dashboard.alertas().stream()
                .filter(alerta -> titulo.equals(alerta.titulo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Alerta não encontrado: " + titulo));
    }

    /** Remove a frota e o histórico para que as contagens do teste sejam determinísticas. */
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
        Veiculo veiculo = new Veiculo(null, placa, "Modelo " + placa, 2024, 1000L, status, null,
                proximaRevisao, null, null, "Em dia");
        veiculo.setMotorista(motorista);
        return veiculoRepository.saveAndFlush(veiculo);
    }

    private Veiculo criarVeiculoComTags(String placa, String nomeTag) {
        return adicionarTag(criarVeiculo(placa, "Disponível", null, null), nomeTag);
    }

    private Veiculo adicionarTag(Veiculo veiculo, String nomeTag) {
        veiculo.getTags().add(tagRepository.findAll().stream()
                .filter(tag -> nomeTag.equals(tag.getNome()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tag não encontrada: " + nomeTag)));
        return veiculoRepository.saveAndFlush(veiculo);
    }

    private void criarManutencao(Veiculo veiculo, LocalDate data) {
        manutencaoRepository.saveAndFlush(
                new Manutencao(null, data, 1000L, "Serviço de teste", "Oficina de teste", null, veiculo));
    }
}
