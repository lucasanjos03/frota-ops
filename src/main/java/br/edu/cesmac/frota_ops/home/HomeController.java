package br.edu.cesmac.frota_ops.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.edu.cesmac.frota_ops.veiculo.VeiculoService;

@Controller
public class HomeController {

    private final VeiculoService veiculoService;

    public HomeController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("veiculosAlerta", veiculoService.buscarVeiculosAlerta());
        model.addAttribute("totalVeiculos", veiculoService.contarTotal());
        model.addAttribute("totalDisponiveis", veiculoService.contarPorStatus("Disponível"));
        model.addAttribute("totalEmManutencao", veiculoService.contarPorStatus("Em manutenção"));
        model.addAttribute("totalEmTransito", veiculoService.contarPorStatus("Em trânsito"));

        return "home/index";
    }
}
