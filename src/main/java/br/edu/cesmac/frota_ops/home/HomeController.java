package br.edu.cesmac.frota_ops.home;

import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        List<Veiculo> veiculosAlerta = List.of(
                new Veiculo(1L, "ABC1D23", "Ford Transit 350", 2023, 42180L,
                        "Disponível", LocalDate.of(2026, 6, 18),
                        LocalDate.of(2026, 9, 18), 36200L, null, "Em dia"),
                new Veiculo(2L, "DEF4G56", "Mercedes-Benz Sprinter", 2022, 67420L,
                        "Em trânsito", LocalDate.of(2026, 3, 12),
                        LocalDate.of(2026, 9, 4), 28700L, null, "Atenção")
        );

        model.addAttribute("veiculosAlerta", veiculosAlerta);
        model.addAttribute("totalVeiculos", 24);
        model.addAttribute("totalDisponiveis", 14);
        model.addAttribute("totalEmManutencao", 3);
        model.addAttribute("totalEmTransito", 7);

        return "home/index";
    }
}
