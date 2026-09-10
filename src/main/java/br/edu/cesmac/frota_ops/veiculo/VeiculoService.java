package br.edu.cesmac.frota_ops.veiculo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class VeiculoService {

    private List<Veiculo> veiculos = new ArrayList<>();
    private List<Manutencao> manutencoes = new ArrayList<>();

    public VeiculoService() {
        veiculos.add(new Veiculo(1L, "ABC1D23", "Ford Transit 350", 2023, 42180L,
                "Disponível", LocalDate.of(2026, 6, 18),
                LocalDate.of(2026, 9, 18), 36200L,
                "Veículo utilizado em rotas urbanas e regionais. Pneus revisados na última manutenção preventiva.",
                "Em dia"));

        veiculos.add(new Veiculo(2L, "DEF4G56", "Mercedes-Benz Sprinter", 2022, 67420L,
                "Em trânsito", LocalDate.of(2026, 3, 12),
                LocalDate.of(2026, 9, 4), 28700L,
                "Veículo de longa distância, utilizado principalmente em rodovias.",
                "Atenção"));

        veiculos.add(new Veiculo(3L, "GHI7J89", "Volkswagen Delivery", 2021, 112300L,
                "Em manutenção", LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 11, 20), 98500L,
                "Em revisão geral de freios e suspensão.",
                "Em manutenção"));

        veiculos.add(new Veiculo(4L, "JKL0M12", "Iveco Daily", 2024, 15800L,
                "Disponível", LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 10, 1), 12000L,
                "Veículo novo, adquirido recentemente.",
                "Em dia"));

        manutencoes.add(new Manutencao(1L, LocalDate.of(2026, 6, 18), 36200L,
                "Revisão preventiva", "Oficina Central", "Troca de óleo e filtros", 1L));
        manutencoes.add(new Manutencao(2L, LocalDate.of(2026, 3, 12), 28700L,
                "Inspeção de freios", "Equipe interna", "Sem irregularidades", 1L));
        manutencoes.add(new Manutencao(3L, LocalDate.of(2026, 3, 12), 28700L,
                "Revisão preventiva", "Oficina Central", "Troca de pastilhas", 2L));
    }

    public List<Veiculo> listarTodos() {
        return veiculos;
    }

    public List<Veiculo> filtrar(String q, String status) {
        List<Veiculo> filtrados = veiculos;

        if (q != null && !q.isBlank()) {
            String busca = q.toLowerCase();
            filtrados = filtrados.stream()
                    .filter(v -> (v.getPlaca() != null && v.getPlaca().toLowerCase().contains(busca))
                            || (v.getModelo() != null && v.getModelo().toLowerCase().contains(busca)))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            filtrados = filtrados.stream()
                    .filter(v -> status.equalsIgnoreCase(v.getStatus()))
                    .collect(Collectors.toList());
        }

        return filtrados;
    }

    public Optional<Veiculo> buscarPorId(Long id) {
        return veiculos.stream()
                .filter(v -> v.getId().equals(id))
                .findFirst();
    }

    public void salvar(Veiculo veiculo) {
        Long proximoId = veiculos.stream()
                .mapToLong(Veiculo::getId)
                .max()
                .orElse(0L) + 1;
        veiculo.setId(proximoId);
        veiculos.add(veiculo);
    }

    public void atualizar(Long id, Veiculo dadosAtualizados) {
        for (int i = 0; i < veiculos.size(); i++) {
            if (veiculos.get(i).getId().equals(id)) {
                dadosAtualizados.setId(id);
                // Preserva situacao e km da última revisão se não informados no formulário
                if (dadosAtualizados.getSituacaoManutencao() == null) {
                    dadosAtualizados.setSituacaoManutencao(veiculos.get(i).getSituacaoManutencao());
                }
                if (dadosAtualizados.getQuilometragemUltimaRevisao() == null) {
                    dadosAtualizados.setQuilometragemUltimaRevisao(veiculos.get(i).getQuilometragemUltimaRevisao());
                }
                veiculos.set(i, dadosAtualizados);
                return;
            }
        }
    }

    public void excluir(Long id) {
        veiculos.removeIf(v -> v.getId().equals(id));
    }

    public List<Manutencao> buscarManutencoesPorVeiculo(Long veiculoId) {
        return manutencoes.stream()
                .filter(m -> m.getVeiculoId().equals(veiculoId))
                .collect(Collectors.toList());
    }

    public List<Veiculo> buscarVeiculosAlerta() {
        return veiculos.stream()
                .filter(v -> v.getProximaRevisao() != null)
                .sorted((v1, v2) -> v1.getProximaRevisao().compareTo(v2.getProximaRevisao()))
                .collect(Collectors.toList());
    }

    public long contarTotal() {
        return veiculos.size();
    }

    public long contarPorStatus(String status) {
        return veiculos.stream()
                .filter(v -> status.equalsIgnoreCase(v.getStatus()))
                .count();
    }
}

