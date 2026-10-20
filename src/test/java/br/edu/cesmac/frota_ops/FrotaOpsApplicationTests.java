package br.edu.cesmac.frota_ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
}
