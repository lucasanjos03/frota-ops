package br.edu.cesmac.frota_ops.auth;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String postLogin(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Optional<Usuario> autenticado = usuarioService.autenticar(username, password);
        if (autenticado.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Usuário ou senha inválidos.");
            redirectAttributes.addFlashAttribute("usernameTentativa", username);
            return "redirect:/login";
        }

        session.setAttribute("usuarioLogado", autenticado.get());
        return "redirect:/";
    }

    /**
     * Formulário de criação de usuário. Deixou de ser público: o autocadastro foi
     * desativado e a rota passa a exigir perfil ADMIN (a criação de usuários também está
     * disponível em {@code /usuarios/novo}).
     */
    @GetMapping("/cadastro")
    public String formularioCadastro(HttpSession session,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (!ehAdmin(session)) {
            redirectAttributes.addFlashAttribute("erro",
                    "Acesso negado: apenas administradores podem cadastrar novos usuários.");
            return "redirect:/usuarios";
        }
        model.addAttribute("usuario", new Usuario());
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("usuario") Usuario usuario,
                            BindingResult result,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (!ehAdmin(session)) {
            redirectAttributes.addFlashAttribute("erro",
                    "Acesso negado: apenas administradores podem cadastrar novos usuários.");
            return "redirect:/usuarios";
        }

        if (result.hasErrors()) {
            return "auth/cadastro";
        }

        usuarioService.salvar(usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário cadastrado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private boolean ehAdmin(HttpSession session) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        return logado != null && logado.isAdmin();
    }
}
