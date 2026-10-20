package br.edu.cesmac.frota_ops.tag;

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
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tags", tagService.listarTodas());
        model.addAttribute("totalTags", tagService.contarTotal());
        return "tag/list";
    }

    @GetMapping("/novo")
    public String formularioNovo(HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!tagService.podeGerenciar(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: perfil Operador possui permissão apenas de visualização.");
            return "redirect:/tags";
        }

        model.addAttribute("tag", new Tag());
        return "tag/novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("tag") Tag tag,
                            BindingResult result,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (!tagService.podeGerenciar(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: perfil Operador possui permissão apenas de visualização.");
            return "redirect:/tags";
        }

        if (result.hasErrors()) {
            return "tag/novo";
        }

        tagService.salvar(tag);
        redirectAttributes.addFlashAttribute("sucesso", "Tag cadastrada com sucesso!");
        return "redirect:/tags";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!tagService.podeGerenciar(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: perfil Operador possui permissão apenas de visualização.");
            return "redirect:/tags";
        }

        Tag tag = tagService.buscarPorId(id).orElse(null);
        if (tag == null) {
            redirectAttributes.addFlashAttribute("erro", "Tag não encontrada.");
            return "redirect:/tags";
        }

        model.addAttribute("tag", tag);
        return "tag/editar";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("tag") Tag tag,
                            BindingResult result,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (!tagService.podeGerenciar(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: perfil Operador possui permissão apenas de visualização.");
            return "redirect:/tags";
        }

        if (result.hasErrors()) {
            tag.setId(id);
            return "tag/editar";
        }

        boolean atualizado = tagService.atualizar(id, tag);
        if (!atualizado) {
            redirectAttributes.addFlashAttribute("erro", "Tag não encontrada.");
            return "redirect:/tags";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Tag atualizada com sucesso!");
        return "redirect:/tags";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (!tagService.podeExcluir(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem excluir tags.");
            return "redirect:/tags";
        }

        boolean excluido = tagService.excluir(id);
        if (!excluido) {
            redirectAttributes.addFlashAttribute("erro", "Tag não encontrada.");
            return "redirect:/tags";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Tag excluída com sucesso!");
        return "redirect:/tags";
    }
}
