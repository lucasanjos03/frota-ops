package br.edu.cesmac.frota_ops.motorista;

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
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.cesmac.frota_ops.manutencao.ManutencaoRepository;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoRepository;

/**
 * Testes da página de motoristas (visão híbrida de análise e gestão).
 *
 * <p>A validação exigida cobre a classificação da CNH com data de referência controlada
 * (vencimento no dia, dentro de 30 dias e no passado), o indicador e o filtro de vínculo,
 * a cardinalidade real 1:N entre motorista e veículo, o mascaramento do CPF e a
 * preservação das permissões existentes.</p>
 *
 * <p>As asserções de situação da CNH usam {@code motoristaService.listar(filtro, referencia)}
 * e {@code motoristaService.montarResumo(referencia)} — a data é sempre parâmetro, nunca
 * {@code LocalDate.now()} interno —, de modo que o teste não depende do dia em que roda.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MotoristaPaginaTests {

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------
    // Indicadores e painel de documentos
    // ------------------------------------------------------------------

    @Test
    void indicadoresDevemCorresponderAsConsultasDoBanco() {
        limpar();

        LocalDate hoje = LocalDate.now();
        criarMotorista("kpi.vencida", "Alfa Vencida", "111.111.111-11", "10000000001", "B",
                hoje.minusDays(10), "Ativo");
        criarMotorista("kpi.hoje", "Bravo Hoje", "222.222.222-22", "10000000002", "C",
                hoje, "Ativo");
        criarMotorista("kpi.janela", "Charlie Janela", "333.333.333-33", "10000000003", "C",
                hoje.plusDays(30), "Inativo");
        Usuario valido = criarMotorista("kpi.valida", "Delta Valida", "444.444.444-44", "10000000004", "D",
                hoje.plusDays(45), "Ativo");
        // Apenas o motorista de CNH válida tem veículo atribuído.
        criarVeiculo("KPIA001", valido.getMotorista());

        MotoristaPaginaDTO resumo = motoristaService.montarResumo(hoje);
        MotoristaIndicadoresDTO indicadores = resumo.indicadores();

        assertThat(resumo.janelaCnhDias()).isEqualTo(MotoristaService.JANELA_CNH_DIAS).isEqualTo(30);
        assertThat(resumo.referencia()).isEqualTo(hoje);

        // Cards de total / ativos / inativos.
        assertThat(indicadores.total()).isEqualTo(motoristaRepository.count()).isEqualTo(4);
        assertThat(indicadores.ativos()).isEqualTo(motoristaRepository.countByStatus("Ativo")).isEqualTo(3);
        assertThat(indicadores.inativos()).isEqualTo(motoristaRepository.countByStatus("Inativo")).isEqualTo(1);
        assertThat(indicadores.ativos() + indicadores.inativos()).isEqualTo(indicadores.total());

        // CNHs vencidas (estritamente antes) x a vencer (janela [hoje, hoje+30], dia inclusos).
        assertThat(indicadores.cnhVencidas())
                .isEqualTo(motoristaRepository.countByValidadeCnhBefore(hoje)).isEqualTo(1);
        assertThat(indicadores.cnhProximas())
                .isEqualTo(motoristaRepository.countByValidadeCnhBetween(hoje, hoje.plusDays(30)))
                .isEqualTo(2);

        // Sem veículo: os três sem vínculo na tabela veiculo.
        assertThat(indicadores.semVeiculo())
                .isEqualTo(motoristaRepository.contarSemVeiculo()).isEqualTo(3);

        // Distribuição por categoria (uma única categoria por motorista).
        assertThat(resumo.porCategoria())
                .extracting(MotoristaCategoriaDTO::categoria, MotoristaCategoriaDTO::total)
                .containsExactly(tuple("B", 1L), tuple("C", 2L), tuple("D", 1L));

        // Painel de documentos: vencidas primeiro (ordem por validade ascendente).
        assertThat(resumo.documentosAtencao())
                .extracting(MotoristaDocumentoDTO::nome, MotoristaDocumentoDTO::situacao)
                .containsExactly(
                        tuple("Alfa Vencida", "Vencida"),
                        tuple("Bravo Hoje", "A vencer"),
                        tuple("Charlie Janela", "A vencer"));
        assertThat(resumo.documentosAtencao())
                .extracting(MotoristaDocumentoDTO::diasRestantes)
                .containsExactly(-10L, 0L, 30L);
    }

    @Test
    void deveDistinguirCnhVencidaDeProximaEValidaNaDataDeReferencia() {
        limpar();

        LocalDate hoje = LocalDate.now();
        criarMotorista("cnh.vencida", "Alfa Vencida", "111.111.111-11", "10000000001", "B",
                hoje.minusDays(1), "Ativo");
        criarMotorista("cnh.hoje", "Bravo Hoje", "222.222.222-22", "10000000002", "B",
                hoje, "Ativo");
        criarMotorista("cnh.limite", "Charlie Limite", "333.333.333-33", "10000000003", "B",
                hoje.plusDays(30), "Ativo");
        criarMotorista("cnh.valida", "Delta Valida", "444.444.444-44", "10000000004", "B",
                hoje.plusDays(31), "Ativo");

        // Fronteira inferior: vencimento no próprio dia ainda é "a vencer".
        assertThat(nomes(filtro(null, null, null, "vencida", null), hoje)).containsExactly("Alfa Vencida");
        assertThat(nomes(filtro(null, null, null, "proxima", null), hoje))
                .containsExactly("Bravo Hoje", "Charlie Limite");
        assertThat(nomes(filtro(null, null, null, "valida", null), hoje)).containsExactly("Delta Valida");

        // A data de referência desloca a classificação sem tocar no cadastro: ao avançar
        // 31 dias, as três primeiras validades ficam no passado e a quarta entra na janela.
        assertThat(nomes(filtro(null, null, null, "vencida", null), hoje.plusDays(31)))
                .containsExactly("Alfa Vencida", "Bravo Hoje", "Charlie Limite");
        assertThat(nomes(filtro(null, null, null, "proxima", null), hoje.plusDays(31)))
                .containsExactly("Delta Valida");
        assertThat(nomes(filtro(null, null, null, "valida", null), hoje.plusDays(31))).isEmpty();
    }

    // ------------------------------------------------------------------
    // Busca, filtros e vínculo
    // ------------------------------------------------------------------

    @Test
    void deveFiltrarPorStatusCategoriaBuscaEVinculo() {
        limpar();

        LocalDate hoje = LocalDate.now();
        Usuario comVeiculo = criarMotorista("filtro.com", "Alfa Com Veiculo", "111.111.111-11",
                "10000000001", "B", hoje.plusYears(3), "Ativo");
        criarMotorista("filtro.sem", "Bravo Sem Veiculo", "222.222.222-22", "10000000002", "C",
                hoje.plusYears(3), "Inativo");
        criarVeiculo("FILM001", comVeiculo.getMotorista());

        assertThat(nomes(filtro(null, "Ativo", null, null, null), hoje)).containsExactly("Alfa Com Veiculo");
        assertThat(nomes(filtro(null, "Inativo", null, null, null), hoje)).containsExactly("Bravo Sem Veiculo");
        assertThat(nomes(filtro(null, null, "C", null, null), hoje)).containsExactly("Bravo Sem Veiculo");
        assertThat(nomes(filtro(null, null, null, null, "com"), hoje)).containsExactly("Alfa Com Veiculo");
        assertThat(nomes(filtro(null, null, null, null, "sem"), hoje)).containsExactly("Bravo Sem Veiculo");

        // Busca por nome, CPF, número da CNH e username — insensível a caixa e a espaços.
        assertThat(nomes(filtro("  bravo  ", null, null, null, null), hoje)).containsExactly("Bravo Sem Veiculo");
        assertThat(nomes(filtro("222.222.222-22", null, null, null, null), hoje)).containsExactly("Bravo Sem Veiculo");
        assertThat(nomes(filtro("10000000001", null, null, null, null), hoje)).containsExactly("Alfa Com Veiculo");
        assertThat(nomes(filtro("FILTRO.SEM", null, null, null, null), hoje)).containsExactly("Bravo Sem Veiculo");
        assertThat(nomes(filtro("inexistente", null, null, null, null), hoje)).isEmpty();

        // Filtros combinados.
        assertThat(nomes(filtro(null, "Inativo", "C", null, "sem"), hoje))
                .containsExactly("Bravo Sem Veiculo");
    }

    @Test
    void deveListarCategoriasPresentesNoCadastro() {
        limpar();

        LocalDate hoje = LocalDate.now();
        criarMotorista("cat.b", "Alfa B", "111.111.111-11", "10000000001", "B", hoje.plusYears(3), "Ativo");
        criarMotorista("cat.d", "Bravo D", "222.222.222-22", "10000000002", "D", hoje.plusYears(3), "Ativo");

        assertThat(motoristaService.listarCategorias()).containsExactly("B", "D");
    }

    // ------------------------------------------------------------------
    // Cardinalidade motorista → veículos
    // ------------------------------------------------------------------

    @Test
    void deveListarMaisDeUmVeiculoAtribuidoAoMesmoMotorista() throws Exception {
        limpar();

        Usuario motorista = criarMotorista("cardinalidade", "Alfa Cardinalidade", "111.111.111-11",
                "10000000001", "B", LocalDate.now().plusYears(3), "Ativo");
        criarVeiculo("CARD001", motorista.getMotorista());
        criarVeiculo("CARD002", motorista.getMotorista());
        criarMotorista("cardinalidade.outro", "Bravo Sem Veiculo", "222.222.222-22", "10000000002", "B",
                LocalDate.now().plusYears(3), "Ativo");

        // A relação real é 1:N — um motorista responde por N veículos.
        assertThat(motoristaService.listarVeiculosDoMotorista(motorista.getMotorista().getId()))
                .extracting(Veiculo::getPlaca)
                .containsExactly("CARD001", "CARD002");

        mockMvc.perform(get("/motoristas/detalhes/" + motorista.getMotorista().getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/detalhes"))
                .andExpect(content().string(containsString("CARD001")))
                .andExpect(content().string(containsString("CARD002")))
                .andExpect(content().string(containsString("Alfa Cardinalidade")));

        // O motorista sem veículo exibe o estado vazio da seção.
        Long semVeiculoId = motoristaRepository.findAll().stream()
                .filter(m -> m.getNome().equals("Bravo Sem Veiculo"))
                .map(Motorista::getId)
                .findFirst()
                .orElseThrow();
        mockMvc.perform(get("/motoristas/detalhes/" + semVeiculoId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum veículo atribuído")))
                .andExpect(content().string(containsString("implicam aptidão operacional")));
    }

    // ------------------------------------------------------------------
    // Renderização da listagem
    // ------------------------------------------------------------------

    @Test
    void deveRenderizarIndicadoresGraficoPainelEFiltrosPreservandoATabela() throws Exception {
        limpar();

        Usuario motorista = criarMotorista("carlos.teste", "Carlos Teste", "111.444.777-35",
                "10000000001", "B", LocalDate.now().minusDays(5), "Ativo");
        criarVeiculo("REND001", motorista.getMotorista());

        mockMvc.perform(get("/motoristas"))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/list"))
                // Cards da equipe
                .andExpect(content().string(containsString("Total de motoristas")))
                .andExpect(content().string(containsString(">Ativos<")))
                .andExpect(content().string(containsString(">Inativos<")))
                .andExpect(content().string(containsString("CNHs vencidas")))
                .andExpect(content().string(containsString("CNHs a vencer")))
                .andExpect(content().string(containsString("Sem veículo atribuído")))
                // Gráfico por categoria com a lista acessível
                .andExpect(content().string(containsString("Motoristas por categoria de CNH")))
                .andExpect(content().string(containsString("chart-categoria")))
                .andExpect(content().string(containsString("window.FLEET_MOTORISTAS")))
                .andExpect(content().string(containsString("Categoria B")))
                // Painel de documentos que exigem atenção
                .andExpect(content().string(containsString("Documentos que exigem atenção")))
                .andExpect(content().string(containsString("vencida há 5 dia(s)")))
                .andExpect(content().string(containsString("alert-item--alta")))
                .andExpect(content().string(containsString("Documento dentro da validade não garante aptidão operacional")))
                // Filtros
                .andExpect(content().string(containsString("id=\"status\"")))
                .andExpect(content().string(containsString("id=\"categoria\"")))
                .andExpect(content().string(containsString("id=\"cnh\"")))
                .andExpect(content().string(containsString("id=\"vinculo\"")))
                // A tabela permanece a base da listagem
                .andExpect(content().string(containsString("data-table--equipe")))
                .andExpect(content().string(containsString("Carlos Teste")))
                // Proteção de dados: o CPF nunca aparece por inteiro.
                .andExpect(content().string(containsString("111.***.***-**")))
                .andExpect(content().string(not(containsString("111.444.777-35"))))
                .andExpect(content().string(containsString("Dados pessoais protegidos")));
    }

    @Test
    void deveRenderizarDetalhesComSituacaoDaCnh() throws Exception {
        limpar();

        LocalDate hoje = LocalDate.now();
        Long vencidaId = criarMotorista("detalhe.vencida", "Alfa Detalhe", "111.111.111-11",
                "10000000001", "B", hoje.minusDays(2), "Ativo").getMotorista().getId();
        Long validaId = criarMotorista("detalhe.valida", "Bravo Detalhe", "222.222.222-22",
                "10000000002", "B", hoje.plusDays(60), "Ativo").getMotorista().getId();

        mockMvc.perform(get("/motoristas/detalhes/" + vencidaId))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/detalhes"))
                .andExpect(content().string(containsString("Alfa Detalhe")))
                .andExpect(content().string(containsString("111.***.***-**")))
                .andExpect(content().string(containsString("status-danger")))
                .andExpect(content().string(containsString(">Vencida<")));

        mockMvc.perform(get("/motoristas/detalhes/" + validaId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("status-success")))
                .andExpect(content().string(containsString(">Válida<")));
    }

    @Test
    void deveExibirEstadosVaziosSemDadosESemCorrespondenciaDeFiltro() throws Exception {
        limpar();

        // Equipe vazia: sem tabela, sem canvas e sem itens no painel.
        mockMvc.perform(get("/motoristas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum motorista encontrado")))
                .andExpect(content().string(containsString("Não há motoristas cadastrados.")))
                .andExpect(content().string(containsString("Sem categorias registradas")))
                .andExpect(content().string(containsString("Nenhuma CNH exige atenção")))
                .andExpect(content().string(not(containsString("id=\"chart-categoria\""))));

        criarMotorista("unico", "Alfa Unico", "111.111.111-11", "10000000001", "B",
                LocalDate.now().plusYears(3), "Ativo");

        // Filtro sem correspondência: mensagem específica, chip do filtro e atalho para limpar.
        mockMvc.perform(get("/motoristas").param("q", "ZZZZZZ"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum motorista corresponde aos filtros atuais.")))
                .andExpect(content().string(containsString("Filtros aplicados")))
                .andExpect(content().string(containsString("Busca: ZZZZZZ")))
                .andExpect(content().string(not(containsString("Alfa Unico"))));
    }

    @Test
    void deveRedirecionarComErroQuandoMotoristaInexistente() throws Exception {
        mockMvc.perform(get("/motoristas/detalhes/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motoristas"))
                .andExpect(flash().attribute("erro", "Motorista não encontrado."));
    }

    // ------------------------------------------------------------------
    // Permissões preservadas
    // ------------------------------------------------------------------

    @Test
    void devePreservarDetalhesEdicaoEExclusaoAutorizada() throws Exception {
        limpar();

        Usuario admin = admin();
        Long id = criarMotorista("crud.admin", "Alfa Crud", "111.111.111-11", "10000000001", "B",
                LocalDate.now().plusYears(3), "Ativo").getMotorista().getId();

        mockMvc.perform(get("/motoristas/detalhes/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/detalhes"))
                .andExpect(content().string(containsString("Alfa Crud")));

        mockMvc.perform(get("/motoristas/editar/" + id).sessionAttr("usuarioLogado", admin))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/editar"));

        mockMvc.perform(post("/motoristas/excluir/" + id).sessionAttr("usuarioLogado", admin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motoristas"))
                .andExpect(flash().attribute("sucesso", "Motorista excluído com sucesso!"));

        assertThat(motoristaRepository.findById(id)).isEmpty();
    }

    @Test
    void deveBloquearCadastroExclusaoEEdicaoDeTerceirosParaMotorista() throws Exception {
        limpar();

        Usuario proprio = criarMotorista("perm.proprio", "Alfa Proprio", "111.111.111-11",
                "10000000001", "B", LocalDate.now().plusYears(3), "Ativo");
        Usuario terceiro = criarMotorista("perm.terceiro", "Bravo Terceiro", "222.222.222-22",
                "10000000002", "B", LocalDate.now().plusYears(3), "Ativo");

        // Cadastro e exclusão são exclusivos do ADMIN.
        mockMvc.perform(get("/motoristas/novo").sessionAttr("usuarioLogado", proprio))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motoristas"))
                .andExpect(flash().attribute("erro",
                        "Acesso negado: apenas administradores podem cadastrar novos motoristas."));

        mockMvc.perform(post("/motoristas/excluir/" + proprio.getMotorista().getId())
                .sessionAttr("usuarioLogado", proprio))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motoristas"))
                .andExpect(flash().attribute("erro",
                        "Acesso negado: apenas administradores podem excluir motoristas."));

        assertThat(motoristaRepository.findById(proprio.getMotorista().getId())).isPresent();

        // O MOTORISTA edita apenas o próprio cadastro.
        mockMvc.perform(get("/motoristas/editar/" + proprio.getMotorista().getId())
                .sessionAttr("usuarioLogado", proprio))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/editar"));

        mockMvc.perform(get("/motoristas/editar/" + terceiro.getMotorista().getId())
                .sessionAttr("usuarioLogado", proprio))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motoristas"))
                .andExpect(flash().attribute("erro",
                        "Acesso negado: você só tem permissão para editar seu próprio perfil de motorista."));
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Usuario admin() {
        return new Usuario(null, "admin.pagina", "Administrador Pagina", "admin.pagina@fleetops.com",
                "123456", "ADMIN", true);
    }

    private MotoristaFiltro filtro(String q, String status, String categoria, String cnh, String vinculo) {
        return new MotoristaFiltro(q, status, categoria, cnh, vinculo);
    }

    /** Nomes devolvidos pelo filtro na data de referência informada. */
    private List<String> nomes(MotoristaFiltro filtro, LocalDate referencia) {
        return motoristaService.listar(filtro, referencia).stream()
                .map(Motorista::getNome)
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

    private Veiculo criarVeiculo(String placa, Motorista motorista) {
        Veiculo veiculo = new Veiculo(null, placa, "Modelo " + placa, 2024, 1000L, "Disponível", null,
                null, null, null, "Em dia");
        veiculo.setMotorista(motorista);
        return veiculoRepository.saveAndFlush(veiculo);
    }
}
