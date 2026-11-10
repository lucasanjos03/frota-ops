package br.edu.cesmac.frota_ops.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes do armazenamento de senhas (R4b): senhas novas são gravadas como hash BCrypt e
 * senhas legadas em texto plano são convertidas para hash no primeiro login que confere,
 * sem migração de dados dedicada.
 */
@SpringBootTest
@Transactional
class UsuarioSenhaTests {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void deveArmazenarSenhaDeNovoUsuarioComoHashBcrypt() {
        usuarioService.salvar(new Usuario(null, "senha.nova", "Senha Nova",
                "senha.nova@fleetops.com", "123456", "OPERADOR", true));

        String armazenada = buscarSenha("senha.nova");

        assertThat(armazenada).isNotEqualTo("123456");
        assertThat(armazenada).startsWith("$2");
        assertThat(usuarioService.autenticar("senha.nova", "123456")).isPresent();
    }

    @Test
    void deveConverterSenhaLegadaEmTextoPlanoParaHashNoPrimeiroLogin() {
        // Simula a base legada: senha gravada em texto plano, sem passar pelo hash do serviço.
        usuarioRepository.save(new Usuario(null, "senha.legada", "Senha Legada",
                "senha.legada@fleetops.com", "plana123", "OPERADOR", true));
        assertThat(buscarSenha("senha.legada")).isEqualTo("plana123");

        assertThat(usuarioService.autenticar("senha.legada", "plana123")).isPresent();

        assertThat(buscarSenha("senha.legada")).startsWith("$2");
        assertThat(usuarioService.autenticar("senha.legada", "plana123")).isPresent();
    }

    @Test
    void deveManterHashExistenteAposNovoLogin() {
        usuarioService.salvar(new Usuario(null, "senha.hash", "Senha Hash",
                "senha.hash@fleetops.com", "abcdef", "OPERADOR", true));
        String primeiroHash = buscarSenha("senha.hash");

        usuarioService.autenticar("senha.hash", "abcdef");

        assertThat(buscarSenha("senha.hash")).isEqualTo(primeiroHash);
    }

    @Test
    void deveRejeitarSenhaIncorretaParaHashBcrypt() {
        usuarioService.salvar(new Usuario(null, "senha.errada", "Senha Errada",
                "senha.errada@fleetops.com", "correta1", "OPERADOR", true));

        assertThat(usuarioService.autenticar("senha.errada", "errada1")).isEmpty();
    }

    private String buscarSenha(String username) {
        return usuarioRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow()
                .getSenha();
    }
}
