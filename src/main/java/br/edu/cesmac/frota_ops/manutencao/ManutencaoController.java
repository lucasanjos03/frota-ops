package br.edu.cesmac.frota_ops.manutencao;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/manutencoes")
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    public ManutencaoController(ManutencaoService manutencaoService) {
        this.manutencaoService = manutencaoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long veiculoId, Model model) {
        List<Manutencao> manutencoes = (veiculoId != null)
                ? manutencaoService.buscarPorVeiculo(veiculoId)
                : manutencaoService.listarTodas();

        model.addAttribute("manutencoes", manutencoes);
        model.addAttribute("totalManutencoes", manutencaoService.contarTotal());
        model.addAttribute("veiculos", manutencaoService.listarVeiculos());
        model.addAttribute("filtroVeiculoId", veiculoId);
        return "manutencao/list";
    }

    @GetMapping("/novo")
    public String formularioNovo(@RequestParam(required = false) Long veiculoId, Model model) {
        model.addAttribute("manutencao", manutencaoService.criarNovaManutencao(veiculoId));
        model.addAttribute("veiculos", manutencaoService.listarVeiculos());
        return "manutencao/novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("manutencao") Manutencao manutencao,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("veiculos", manutencaoService.listarVeiculos());
            return "manutencao/novo";
        }

        manutencaoService.salvar(manutencao);
        redirectAttributes.addFlashAttribute("sucesso", "Manutenção registrada com sucesso!");
        return "redirect:/manutencoes";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Manutencao manutencao = manutencaoService.buscarPorId(id).orElse(null);
        if (manutencao == null) {
            redirectAttributes.addFlashAttribute("erro", "Manutenção não encontrada.");
            return "redirect:/manutencoes";
        }

        model.addAttribute("manutencao", manutencao);
        model.addAttribute("veiculos", manutencaoService.listarVeiculos());
        return "manutencao/editar";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("manutencao") Manutencao manutencao,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            manutencao.setId(id);
            model.addAttribute("veiculos", manutencaoService.listarVeiculos());
            return "manutencao/editar";
        }

        boolean atualizado = manutencaoService.atualizar(id, manutencao);
        if (!atualizado) {
            redirectAttributes.addFlashAttribute("erro", "Manutenção não encontrada.");
            return "redirect:/manutencoes";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Manutenção atualizada com sucesso!");
        return "redirect:/manutencoes";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttributes) {
        boolean excluido = manutencaoService.excluir(id);
        if (!excluido) {
            redirectAttributes.addFlashAttribute("erro", "Manutenção não encontrada.");
            return "redirect:/manutencoes";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Manutenção excluída com sucesso!");
        return "redirect:/manutencoes";
    }
}

