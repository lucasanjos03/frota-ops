package br.edu.cesmac.frota_ops.veiculo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/veiculos")
public class VeiculoController {

    private final List<Veiculo> veiculosMock = new ArrayList<>(List.of(
            new Veiculo(1L, "ABC1D23", "Ford Transit 350", 2023, 42180L,
                    "Disponível", LocalDate.of(2026, 6, 18),
                    LocalDate.of(2026, 9, 18), 36200L,
                    "Veículo utilizado em rotas urbanas e regionais. Pneus revisados na última manutenção preventiva.",
                    "Em dia"),
            new Veiculo(2L, "DEF4G56", "Mercedes-Benz Sprinter", 2022, 67420L,
                    "Em trânsito", LocalDate.of(2026, 3, 12),
                    LocalDate.of(2026, 9, 4), 28700L,
                    "Veículo de longa distância, utilizado principalmente em rodovias.",
                    "Atenção"),
            new Veiculo(3L, "GHI7J89", "Volkswagen Delivery", 2021, 112300L,
                    "Em manutenção", LocalDate.of(2026, 5, 20),
                    LocalDate.of(2026, 11, 20), 98500L,
                    "Em revisão geral de freios e suspensão.",
                    "Em manutenção"),
            new Veiculo(4L, "JKL0M12", "Iveco Daily", 2024, 15800L,
                    "Disponível", LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 10, 1), 12000L,
                    "Veículo novo, adquirido recentemente.",
                    "Em dia")
    ));

    private final List<Manutencao> manutencoesMock = List.of(
            new Manutencao(1L, LocalDate.of(2026, 6, 18), 36200L,
                    "Revisão preventiva", "Oficina Central", "Troca de óleo e filtros", 1L),
            new Manutencao(2L, LocalDate.of(2026, 3, 12), 28700L,
                    "Inspeção de freios", "Equipe interna", "Sem irregularidades", 1L),
            new Manutencao(3L, LocalDate.of(2026, 3, 12), 28700L,
                    "Revisão preventiva", "Oficina Central", "Troca de pastilhas", 2L)
    );

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String status,
                         Model model) {
        List<Veiculo> filtrados = veiculosMock;

        if (q != null && !q.isBlank()) {
            String busca = q.toLowerCase();
            filtrados = filtrados.stream()
                    .filter(v -> v.getPlaca().toLowerCase().contains(busca)
                            || v.getModelo().toLowerCase().contains(busca))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            filtrados = filtrados.stream()
                    .filter(v -> status.equals(v.getStatus()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("veiculos", filtrados);
        model.addAttribute("totalVeiculos", veiculosMock.size());
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
        Long novoId = (long) (veiculosMock.size() + 1);
        veiculo.setId(novoId);
        veiculosMock.add(veiculo);
        return "redirect:/veiculos";
    }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable("id") Long id, Model model) {
        Veiculo veiculo = veiculosMock.stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElse(null);

        List<Manutencao> historico = manutencoesMock.stream()
                .filter(m -> m.getVeiculoId().equals(id))
                .collect(Collectors.toList());

        model.addAttribute("veiculo", veiculo);
        model.addAttribute("manutencoes", historico);
        return "veiculo/detalhes";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable("id") Long id, Model model) {
        Veiculo veiculo = veiculosMock.stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElse(new Veiculo());

        model.addAttribute("veiculo", veiculo);
        return "veiculo/editar";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable("id") Long id,
                            @ModelAttribute Veiculo veiculo) {
        veiculosMock.removeIf(v -> v.getId().equals(id));
        veiculo.setId(id);
        veiculosMock.add(veiculo);
        return "redirect:/veiculos/" + id;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable("id") Long id) {
        veiculosMock.removeIf(v -> v.getId().equals(id));
        return "redirect:/veiculos";
    }
}
