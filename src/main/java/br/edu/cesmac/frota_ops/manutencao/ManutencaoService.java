package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoService;

@Service
public class ManutencaoService {

    private final ManutencaoRepository manutencaoRepository;
    private final VeiculoService veiculoService;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             VeiculoService veiculoService) {
        this.manutencaoRepository = manutencaoRepository;
        this.veiculoService = veiculoService;
    }

    public List<Manutencao> listarTodas() {
        return manutencaoRepository.findAllByOrderByDataDesc();
    }

    public List<Manutencao> buscarPorVeiculo(Long veiculoId) {
        return manutencaoRepository.findByVeiculoIdOrderByDataDesc(veiculoId);
    }

    public Optional<Manutencao> buscarPorId(Long id) {
        return manutencaoRepository.findById(id);
    }

    public Manutencao criarNovaManutencao(Long veiculoId) {
        Manutencao manutencao = new Manutencao();
        manutencao.setData(LocalDate.now());
        if (veiculoId != null) {
            veiculoService.buscarPorId(veiculoId).ifPresent(veiculo -> {
                manutencao.setVeiculo(veiculo);
                manutencao.setQuilometragem(veiculo.getQuilometragem());
            });
        }
        return manutencao;
    }

    public void salvar(Manutencao manutencao) {
        manutencao.setId(null);
        manutencaoRepository.save(manutencao);

        if (manutencao.getVeiculo() != null) {
            veiculoService.atualizarAposManutencao(
                    manutencao.getVeiculo(),
                    manutencao.getData(),
                    manutencao.getQuilometragem()
            );
        }
    }

    public boolean atualizar(Long id, Manutencao dadosAtualizados) {
        Manutencao manutencao = manutencaoRepository.findById(id).orElse(null);
        if (manutencao == null) {
            return false;
        }

        manutencao.setVeiculo(dadosAtualizados.getVeiculo());
        manutencao.setData(dadosAtualizados.getData());
        manutencao.setQuilometragem(dadosAtualizados.getQuilometragem());
        manutencao.setServicoRealizado(dadosAtualizados.getServicoRealizado());
        manutencao.setResponsavel(dadosAtualizados.getResponsavel());
        manutencao.setObservacao(dadosAtualizados.getObservacao());

        manutencaoRepository.save(manutencao);

        if (manutencao.getVeiculo() != null) {
            veiculoService.atualizarAposManutencao(
                    manutencao.getVeiculo(),
                    manutencao.getData(),
                    manutencao.getQuilometragem()
            );
        }
        return true;
    }

    public boolean excluir(Long id) {
        if (!manutencaoRepository.existsById(id)) {
            return false;
        }
        manutencaoRepository.deleteById(id);
        return true;
    }

    public List<Veiculo> listarVeiculos() {
        return veiculoService.listarTodos();
    }

    public long contarTotal() {
        return manutencaoRepository.count();
    }
}
