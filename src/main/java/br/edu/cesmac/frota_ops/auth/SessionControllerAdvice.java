package br.edu.cesmac.frota_ops.auth;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class SessionControllerAdvice {

    private final UsuarioService usuarioService;

    public SessionControllerAdvice(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("usuarioLogado")
    public Usuario adicionarUsuarioLogadoAoModel(HttpSession session) {
        return usuarioService.obterUsuarioLogado(session);
    }
}

