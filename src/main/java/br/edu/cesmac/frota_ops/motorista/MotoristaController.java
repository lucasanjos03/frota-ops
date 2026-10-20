package br.edu.cesmac.frota_ops.motorista;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/motoristas")
public class MotoristaController {

    private final MotoristaService motoristaService;

    public MotoristaController(MotoristaService motoristaService) {
        this.motoristaService = motoristaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("motoristas", motoristaService.listarTodos());
        model.addAttribute("totalMotoristas", motoristaService.contarTotal());
        return "motorista/list";
    }

    @GetMapping("/novo")
    public String formularioNovo(Model model) {
        model.addAttribute("motorista", new Motorista());
        return "motorista/novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("motorista") Motorista motorista,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "motorista/novo";
        }

        motoristaService.salvar(motorista);
        redirectAttributes.addFlashAttribute("sucesso", "Motorista cadastrado com sucesso!");
        return "redirect:/motoristas";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Motorista motorista = motoristaService.buscarPorId(id).orElse(null);
        if (motorista == null) {
            redirectAttributes.addFlashAttribute("erro", "Motorista não encontrado.");
            return "redirect:/motoristas";
        }

        model.addAttribute("motorista", motorista);
        return "motorista/editar";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("motorista") Motorista motorista,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            motorista.setId(id);
            return "motorista/editar";
        }

        boolean atualizado = motoristaService.atualizar(id, motorista);
        if (!atualizado) {
            redirectAttributes.addFlashAttribute("erro", "Motorista não encontrado.");
            return "redirect:/motoristas";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Motorista atualizado com sucesso!");
        return "redirect:/motoristas";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttributes) {
        boolean excluido = motoristaService.excluir(id);
        if (!excluido) {
            redirectAttributes.addFlashAttribute("erro", "Motorista não encontrado.");
            return "redirect:/motoristas";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Motorista excluído com sucesso!");
        return "redirect:/motoristas";
    }
}

