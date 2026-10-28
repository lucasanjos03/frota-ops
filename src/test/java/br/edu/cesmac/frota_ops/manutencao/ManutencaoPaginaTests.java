package br.edu.cesmac.frota_ops.manutencao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.cesmac.frota_ops.home.dashboard.RevisaoVeiculoDTO;
import br.edu.cesmac.frota_ops.motorista.Motorista;
import br.edu.cesmac.frota_ops.motorista.MotoristaRepository;
import br.edu.cesmac.frota_ops.motorista.MotoristaService;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoRepository;

/**
 * Testes da página de manutenções (interface analítica e operacional).
 *
 * <p>A validação exigida cobre: contagem por intervalo (limite da janela de 12 meses),
 * ordenação cronológica (com desempate por id), meses sem registros zerados, filtros de
 * veículo/serviço/período com limites inclusivos, histórico vazio, consistência de contagem
 * contra o banco, separação entre revisões atrasadas e previstas (janela de 15 dias) e o
 * escopo por perfil (MOTORISTA restrito; ADMIN/OPERADOR com frota completa).</p>
 *
 * <p>As agregações são sempre chamadas com data de referência explícita
 * ({@code montarResumo(logado, referencia)}), nunca {@code LocalDate.now()} interno, para que
 * o teste não dependa do dia em que roda.</p>
 *
 * <p>Séries distintas por construção: o histórico conta manutenções <em>realizadas</em>
 * ({@code data_manutencao}) e o painel de revisões é <em>previsão</em>
 * ({@code veiculo.proxima_revisao}). Os testes verificam que uma nunca entra no indicador da
 * outra.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManutencaoPaginaTests {

    @Autowired
    private ManutencaoService manutencaoService;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------
    // Indicadores e consistência de contagem
    // ------------------------------------------------------------------

    @Test
    void indicadoresDevemCorresponderAsConsultasDoBanco() {
        limpar();

        LocalDate hoje = LocalDate.now();
        Motorista motorista = criarMotorista("kpi.motorista", "Alfa Kpi", "111.111.111-11",
                "10000000001", "B", hoje.plusYears(3), "Ativo").getMotorista();
        Veiculo primeiro = criarVeiculo("MNTA001", motorista, null);
        Veiculo segundo = criarVeiculo("MNTA002", null, null);

        criarManutencao(primeiro, hoje, "Troca de óleo");
        criarManutencao(primeiro, hoje.withDayOfMonth(1), "Alinhamento");
        criarManutencao(segundo, hoje.minusMonths(2).withDayOfMonth(10), "Revisão preventiva");

        ManutencaoPaginaDTO resumo = manutencaoService.montarResumo(admin(), hoje);
        ManutencaoIndicadoresDTO indicadores = resumo.indicadores();

        assertThat(resumo.referencia()).isEqualTo(hoje);
        assertThat(resumo.inicioMes()).isEqualTo(hoje.withDayOfMonth(1));
        assertThat(resumo.mesesGrafico()).isEqualTo(12);
        assertThat(resumo.janelaRevisoesDias()).isEqualTo(ManutencaoService.JANELA_REVISAO_DIAS).isEqualTo(15);
        assertThat(resumo.escopoRestrito()).isFalse();
        assertThat(resumo.escopoDescricao()).isEqualTo("Frota completa");

        // Card de total: histórico completo, conferido contra a consulta do banco.
        assertThat(indicadores.totalRegistros())
                .isEqualTo(manutencaoRepository.contarEscopado(null))
                .isEqualTo(3);

        // Card do mês: de inicioMes até a referência (ambos inclusos).
        assertThat(indicadores.registrosNoMes())
                .isEqualTo(manutencaoRepository.contarNoIntervalo(hoje.withDayOfMonth(1), hoje, null))
                .isEqualTo(2);

        // O histórico não é contaminado pelo painel de revisões (nenhum veículo tem revisão).
        assertThat(indicadores.revisoesAtrasadas()).isZero();
        assertThat(indicadores.revisoesPrevistas()).isZero();

        // Ranking: 2 registros no primeiro veículo, 1 no segundo.
        assertThat(resumo.rankingVeiculos())
                .extracting(ManutencaoRankingVeiculoDTO::placa, ManutencaoRankingVeiculoDTO::total)
                .containsExactly(tuple("MNTA001", 2L), tuple("MNTA002", 1L));

        assertThat(resumo.exibirGraficoMensal()).isTrue();
    }

    @Test
    void deveRespeitarJanelaDeDozeMesesNoGrafico() {
        limpar();

        LocalDate hoje = LocalDate.now();
        Veiculo veiculo = criarVeiculo("MNTJ001", null, null);

        LocalDate inicioJanela = YearMonth.from(hoje).minusMonths(ManutencaoService.MESES_GRAFICO - 1).atDay(1);
        // Limite inferior da janela entra; o dia imediatamente anterior (13º mês) fica fora.
        criarManutencao(veiculo, inicioJanela, "No limite da janela");
        criarManutencao(veiculo, inicioJanela.minusDays(1), "Fora da janela");

        ManutencaoPaginaDTO resumo = manutencaoService.montarResumo(admin(), hoje);

        assertThat(resumo.manutencoesPorMes()).hasSize(12);
        assertThat(resumo.manutencoesPorMes().stream().mapToLong(ManutencaoMesDTO::total).sum())
                .isEqualTo(1);
        // O card do histórico continua contando tudo, inclusive o registro fora do gráfico.
        assertThat(resumo.indicadores().totalRegistros()).isEqualTo(2);
    }

    @Test
    void deveMontarSerieMensalContinuaIncluindoMesesSemRegistro() {
        limpar();

        LocalDate hoje = LocalDate.now();
        YearMonth mesAtual = YearMonth.from(hoje);
        Veiculo veiculo = criarVeiculo("MNTB001", null, null);

        criarManutencao(veiculo, hoje, "Serviço atual 1");
        criarManutencao(veiculo, hoje, "Serviço atual 2");
        criarManutencao(veiculo, mesAtual.minusMonths(11).atDay(10), "Serviço antigo");
        criarManutencao(veiculo, mesAtual.minusMonths(12).atDay(10), "Fora da janela");

        ManutencaoPaginaDTO resumo = manutencaoService.montarResumo(admin(), hoje);
        List<ManutencaoMesDTO> serie = resumo.manutencoesPorMes();

        // Eixo temporal contínuo: do mês -11 até o mês corrente, sempre com 12 posições.
        assertThat(serie).hasSize(12);
        assertThat(serie.get(0).competencia()).isEqualTo(mesAtual.minusMonths(11).toString());
        assertThat(serie.get(11).competencia()).isEqualTo(mesAtual.toString());
        assertThat(serie).extracting(ManutencaoMesDTO::competencia)
                .doesNotContain(mesAtual.minusMonths(12).toString());

        // Intervalo exato de cada bucket (usado pelos links "mês → registros").
        assertThat(serie.get(0).inicio()).isEqualTo(mesAtual.minusMonths(11).atDay(1));
        assertThat(serie.get(0).fim()).isEqualTo(mesAtual.minusMonths(11).atEndOfMonth());

        // Totais: 1 no mês mais antigo da janela, 2 no mês corrente.
        assertThat(serie.get(0).total()).isEqualTo(1);
        assertThat(serie.get(11).total()).isEqualTo(2);

        // Meses intermediários são zero, não ausentes.
        assertThat(serie.subList(1, 11)).allSatisfy(mes -> assertThat(mes.total()).isZero());

        // 2 (mês corrente) + 1 (mês -11); o 13º mês fica fora do gráfico mas dentro do total.
        assertThat(serie.stream().mapToLong(ManutencaoMesDTO::total).sum()).isEqualTo(3);
        assertThat(resumo.indicadores().totalRegistros()).isEqualTo(4);
    }

    // ------------------------------------------------------------------
    // Ordenação e filtros do histórico
    // ------------------------------------------------------------------

    @Test
    void deveOrdenarHistoricoDaMaisRecenteParaAMaisAntiga() {
        limpar();

        LocalDate hoje = LocalDate.now();
        Veiculo veiculo = criarVeiculo("MNTC001", null, null);

        criarManutencao(veiculo, hoje.minusDays(30), "Serviço A");
        criarManutencao(veiculo, hoje, "Serviço B");
        criarManutencao(veiculo, hoje.minusDays(10), "Serviço C");
        // Dois registros na mesma data: o desempate é pelo id (o mais recente primeiro).
        criarManutencao(veiculo, hoje.minusDays(5), "Serviço D");
        criarManutencao(veiculo, hoje.minusDays(5), "Serviço E");

        List<Manutencao> historico = manutencaoService.listar(admin(), semFiltro());

        assertThat(historico)
                .extracting(Manutencao::getServicoRealizado)
                .containsExactly("Serviço B", "Serviço E", "Serviço D", "Serviço C", "Serviço A");
        assertThat(historico)
                .extracting(Manutencao::getData)
                .containsExactly(hoje, hoje.minusDays(5), hoje.minusDays(5), hoje.minusDays(10),
                        hoje.minusDays(30));
    }

    @Test
    void deveFiltrarHistoricoPorVeiculoServicoEPeriodoComLimitesInclusivos() {
        limpar();

        LocalDate hoje = LocalDate.now();
        Veiculo primeiro = criarVeiculo("MNTD001", null, null);
        Veiculo segundo = criarVeiculo("MNTD002", null, null);

        criarManutencao(primeiro, hoje.minusDays(20), "Troca de óleo");
        criarManutencao(primeiro, hoje.minusDays(10), "Troca de pastilhas de freio");
        criarManutencao(segundo, hoje.minusDays(5), "Alinhamento");
        criarManutencao(segundo, hoje, "Revisão preventiva");

        // Por veículo.
        assertThat(servicos(new ManutencaoFiltro(primeiro.getId(), null, null, null)))
                .containsExactly("Troca de pastilhas de freio", "Troca de óleo");

        // Por serviço: busca textual, insensível a caixa e a espaços laterais.
        assertThat(servicos(new ManutencaoFiltro(null, "  TROCA  ", null, null)))
                .containsExactly("Troca de pastilhas de freio", "Troca de óleo");
        assertThat(servicos(new ManutencaoFiltro(null, "freio", null, null)))
                .containsExactly("Troca de pastilhas de freio");
        assertThat(servicos(new ManutencaoFiltro(null, "inexistente", null, null))).isEmpty();

        // Período: os dois limites são inclusivos.
        assertThat(servicos(new ManutencaoFiltro(null, null, hoje.minusDays(20), hoje.minusDays(10))))
                .containsExactly("Troca de pastilhas de freio", "Troca de óleo");
        assertThat(servicos(new ManutencaoFiltro(null, null, hoje.minusDays(9), null)))
                .containsExactly("Revisão preventiva", "Alinhamento");

        // Filtros combinados.
        assertThat(servicos(new ManutencaoFiltro(segundo.getId(), null, null, hoje.minusDays(4))))
                .containsExactly("Alinhamento");
    }

    @Test
    void deveLimitarRankingACincoVeiculosComDesempatePorPlaca() {
        limpar();

        LocalDate hoje = LocalDate.now();
        Veiculo maior = criarVeiculo("MNTI001", null, null);
        Veiculo b = criarVeiculo("MNTI002", null, null);
        Veiculo c = criarVeiculo("MNTI003", null, null);
        Veiculo d = criarVeiculo("MNTI004", null, null);
        Veiculo e = criarVeiculo("MNTI005", null, null);
        Veiculo fora = criarVeiculo("MNTI006", null, null);

        for (int i = 0; i < 3; i++) {
            criarManutencao(maior, hoje, "Serviço " + i);
        }
        for (Veiculo veiculo : List.of(b, c, d, e)) {
            criarManutencao(veiculo, hoje.minusDays(1), "Serviço 1");
            criarManutencao(veiculo, hoje.minusDays(2), "Serviço 2");
        }
        criarManutencao(fora, hoje.minusDays(2), "Serviço");

        ManutencaoPaginaDTO resumo = manutencaoService.montarResumo(admin(), hoje);

        // Limite de 5 posições; empate desfeito por placa ascendente; o sexto fica de fora.
        assertThat(resumo.rankingVeiculos())
                .extracting(ManutencaoRankingVeiculoDTO::placa, ManutencaoRankingVeiculoDTO::total)
                .containsExactly(
                        tuple("MNTI001", 3L),
                        tuple("MNTI002", 2L),
                        tuple("MNTI003", 2L),
                        tuple("MNTI004", 2L),
                        tuple("MNTI005", 2L));
    }

    // ------------------------------------------------------------------
    // Painel de revisões (previsão) x histórico (realizado)
    // ------------------------------------------------------------------

    @Test
    void deveSepararRevisoesAtrasadasDePrevistasNaJanelaDe15Dias() {
        limpar();

        LocalDate hoje = LocalDate.now();
        criarVeiculo("MNTD001", null, hoje.minusDays(1));
        criarVeiculo("MNTD002", null, hoje);
        criarVeiculo("MNTD003", null, hoje.plusDays(15));
        criarVeiculo("MNTD004", null, hoje.plusDays(16));
        criarVeiculo("MNTD005", null, null);

        ManutencaoPaginaDTO resumo = manutencaoService.montarResumo(admin(), hoje);

        assertThat(resumo.janelaRevisoesDias()).isEqualTo(15);

        // Atrasadas: data prevista estritamente anterior à referência.
        assertThat(resumo.revisoesAtrasadas())
                .extracting(RevisaoVeiculoDTO::placa, RevisaoVeiculoDTO::situacao,
                        RevisaoVeiculoDTO::diasRestantes)
                .containsExactly(tuple("MNTD001", "Vencida", -1L));

        // Previstas: da referência até referência + 15 dias (ambos inclusos).
        assertThat(resumo.revisoesPrevistas())
                .extracting(RevisaoVeiculoDTO::placa, RevisaoVeiculoDTO::situacao,
                        RevisaoVeiculoDTO::diasRestantes)
                .containsExactly(tuple("MNTD002", "Próxima", 0L), tuple("MNTD003", "Próxima", 15L));

        assertThat(resumo.indicadores().revisoesAtrasadas()).isEqualTo(1);
        assertThat(resumo.indicadores().revisoesPrevistas()).isEqualTo(2);

        // Fora da janela (16 dias) e sem revisão agendada não entram em nenhuma série.
        assertThat(resumo.revisoesAtrasadas())
                .extracting(RevisaoVeiculoDTO::placa)
                .doesNotContain("MNTD004", "MNTD005");
        assertThat(resumo.revisoesPrevistas())
                .extracting(RevisaoVeiculoDTO::placa)
                .doesNotContain("MNTD004", "MNTD005");

        // A previsão não contamina o histórico — nem cartões, nem gráfico.
        assertThat(resumo.indicadores().totalRegistros()).isZero();
        assertThat(resumo.indicadores().registrosNoMes()).isZero();
        assertThat(resumo.manutencoesPorMes()).hasSize(12)
                .allSatisfy(mes -> assertThat(mes.total()).isZero());
        assertThat(resumo.exibirGraficoMensal()).isFalse();
    }

    // ------------------------------------------------------------------
    // Escopo por perfil
    // ------------------------------------------------------------------

    @Test
    void deveRestringirEscopoDoMotoristaSemRestringirOperadorNemAdmin() {
        limpar();

        LocalDate hoje = LocalDate.now();
        Usuario dono = criarMotorista("escopo.dono", "Alfa Dono", "111.111.111-11",
                "10000000001", "B", hoje.plusYears(3), "Ativo");
        Usuario outro = criarMotorista("escopo.outro", "Bravo Outro", "222.222.222-22",
                "10000000002", "B", hoje.plusYears(3), "Ativo");
        Veiculo meu = criarVeiculo("MNTE001", dono.getMotorista(), null);
        Veiculo alheio = criarVeiculo("MNTE002", outro.getMotorista(), null);

        criarManutencao(meu, hoje, "Serviço meu");
        criarManutencao(meu, hoje.minusDays(20), "Serviço meu antigo");
        criarManutencao(alheio, hoje, "Serviço alheio");

        // MOTORISTA: apenas os veículos vinculados ao próprio cadastro.
        ManutencaoPaginaDTO resumoMotorista = manutencaoService.montarResumo(dono, hoje);
        assertThat(resumoMotorista.escopoRestrito()).isTrue();
        assertThat(resumoMotorista.indicadores().totalRegistros())
                .isEqualTo(manutencaoRepository.contarEscopado(dono.getMotorista().getId()))
                .isEqualTo(2);
        assertThat(resumoMotorista.manutencoesPorMes().stream().mapToLong(ManutencaoMesDTO::total).sum())
                .isEqualTo(2);
        assertThat(resumoMotorista.rankingVeiculos())
                .extracting(ManutencaoRankingVeiculoDTO::placa, ManutencaoRankingVeiculoDTO::total)
                .containsExactly(tuple("MNTE001", 2L));
        assertThat(servicos(dono, semFiltro())).containsExactly("Serviço meu", "Serviço meu antigo");

        // OPERADOR e ADMIN: frota completa.
        assertThat(manutencaoService.montarResumo(operador(), hoje).escopoRestrito()).isFalse();
        assertThat(servicos(operador(), semFiltro()))
                .containsExactlyInAnyOrder("Serviço meu", "Serviço meu antigo", "Serviço alheio");
        assertThat(manutencaoService.listar(admin(), semFiltro())).hasSize(3);

        // MOTORISTA sem cadastro de motorista vinculado não possui histórico visível.
        Usuario semCadastro = new Usuario(null, "sem.cadastro", "Sem Cadastro",
                "sem.cadastro@fleetops.com", "123456", "MOTORISTA", true);
        ManutencaoPaginaDTO vazio = manutencaoService.montarResumo(semCadastro, hoje);
        assertThat(vazio.escopoRestrito()).isTrue();
        assertThat(vazio.indicadores().totalRegistros()).isZero();
        assertThat(vazio.rankingVeiculos()).isEmpty();
        assertThat(vazio.manutencoesPorMes()).hasSize(12);
        assertThat(manutencaoService.listar(semCadastro, semFiltro())).isEmpty();
    }

    // ------------------------------------------------------------------
    // Renderização
    // ------------------------------------------------------------------

    @Test
    void deveRenderizarIndicadoresGraficoRankingPainelEFiltros() throws Exception {
        limpar();

        LocalDate hoje = LocalDate.now();
        Motorista motorista = criarMotorista("render.motorista", "Alfa Render", "111.111.111-11",
                "10000000001", "B", hoje.plusYears(3), "Ativo").getMotorista();
        Veiculo prevista = criarVeiculo("MNTG001", motorista, hoje.plusDays(5));
        criarVeiculo("MNTG002", motorista, hoje.minusDays(3));

        criarManutencao(prevista, hoje, "Revisão preventiva");
        criarManutencao(prevista, hoje.minusMonths(1).withDayOfMonth(15), "Troca de óleo");

        mockMvc.perform(get("/manutencoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencao/list"))
                // Cards (histórico x previsão em cartões distintos)
                .andExpect(content().string(containsString("Registros no histórico")))
                .andExpect(content().string(containsString("Realizadas no mês")))
                .andExpect(content().string(containsString("Revisões atrasadas")))
                .andExpect(content().string(containsString("Revisões previstas")))
                // Gráfico mensal com lista acessível e objeto global
                .andExpect(content().string(containsString("Manutenções realizadas por mês")))
                .andExpect(content().string(containsString("id=\"chart-manutencoes-mes\"")))
                .andExpect(content().string(containsString("chart-legend--meses")))
                .andExpect(content().string(containsString("window.FLEET_MANUTENCOES")))
                // Ranking com links para os registros e para o veículo
                .andExpect(content().string(containsString("Veículos com mais registros de manutenção")))
                .andExpect(content().string(containsString("data-table--ranking")))
                .andExpect(content().string(containsString("Ver registros")))
                // Painel de revisões: duas séries separadas, atrasadas e previstas
                .andExpect(content().string(containsString("Revisões previstas no cadastro")))
                .andExpect(content().string(containsString("revision-block--overdue")))
                .andExpect(content().string(containsString("revision-block--soon")))
                .andExpect(content().string(containsString("MNTG002")))
                .andExpect(content().string(containsString("é uma série distinta")))
                // Histórico, filtros e formatação de milhar
                .andExpect(content().string(containsString("data-table--historico")))
                .andExpect(content().string(containsString("id=\"veiculoId\"")))
                .andExpect(content().string(containsString("id=\"servico\"")))
                .andExpect(content().string(containsString("id=\"de\"")))
                .andExpect(content().string(containsString("id=\"ate\"")))
                .andExpect(content().string(containsString("Revisão preventiva")))
                .andExpect(content().string(containsString("Troca de óleo")))
                .andExpect(content().string(containsString("1.000 km")));
    }

    @Test
    void deveExibirEstadosVaziosQuandoNaoHaHistorico() throws Exception {
        limpar();

        LocalDate hoje = LocalDate.now();
        criarVeiculo("MNTF001", null, hoje.plusDays(3));

        ManutencaoPaginaDTO resumo = manutencaoService.montarResumo(admin(), hoje);
        assertThat(resumo.indicadores().totalRegistros()).isZero();
        assertThat(resumo.indicadores().registrosNoMes()).isZero();
        assertThat(resumo.rankingVeiculos()).isEmpty();
        assertThat(resumo.exibirGraficoMensal()).isFalse();
        assertThat(resumo.manutencoesPorMes()).hasSize(12)
                .allSatisfy(mes -> assertThat(mes.total()).isZero());

        mockMvc.perform(get("/manutencoes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sem manutenções nos últimos meses")))
                .andExpect(content().string(containsString("Sem registros para ranquear")))
                .andExpect(content().string(containsString("Nenhuma manutenção encontrada")))
                .andExpect(content().string(
                        containsString("Não há manutenções registradas no escopo exibido.")))
                .andExpect(content().string(not(containsString("id=\"chart-manutencoes-mes\""))));

        // Filtro sem correspondência: mensagem específica e chip do filtro aplicado.
        mockMvc.perform(get("/manutencoes").param("servico", "ZZZZ"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Nenhum registro corresponde aos filtros atuais.")))
                .andExpect(content().string(containsString("Filtros aplicados")));
    }

    // ------------------------------------------------------------------
    // CRUD e permissões preservadas
    // ------------------------------------------------------------------

    @Test
    void devePreservarCrudEPermissoesDeManutencao() throws Exception {
        limpar();

        LocalDate hoje = LocalDate.now();
        Usuario operador = criarOperador("perm.operador", "Operador Perm");
        Usuario dono = criarMotorista("perm.dono", "Alfa Perm", "111.111.111-11",
                "10000000001", "B", hoje.plusYears(3), "Ativo");
        Usuario terceiro = criarMotorista("perm.terceiro", "Bravo Perm", "222.222.222-22",
                "10000000002", "B", hoje.plusYears(3), "Ativo");
        Veiculo meu = criarVeiculo("MNTH001", dono.getMotorista(), null);
        Veiculo alheio = criarVeiculo("MNTH002", terceiro.getMotorista(), null);
        Manutencao minha = criarManutencao(meu, hoje, "Serviço meu");
        Manutencao alheia = criarManutencao(alheio, hoje, "Serviço alheio");

        // OPERADOR não registra manutenções (permissão inalterada).
        mockMvc.perform(get("/manutencoes/novo").sessionAttr("usuarioLogado", operador))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes"))
                .andExpect(flash().attribute("erro",
                        "Acesso negado: você não possui permissão para registrar manutenções."));

        // ADMIN acessa o formulário de cadastro.
        mockMvc.perform(get("/manutencoes/novo").sessionAttr("usuarioLogado", admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencao/novo"));

        // MOTORISTA edita a manutenção do próprio veículo...
        mockMvc.perform(get("/manutencoes/editar/" + minha.getId())
                        .sessionAttr("usuarioLogado", dono))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencao/editar"));

        // ...mas não a de terceiros.
        mockMvc.perform(get("/manutencoes/editar/" + alheia.getId())
                        .sessionAttr("usuarioLogado", dono))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes"))
                .andExpect(flash().attribute("erro",
                        "Acesso negado: você só pode editar manutenções dos seus próprios veículos."));

        // ADMIN exclui e o registro do escopo do motorista permanece.
        mockMvc.perform(post("/manutencoes/excluir/" + alheia.getId())
                        .sessionAttr("usuarioLogado", admin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes"))
                .andExpect(flash().attribute("sucesso", "Manutenção excluída com sucesso!"));

        assertThat(manutencaoRepository.findById(alheia.getId())).isEmpty();
        assertThat(manutencaoRepository.findById(minha.getId())).isPresent();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Usuario admin() {
        return new Usuario(null, "admin.pagina", "Administrador Pagina",
                "admin.pagina@fleetops.com", "123456", "ADMIN", true);
    }

    private Usuario operador() {
        return new Usuario(null, "operador.pagina", "Operador Pagina",
                "operador.pagina@fleetops.com", "123456", "OPERADOR", true);
    }

    private Usuario criarOperador(String username, String nome) {
        usuarioService.salvar(new Usuario(null, username, nome, username + "@fleetops.com",
                "123456", "OPERADOR", true));
        return usuarioService.autenticar(username, "123456").orElseThrow();
    }

    private ManutencaoFiltro semFiltro() {
        return new ManutencaoFiltro(null, null, null, null);
    }

    private List<String> servicos(ManutencaoFiltro filtro) {
        return servicos(admin(), filtro);
    }

    /** Serviços devolvidos pelo filtro (na ordem do histórico: mais recente primeiro). */
    private List<String> servicos(Usuario logado, ManutencaoFiltro filtro) {
        return manutencaoService.listar(logado, filtro).stream()
                .map(Manutencao::getServicoRealizado)
                .collect(Collectors.toList());
    }

    /** Remove histórico, frota e equipe para que as contagens do teste sejam determinísticas. */
    private void limpar() {
        manutencaoRepository.deleteAllInBatch();
        veiculoRepository.deleteAllInBatch();
        motoristaRepository.deleteAllInBatch();
    }

    private Usuario criarMotorista(String username, String nome, String cpf, String numeroCnh,
                                   String categoria, LocalDate validadeCnh, String status) {
        Usuario usuario = new Usuario(null, username, nome, username + "@fleetops.com", "123456",
                "OPERADOR", true);
        usuarioService.salvar(usuario);

        Motorista motorista = new Motorista(null, nome, cpf, numeroCnh, categoria, validadeCnh, null);
        motorista.setStatus(status);
        motorista.setUsuario(usuario);
        motoristaService.salvar(motorista);

        // Mantém o vínculo bidirecional em memória: em um único contexto de persistência
        // (teste transacional) o lado inverso Usuario.motorista não é preenchido pelo JPA.
        usuario.setMotorista(motorista);

        // Recarrega pelo login para obter o perfil MOTORISTA e o vínculo já resolvidos.
        return usuarioService.autenticar(username, "123456").orElseThrow();
    }

    private Veiculo criarVeiculo(String placa, Motorista motorista, LocalDate proximaRevisao) {
        Veiculo veiculo = new Veiculo(null, placa, "Modelo " + placa, 2024, 1000L, "Disponível",
                null, proximaRevisao, null, null, "Em dia");
        veiculo.setMotorista(motorista);
        return veiculoRepository.saveAndFlush(veiculo);
    }

    private Manutencao criarManutencao(Veiculo veiculo, LocalDate data, String servico) {
        return manutencaoRepository.saveAndFlush(new Manutencao(null, data, 1000L, servico,
                "Oficina FrotaOps", null, veiculo));
    }
}
