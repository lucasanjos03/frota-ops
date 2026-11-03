package br.edu.cesmac.frota_ops.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes das telas de autenticação: mensagens de erro/sucesso padronizadas (sem duplicação),
 * validação visual dos formulários e campo de senha sempre mascarado.
 *
 * <p>As regras de autenticação não são alteradas por esta etapa: os testes apenas garantem
 * que a apresentação das mensagens segue o design system.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthPaginaTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveExibirMensagemDeErroUnicaNoLogin() throws Exception {
        mockMvc.perform(post("/login").param("username", "inexistente").param("password", "errada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("erro", "Usuário ou senha inválidos."));

        // A mensagem é publicada uma única vez, via flash, e não duplicada no formulário.
        String html = mockMvc.perform(get("/login").flashAttr("erro", "Usuário ou senha inválidos."))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("alert alert-erro")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(contarOcorrencias(html, "Usuário ou senha inválidos.")).isEqualTo(1);
    }

    @Test
    void deveExibirMensagemDeSucessoAposCadastro() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .param("nome", "Novo Usuario")
                        .param("username", "novo.cadastro")
                        .param("email", "novo.cadastro@fleetops.com")
                        .param("senha", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("sucesso", "Conta criada com sucesso! Faça login para continuar."));
    }

    @Test
    void deveExibirValidacaoVisualNoCadastroInvalido() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .param("nome", "")
                        .param("username", "")
                        .param("email", "invalido")
                        .param("senha", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"))
                .andExpect(content().string(containsString("mensagem-erro")))
                .andExpect(content().string(containsString("campo-erro")));
    }

    @Test
    void deveMascararOCampoDeSenhaNoCadastro() throws Exception {
        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"))
                .andExpect(content().string(containsString("id=\"senha\"")))
                .andExpect(content().string(containsString("type=\"password\"")));
    }

    private int contarOcorrencias(String texto, String alvo) {
        int total = 0;
        int indice = 0;
        while ((indice = texto.indexOf(alvo, indice)) != -1) {
            total++;
            indice += alvo.length();
        }
        return total;
    }
}
