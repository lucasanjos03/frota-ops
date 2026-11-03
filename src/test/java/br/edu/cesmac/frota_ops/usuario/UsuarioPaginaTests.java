package br.edu.cesmac.frota_ops.usuario;

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

import br.edu.cesmac.frota_ops.motorista.Motorista;
import br.edu.cesmac.frota_ops.motorista.MotoristaRepository;
import br.edu.cesmac.frota_ops.motorista.MotoristaService;

/**
 * Testes da página de usuários: indicadores compactos por perfil (restritos ao ADMIN),
 * recorte de visibilidade do perfil MOTORISTA, padronização da tabela e do estado vazio,
 * e proteção de credenciais na interface.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioPaginaTests {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------
    // Resumo por perfil
    // ------------------------------------------------------------------

    @Test
    void deveResumirUsuariosAtivosPorPerfil() {
        UsuarioPaginaDTO base = usuarioService.montarResumo();
        assertThat(base.total()).isEqualTo(usuarioRepository.countByAtivoTrue());

        criarUsuario("resumo.admin", "ADMIN");
        criarUsuario("resumo.oper", "OPERADOR");
        criarUsuario("resumo.mot", "MOTORISTA");

        UsuarioPaginaDTO resumo = usuarioService.montarResumo();

        assertThat(resumo.administradores()).isEqualTo(base.administradores() + 1);
        assertThat(resumo.operadores()).isEqualTo(base.operadores() + 1);
        assertThat(resumo.motoristas()).isEqualTo(base.motoristas() + 1);
        assertThat(resumo.total()).isEqualTo(base.total() + 3);
        // Perfis mapeados fecham com o total da base ativa.
        assertThat(resumo.total()).isEqualTo(resumo.administradores() + resumo.operadores() + resumo.motoristas());
    }

    // ------------------------------------------------------------------
    // Renderização e escopo
    // ------------------------------------------------------------------

    @Test
    void deveRenderizarIndicadoresPorPerfilApenasParaAdmin() throws Exception {
        criarUsuario("render.admin", "ADMIN");

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/list"))
                .andExpect(content().string(containsString("Usuários do Sistema")))
                // Indicadores compactos por perfil, visíveis só ao ADMIN
                .andExpect(content().string(containsString("Contas habilitadas no sistema")))
                .andExpect(content().string(containsString("Administradores")))
                .andExpect(content().string(containsString("Operadores")))
                // Tabela padronizada
                .andExpect(content().string(containsString("data-table--usuarios")))
                .andExpect(content().string(containsString("Lista de usuários ativos")))
                .andExpect(content().string(containsString("Contas de acesso")))
                .andExpect(content().string(containsString("Vínculo motorista")))
                // A listagem não expõe campo de credencial.
                .andExpect(content().string(not(containsString("type=\"password\""))));
    }

    @Test
    void deveRestringirListagemDeUsuariosAoProprioPerfil() throws Exception {
        Usuario motorista = criarMotorista("usuario.escopo", "Usuario Escopo", "999.000.111-22", "99900011122");
        String numeroCnh = motorista.getMotorista().getNumeroCnh();

        mockMvc.perform(get("/usuarios").sessionAttr("usuarioLogado", motorista))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Meu Perfil de Usuário")))
                .andExpect(content().string(containsString("Você visualiza apenas a sua própria conta")))
                .andExpect(content().string(containsString("CNH " + numeroCnh)))
                // Sem indicadores da base inteira para perfis não administrativos.
                .andExpect(content().string(not(containsString("Contas habilitadas no sistema"))))
                .andExpect(content().string(not(containsString("Administradores"))));
    }

    @Test
    void deveExibirEstadoVazioQuandoNaoHaUsuariosAtivos() throws Exception {
        usuarioRepository.findAll().forEach(u -> u.setAtivo(false));
        usuarioRepository.flush();

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/list"))
                .andExpect(content().string(containsString("empty-state")))
                .andExpect(content().string(containsString("Nenhum usuário ativo encontrado")))
                .andExpect(content().string(containsString("Não há contas ativas visíveis")))
                .andExpect(content().string(not(containsString("data-table"))));
    }

    // ------------------------------------------------------------------
    // Formulário de edição
    // ------------------------------------------------------------------

    @Test
    void deveMascararOCampoDeSenhaNoFormularioDeEdicao() throws Exception {
        Usuario alvo = criarUsuario("edicao.usuario", "OPERADOR");

        mockMvc.perform(get("/usuarios/editar/" + alvo.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/editar"))
                .andExpect(content().string(containsString("id=\"senha\"")))
                .andExpect(content().string(containsString("type=\"password\"")));
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Usuario criarUsuario(String username, String perfil) {
        Usuario usuario = new Usuario(null, username, "Usuário " + username, username + "@fleetops.com",
                "123456", perfil, true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Usuario criarMotorista(String username, String nome, String cpf, String numeroCnh) {
        Usuario usuario = criarUsuario(username, "OPERADOR");

        Motorista motorista = new Motorista(null, nome, cpf, numeroCnh, "B", LocalDate.now().plusYears(3), null);
        motorista.setUsuario(usuario);
        motoristaService.salvar(motorista);

        // Mantém o vínculo bidirecional em memória: em um único contexto de persistência
        // (teste transacional) o lado inverso Usuario.motorista não é preenchido pelo JPA.
        usuario.setMotorista(motorista);

        return usuarioService.autenticar(username, "123456").orElseThrow();
    }
}
