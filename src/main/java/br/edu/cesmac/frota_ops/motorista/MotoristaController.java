package br.edu.cesmac.frota_ops.motorista;

import java.time.LocalDate;

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
@RequestMapping("/motoristas")
public class MotoristaController {

    private final MotoristaService motoristaService;

    public MotoristaController(MotoristaService motoristaService) {
        this.motoristaService = motoristaService;
    }

    /**
     * Listagem com busca e filtros. A data corrente é calculada uma única vez e repassada ao
     * serviço, de modo que indicadores, painel de documentos e filtro por situação da CNH
     * usem exatamente a mesma referência.
     */
    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String categoria,
                         @RequestParam(required = false) String cnh,
                         @RequestParam(required = false) String vinculo,
                         Model model) {
        MotoristaFiltro filtro = new MotoristaFiltro(q, status, categoria, cnh, vinculo);
        LocalDate hoje = LocalDate.now();

        model.addAttribute("motoristas", motoristaService.listar(filtro, hoje));
        model.addAttribute("resumo", motoristaService.montarResumo(hoje));
        model.addAttribute("filtro", filtro);
        model.addAttribute("hoje", hoje);
        model.addAttribute("categorias", motoristaService.listarCategorias());
        return "motorista/list";
    }

    @GetMapping("/detalhes/{id}")
    public String detalhes(@PathVariable("id") Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Motorista motorista = motoristaService.buscarPorId(id).orElse(null);
        if (motorista == null) {
            redirectAttributes.addFlashAttribute("erro", "Motorista não encontrado.");
            return "redirect:/motoristas";
        }

        model.addAttribute("motorista", motorista);
        model.addAttribute("veiculos", motoristaService.listarVeiculosDoMotorista(id));
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("janelaCnhDias", MotoristaService.JANELA_CNH_DIAS);
        return "motorista/detalhes";
    }

    @GetMapping("/novo")
    public String formularioNovo(HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!motoristaService.podeCriarOuExcluir(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem cadastrar novos motoristas.");
            return "redirect:/motoristas";
        }

        model.addAttribute("motorista", new Motorista());
        model.addAttribute("usuariosDisponiveis", motoristaService.listarUsuariosDisponiveis(null));
        return "motorista/novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("motorista") Motorista motorista,
                            BindingResult result,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!motoristaService.podeCriarOuExcluir(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem cadastrar novos motoristas.");
            return "redirect:/motoristas";
        }

        if (result.hasErrors()) {
            Long usuarioId = motorista.getUsuario() != null ? motorista.getUsuario().getId() : null;
            model.addAttribute("usuariosDisponiveis", motoristaService.listarUsuariosDisponiveis(usuarioId));
            return "motorista/novo";
        }

        motoristaService.salvar(motorista);
        redirectAttributes.addFlashAttribute("sucesso", "Motorista cadastrado com sucesso!");
        return "redirect:/motoristas";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!motoristaService.podeEditar(session, id)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você só tem permissão para editar seu próprio perfil de motorista.");
            return "redirect:/motoristas";
        }

        Motorista motorista = motoristaService.buscarPorId(id).orElse(null);
        if (motorista == null) {
            redirectAttributes.addFlashAttribute("erro", "Motorista não encontrado.");
            return "redirect:/motoristas";
        }

        Long usuarioIdAtual = motorista.getUsuario() != null ? motorista.getUsuario().getId() : null;
        model.addAttribute("motorista", motorista);
        model.addAttribute("usuariosDisponiveis", motoristaService.listarUsuariosDisponiveis(usuarioIdAtual));
        return "motorista/editar";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("motorista") Motorista motorista,
                            BindingResult result,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!motoristaService.podeEditar(session, id)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: você só tem permissão para editar seu próprio perfil de motorista.");
            return "redirect:/motoristas";
        }

        if (result.hasErrors()) {
            motorista.setId(id);
            Long usuarioIdAtual = motorista.getUsuario() != null ? motorista.getUsuario().getId() : null;
            model.addAttribute("usuariosDisponiveis", motoristaService.listarUsuariosDisponiveis(usuarioIdAtual));
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
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (!motoristaService.podeCriarOuExcluir(session)) {
            redirectAttributes.addFlashAttribute("erro", "Acesso negado: apenas administradores podem excluir motoristas.");
            return "redirect:/motoristas";
        }

        boolean excluido = motoristaService.excluir(id);
        if (!excluido) {
            redirectAttributes.addFlashAttribute("erro", "Motorista não encontrado.");
            return "redirect:/motoristas";
        }

        redirectAttributes.addFlashAttribute("sucesso", "Motorista excluído com sucesso!");
        return "redirect:/motoristas";
    }
}
