package br.edu.cesmac.frota_ops.veiculo;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String formularioNovo(Model model) {
        model.addAttribute("veiculo", new Veiculo());
        return "veiculo/novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@ModelAttribute Veiculo veiculo) {
        veiculoService.salvar(veiculo);
        return "redirect:/veiculos";
    }

    @GetMapping("/detalhes/{id}")
    public String detalhes(@PathVariable("id") Long id, Model model) {
        Veiculo veiculo = veiculoService.buscarPorId(id).orElse(null);
        List<Manutencao> historico = veiculoService.buscarManutencoesPorVeiculo(id);

        model.addAttribute("veiculo", veiculo);
        model.addAttribute("manutencoes", historico);
        return "veiculo/detalhes";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Veiculo veiculo = veiculoService.buscarPorId(id).orElse(new Veiculo());

        model.addAttribute("veiculo", veiculo);
        return "veiculo/editar";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable("id") Long id,
                            @ModelAttribute Veiculo veiculo) {
        veiculoService.atualizar(id, veiculo);
        return "redirect:/veiculos/detalhes/" + id;
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id) {
        veiculoService.excluir(id);
        return "redirect:/veiculos";
    }
}
