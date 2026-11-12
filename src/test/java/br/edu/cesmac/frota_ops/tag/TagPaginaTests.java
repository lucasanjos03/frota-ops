package br.edu.cesmac.frota_ops.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.cesmac.frota_ops.manutencao.ManutencaoRepository;
import br.edu.cesmac.frota_ops.motorista.Motorista;
import br.edu.cesmac.frota_ops.motorista.MotoristaService;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoRepository;

/**
 * Testes da página de tags: indicadores compactos, contagem de veículos por tag no escopo
 * do perfil e renderização dos componentes do design system.
 *
 * <p>A contagem respeita o recorte de visibilidade: ADMIN/OPERADOR enxergam a frota
 * completa; MOTORISTA enxerga apenas os veículos vinculados ao próprio cadastro. Como um
 * veículo pode pertencer a várias tags, a soma das contagens pode exceder o total da frota.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TagPaginaTests {

    @Autowired
    private TagService tagService;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------
    // Contagem por tag
    // ------------------------------------------------------------------

    @Test
    void deveContarVeiculosPorTagNoEscopoDaFrota() {
        limparDados();

        Tag urgente = criarTag("Urgente");
        Tag revisao = criarTag("Revisão");
        criarTag("Sem uso");

        criarVeiculo("TAG0001", null, urgente, revisao);
        criarVeiculo("TAG0002", null, urgente);
        criarVeiculo("TAG0003", null, revisao);

        TagPaginaDTO resumo = tagService.montarResumo(null);

        assertThat(resumo.escopoRestrito()).isFalse();
        assertThat(resumo.totalTags()).isEqualTo(3);
        assertThat(resumo.tagsEmUso()).isEqualTo(2);
        assertThat(resumo.tagsSemVeiculos()).isEqualTo(1);
        assertThat(resumo.veiculosPorTag())
                .containsEntry("Urgente", 2L)
                .containsEntry("Revisão", 2L)
                // A visão de frota completa inclui o catálogo inteiro; tags sem veículos
                // aparecem zeradas (no escopo do motorista, essas tags ficam ausentes).
                .containsEntry("Sem uso", 0L);
    }

    @Test
    void deveRestringirContagemDeTagsAosVeiculosDoMotorista() {
        limparDados();

        Tag urgente = criarTag("Urgente");
        Tag revisao = criarTag("Revisão");

        Usuario logado = criarMotorista("tag.escopo", "Tag Escopo", "777.888.999-00", "77788899900");
        Usuario terceiro = criarMotorista("tag.terceiro", "Tag Terceiro", "888.999.000-11", "88899900011");

        criarVeiculo("ESC0001", logado.getMotorista(), urgente, revisao);
        criarVeiculo("ESC0002", terceiro.getMotorista(), urgente);

        TagPaginaDTO noEscopoDoMotorista = tagService.montarResumo(sessaoDe(logado));
        assertThat(noEscopoDoMotorista.escopoRestrito()).isTrue();
        assertThat(noEscopoDoMotorista.veiculosPorTag())
                .containsEntry("Urgente", 1L)
                .containsEntry("Revisão", 1L);

        // A mesma base, vista por um perfil administrativo, conta a frota inteira.
        Map<String, Long> frotaCompleta = tagService.contarVeiculosPorTag(null);
        assertThat(frotaCompleta).containsEntry("Urgente", 2L).containsEntry("Revisão", 1L);
    }

    // ------------------------------------------------------------------
    // Renderização
    // ------------------------------------------------------------------

    @Test
    void deveRenderizarIndicadoresTabelaEContagemDeTags() throws Exception {
        limparDados();

        Tag urgente = criarTag("Urgente");
        criarVeiculo("TAGR001", null, urgente);

        mockMvc.perform(get("/tags").sessionAttr("usuarioLogado", admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("tag/list"))
                // Indicadores compactos (três cartões, sem coluna vazia)
                .andExpect(content().string(containsString("Total de tags")))
                .andExpect(content().string(containsString("Tags em uso")))
                .andExpect(content().string(containsString("Tags sem veículos")))
                .andExpect(content().string(containsString("kpi-grid--tres")))
                // Tabela padronizada com a coluna de contagem
                .andExpect(content().string(containsString("Lista de tags operacionais")))
                .andExpect(content().string(containsString("Veículos associados")))
                .andExpect(content().string(containsString("Urgente")))
                // Ação autorizada para o perfil administrativo
                .andExpect(content().string(containsString("Nova tag")));
    }

    @Test
    void deveExibirEstadoVazioQuandoNaoHaTags() throws Exception {
        limparDados();

        mockMvc.perform(get("/tags").sessionAttr("usuarioLogado", admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("tag/list"))
                .andExpect(content().string(containsString("empty-state")))
                .andExpect(content().string(containsString("Nenhuma tag cadastrada")))
                .andExpect(content().string(containsString("Cadastre a primeira tag")))
                .andExpect(content().string(not(containsString("data-table"))));
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Usuario admin() {
        return usuarioService.autenticar("admin", "admin123").orElseThrow();
    }

    private MockHttpSession sessaoDe(Usuario usuario) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogado", usuario);
        return session;
    }

    private Tag criarTag(String nome) {
        return tagRepository.saveAndFlush(new Tag(null, nome));
    }

    private void limparDados() {
        manutencaoRepository.deleteAllInBatch();
        veiculoRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
    }

    private Usuario criarMotorista(String username, String nome, String cpf, String numeroCnh) {
        Usuario usuario = new Usuario(null, username, nome, username + "@fleetops.com", "123456", "OPERADOR", true);
        usuarioService.salvar(usuario);

        Motorista motorista = new Motorista(null, nome, cpf, numeroCnh, "B", LocalDate.now().plusYears(3), null);
        motorista.setUsuario(usuario);
        motoristaService.salvar(motorista);

        // Mantém o vínculo bidirecional em memória: em um único contexto de persistência
        // (teste transacional) o lado inverso Usuario.motorista não é preenchido pelo JPA.
        usuario.setMotorista(motorista);

        return usuarioService.autenticar(username, "123456").orElseThrow();
    }

    private Veiculo criarVeiculo(String placa, Motorista motorista, Tag... tags) {
        Veiculo veiculo = new Veiculo(null, placa, "Modelo " + placa, 2024, 1000L, "Disponível", null,
                null, null, null, "Em dia");
        veiculo.setMotorista(motorista);
        veiculo.setTags(List.of(tags));
        return veiculoRepository.saveAndFlush(veiculo);
    }
}
