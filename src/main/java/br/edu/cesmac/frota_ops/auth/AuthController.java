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

    @GetMapping("/cadastro")
    public String formularioCadastro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("usuario") Usuario usuario,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/cadastro";
        }

        usuarioService.salvar(usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Conta criada com sucesso! Faça login para continuar.");
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
