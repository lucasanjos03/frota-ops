package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/manutencoes")
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    public ManutencaoController(ManutencaoService manutencaoService) {
        this.manutencaoService = manutencaoService;
    }

    /**
     * Histórico de manutenções com filtros de veículo, serviço e período. A data corrente é
     * calculada uma única vez e repassada ao serviço, de modo que indicadores, gráfico,
     * painel de revisões e a classificação do período usem a mesma referência.
     */
    @GetMapping
    public String listar(@RequestParam(required = false) Long veiculoId,
                         @RequestParam(required = false) String servico,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                         HttpSession session,
                         Model model) {
        ManutencaoFiltro filtro = new ManutencaoFiltro(veiculoId, servico, de, ate);
        LocalDate hoje = LocalDate.now();

        model.addAttribute("manutencoes", manutencaoService.listar(session, filtro));
        model.addAttribute("resumo", manutencaoService.montarResumo(session, hoje));
        model.addAttribute("filtro", filtro);
        model.addAttribute("hoje", hoje);
        model.addAttribute("veiculos", manutencaoService.listarVeiculosNoEscopo(session));
        return "manutencao/list";
    }

    @GetMapping("/novo")
    public String formularioNovo(@RequestParam(required = false) Long veiculoId,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!manutencaoService.podeCriar(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você não possui permissão para registrar manutenções.");
            return "redirect:/manutencoes";
        }

        model.addAttribute("manutencao", manutencaoService.criarNovaManutencao(veiculoId));
        model.addAttribute("veiculos", manutencaoService.listarVeiculosPermitidos(session));
        return "manutencao/novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("manutencao") Manutencao manutencao,
                            BindingResult result,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!manutencaoService.podeGerenciarVeiculo(session, manutencao.getVeiculo())) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você só pode registrar manutenções nos seus próprios veículos.");
            return "redirect:/manutencoes";
        }

        if (result.hasErrors()) {
            model.addAttribute("veiculos", manutencaoService.listarVeiculosPermitidos(session));
            return "manutencao/novo";
        }

        manutencaoService.salvar(manutencao);
        redirectAttributes.addFlashAttribute("sucesso", "Manutenção registrada com sucesso!");
        return "redirect:/manutencoes";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Manutencao manutencao = manutencaoService.buscarPorId(id).orElse(null);
        if (manutencao == null) {
            redirectAttributes.addFlashAttribute("erro", "Manutenção não encontrada.");
            return "redirect:/manutencoes";
        }

        if (!manutencaoService.podeEditarManutencao(session, manutencao)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você só pode editar manutenções dos seus próprios veículos.");
            return "redirect:/manutencoes";
        }

        model.addAttribute("manutencao", manutencao);
        model.addAttribute("veiculos", manutencaoService.listarVeiculosPermitidos(session));
        return "manutencao/editar";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("manutencao") Manutencao manutencao,
                            BindingResult result,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Manutencao existente = manutencaoService.buscarPorId(id).orElse(null);
        if (existente == null) {
            redirectAttributes.addFlashAttribute("erro", "Manutenção não encontrada.");
            return "redirect:/manutencoes";
        }

        if (!manutencaoService.podeEditarManutencao(session, existente)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você só pode editar manutenções dos seus próprios veículos.");
            return "redirect:/manutencoes";
        }

        if (result.hasErrors()) {
            manutencao.setId(id);
            model.addAttribute("veiculos", manutencaoService.listarVeiculosPermitidos(session));
            return "manutencao/editar";
        }

        manutencaoService.atualizar(id, manutencao);
        redirectAttributes.addFlashAttribute("sucesso", "Manutenção atualizada com sucesso!");
        return "redirect:/manutencoes";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        Manutencao existente = manutencaoService.buscarPorId(id).orElse(null);
        if (existente == null) {
            redirectAttributes.addFlashAttribute("erro", "Manutenção não encontrada.");
            return "redirect:/manutencoes";
        }

        if (!manutencaoService.podeExcluir(session, existente)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você não tem permissão para excluir esta manutenção.");
            return "redirect:/manutencoes";
        }

        manutencaoService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Manutenção excluída com sucesso!");
        return "redirect:/manutencoes";
    }
}
