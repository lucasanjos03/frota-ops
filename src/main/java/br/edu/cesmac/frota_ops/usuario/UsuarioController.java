package br.edu.cesmac.frota_ops.usuario;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        List<Usuario> visiveis = usuarioService.listarVisiveisPara(logado);

        model.addAttribute("usuarios", visiveis);
        model.addAttribute("totalUsuarios", visiveis.size());
        return "usuario/list";
    }

    @GetMapping("/novo")
    public String formularioNovo(HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        if (!logado.isAdmin()) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem cadastrar novos usuários.");
            return "redirect:/usuarios";
        }

        model.addAttribute("usuario", new Usuario());
        return "usuario/novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("usuario") Usuario usuario,
                            BindingResult result,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        if (!logado.isAdmin()) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem cadastrar novos usuários.");
            return "redirect:/usuarios";
        }

        if (result.hasErrors()) {
            return "usuario/novo";
        }

        usuarioService.salvar(usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário cadastrado com sucesso!");
        return "redirect:/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        if (!logado.podeEditarUsuario(id)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você não tem permissão para editar este usuário.");
            return "redirect:/usuarios";
        }

        Usuario usuario = usuarioService.buscarPorId(id).orElse(null);
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }

        model.addAttribute("usuario", usuario);
        return "usuario/editar";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("usuario") Usuario usuario,
                            BindingResult result,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        if (!logado.podeEditarUsuario(id)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você não tem permissão para editar este usuário.");
            return "redirect:/usuarios";
        }

        if (result.hasErrors()) {
            usuario.setId(id);
            return "usuario/editar";
        }

        boolean atualizado = usuarioService.atualizar(id, usuario, logado);
        if (!atualizado) {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        if (!logado.isAdmin()) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem excluir usuários.");
            return "redirect:/usuarios";
        }

        boolean excluido = usuarioService.excluir(id);
        if (!excluido) {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Usuário excluído com sucesso!");
        return "redirect:/usuarios";
    }
}
