package br.edu.cesmac.frota_ops.veiculo;

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

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/veiculos")
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String status,
                         Model model) {
        List<Veiculo> filtrados = veiculoService.filtrar(q, status);

        model.addAttribute("veiculos", filtrados);
        model.addAttribute("totalVeiculos", veiculoService.contarTotal());
        model.addAttribute("filtroQ", q);
        model.addAttribute("filtroStatus", status);
        return "veiculo/list";
    }

    @GetMapping("/novo")
    public String formularioNovo(HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!veiculoService.podeCriarOuExcluir(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem cadastrar novos veículos.");
            return "redirect:/veiculos";
        }

        model.addAttribute("veiculo", new Veiculo());
        carregarOpcoesFormulario(model);
        return "veiculo/novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("veiculo") Veiculo veiculo,
                            BindingResult result,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!veiculoService.podeCriarOuExcluir(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem cadastrar novos veículos.");
            return "redirect:/veiculos";
        }

        if (result.hasErrors()) {
            carregarOpcoesFormulario(model);
            return "veiculo/novo";
        }

        veiculoService.salvar(veiculo);
        redirectAttributes.addFlashAttribute("sucesso", "Veículo cadastrado com sucesso!");
        return "redirect:/veiculos";
    }

    @GetMapping("/detalhes/{id}")
    public String detalhes(@PathVariable("id") Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Veiculo veiculo = veiculoService.buscarPorId(id).orElse(null);
        if (veiculo == null) {
            redirectAttributes.addFlashAttribute("erro", "Veículo não encontrado.");
            return "redirect:/veiculos";
        }

        model.addAttribute("veiculo", veiculo);
        model.addAttribute("manutencoes", veiculo.getManutencoes());
        return "veiculo/detalhes";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Veiculo veiculo = veiculoService.buscarPorId(id).orElse(null);
        if (veiculo == null) {
            redirectAttributes.addFlashAttribute("erro", "Veículo não encontrado.");
            return "redirect:/veiculos";
        }

        if (!veiculoService.podeEditar(session, veiculo)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você só tem permissão para editar veículos alocados para você.");
            return "redirect:/veiculos";
        }

        model.addAttribute("veiculo", veiculo);
        carregarOpcoesFormulario(model);
        return "veiculo/editar";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("veiculo") Veiculo veiculo,
                            BindingResult result,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Veiculo existente = veiculoService.buscarPorId(id).orElse(null);
        if (existente == null) {
            redirectAttributes.addFlashAttribute("erro", "Veículo não encontrado.");
            return "redirect:/veiculos";
        }

        if (!veiculoService.podeEditar(session, existente)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você só tem permissão para editar veículos alocados para você.");
            return "redirect:/veiculos";
        }

        if (result.hasErrors()) {
            veiculo.setId(id);
            carregarOpcoesFormulario(model);
            return "veiculo/editar";
        }

        veiculoService.atualizar(id, veiculo, session);
        redirectAttributes.addFlashAttribute("sucesso", "Veículo atualizado com sucesso!");
        return "redirect:/veiculos/detalhes/" + id;
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (!veiculoService.podeCriarOuExcluir(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem excluir veículos.");
            return "redirect:/veiculos";
        }

        boolean excluido = veiculoService.excluir(id);
        if (!excluido) {
            redirectAttributes.addFlashAttribute("erro", "Veículo não encontrado.");
            return "redirect:/veiculos";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Veículo excluído com sucesso!");
        return "redirect:/veiculos";
    }

    private void carregarOpcoesFormulario(Model model) {
        model.addAttribute("motoristas", veiculoService.listarMotoristas());
        model.addAttribute("tags", veiculoService.listarTags());
    }
}
