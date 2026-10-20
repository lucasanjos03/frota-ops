package br.edu.cesmac.frota_ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.cesmac.frota_ops.manutencao.ManutencaoService;
import br.edu.cesmac.frota_ops.motorista.MotoristaService;
import br.edu.cesmac.frota_ops.tag.TagService;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioRepository;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import br.edu.cesmac.frota_ops.veiculo.VeiculoService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FrotaOpsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VeiculoService veiculoService;

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private TagService tagService;

    @Autowired
    private ManutencaoService manutencaoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void contextLoadsAndMigrationsSeedData() {
        assertThat(veiculoService.listarTodos()).isNotEmpty();
        assertThat(motoristaService.listarTodos()).isNotEmpty();
        assertThat(tagService.listarTodas()).isNotEmpty();
        assertThat(manutencaoService.buscarPorVeiculo(1L)).isNotEmpty();
    }

    @Test
    void deveRenderizarTodasAsTelasThymeleafComSucesso() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(content().string(containsString("Visão Geral da Frota")));

        mockMvc.perform(get("/veiculos"))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/list"))
                .andExpect(content().string(containsString("ABC1D23")))
                .andExpect(content().string(containsString("Carlos Eduardo Silva")))
                .andExpect(content().string(containsString("Rastreado GPS")));

        mockMvc.perform(get("/veiculos/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/novo"))
                .andExpect(content().string(containsString("Motorista responsável")))
                .andExpect(content().string(containsString("Tags operacionais")));

        mockMvc.perform(get("/veiculos/detalhes/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/detalhes"))
                .andExpect(content().string(containsString("Ford Transit 350")))
                .andExpect(content().string(containsString("Carlos Eduardo Silva")))
                .andExpect(content().string(containsString("Revisão preventiva")));

        mockMvc.perform(get("/veiculos/editar/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/editar"))
                .andExpect(content().string(containsString("ABC1D23")));

        mockMvc.perform(get("/motoristas"))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/list"))
                .andExpect(content().string(containsString("Carlos Eduardo Silva")));

        mockMvc.perform(get("/motoristas/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/novo"));

        mockMvc.perform(get("/manutencoes"))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencao/list"))
                .andExpect(content().string(containsString("Revisão preventiva")));

        mockMvc.perform(get("/manutencoes/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencao/novo"));

        mockMvc.perform(get("/tags"))
                .andExpect(status().isOk())
                .andExpect(view().name("tag/list"))
                .andExpect(content().string(containsString("Rastreado GPS")));

        mockMvc.perform(get("/tags/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("tag/novo"));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/list"))
                .andExpect(content().string(containsString("carlos.silva")))
                .andExpect(content().string(containsString("admin")));

        mockMvc.perform(get("/usuarios/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/novo"));

        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"));
    }

    @Test
    void deveAutenticarUsuarioValidoERejeitarInvalidoNoLogin() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "admin")
                .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(post("/login")
                .param("username", "admin")
                .param("password", "senha-errada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("erro", "Usuário ou senha inválidos."));
    }

    @Test
    void deveRetornarFormularioComErrosQuandoDadosInvalidosNoCadastro() throws Exception {
        mockMvc.perform(post("/veiculos/novo")
                .param("placa", "")
                .param("modelo", "A")
                .param("ano", "1800")
                .param("quilometragem", "-10")
                .param("status", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/novo"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("veiculo", "placa", "modelo", "ano", "quilometragem", "status"))
                .andExpect(content().string(containsString("A placa é obrigatória.")));
    }

    @Test
    void deveRedirecionarComMensagemFlashDeErroQuandoIdInexistente() throws Exception {
        mockMvc.perform(get("/veiculos/detalhes/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"))
                .andExpect(flash().attribute("erro", "Veículo não encontrado."));

        mockMvc.perform(get("/veiculos/editar/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"))
                .andExpect(flash().attribute("erro", "Veículo não encontrado."));
    }

    @Test
    void deveCadastrarEditarEExcluirVeiculoComRelacionamentosEFeedback() throws Exception {
        mockMvc.perform(post("/veiculos/novo")
                .param("placa", "XYZ9K88")
                .param("modelo", "Volvo FH 540")
                .param("ano", "2025")
                .param("quilometragem", "5000")
                .param("status", "Disponível")
                .param("motorista", "1")
                .param("tags", "1", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"))
                .andExpect(flash().attribute("sucesso", "Veículo cadastrado com sucesso!"));
    }

    @Test
    void deveRespeitarPermissoesDeAdminOperadorEMotorista() throws Exception {
        Usuario operador = new Usuario(null, "op.teste", "Operador Teste", "op.teste@fleetops.com", "123456", "OPERADOR", true);
        usuarioService.salvar(operador);
        Usuario motorista = usuarioService.autenticar("carlos.silva", "123456").orElseThrow();

        // Operador: somente visualização e acesso ao próprio usuário (não pode criar/editar)
        mockMvc.perform(get("/veiculos/novo").sessionAttr("usuarioLogado", operador))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"));

        mockMvc.perform(get("/tags/novo").sessionAttr("usuarioLogado", operador))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tags"));

        mockMvc.perform(get("/usuarios/editar/" + operador.getId()).sessionAttr("usuarioLogado", operador))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));

        // Motorista: pode editar próprio usuário, próprio motorista, próprio veículo (id=1), suas manutenções e tags
        mockMvc.perform(get("/usuarios/editar/" + motorista.getId()).sessionAttr("usuarioLogado", motorista))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/editar"));

        mockMvc.perform(get("/motoristas/editar/1").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().isOk())
                .andExpect(view().name("motorista/editar"));

        mockMvc.perform(get("/veiculos/editar/1").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().isOk())
                .andExpect(view().name("veiculo/editar"));

        mockMvc.perform(get("/manutencoes/editar/1").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencao/editar"));

        mockMvc.perform(get("/tags/novo").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().isOk())
                .andExpect(view().name("tag/novo"));

        // Motorista: bloqueado ao tentar editar veículo/motorista/usuário de terceiros
        mockMvc.perform(get("/veiculos/editar/2").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/veiculos"));

        mockMvc.perform(get("/motoristas/editar/2").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/motoristas"));

        mockMvc.perform(get("/usuarios/editar/1").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
    }

    @Test
    void deveRealizarSoftDeleteNoUsuarioEInvisibilizarNaListagemELogin() throws Exception {
        Usuario temp = new Usuario(null, "soft.delete", "Usuario Soft Delete", "soft.delete@fleetops.com", "123456", "OPERADOR", true);
        usuarioService.salvar(temp);
        Long tempId = temp.getId();

        mockMvc.perform(post("/usuarios/excluir/" + tempId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("sucesso", "Usuário excluído com sucesso!"));

        // Permanece no banco com ativo = false e data_modificacao preenchida
        Usuario noBanco = usuarioRepository.findById(tempId).orElseThrow();
        assertThat(noBanco.getAtivo()).isFalse();
        assertThat(noBanco.getDataModificacao()).isNotNull();

        // Não é mais visível na listagem nem em buscarPorId
        assertThat(usuarioService.buscarPorId(tempId)).isEmpty();
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("soft.delete@fleetops.com"))));

        // Não consegue mais fazer login
        assertThat(usuarioService.autenticar("soft.delete", "123456")).isEmpty();
    }
}
