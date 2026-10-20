package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoService;
import jakarta.servlet.http.HttpSession;

@Service
public class ManutencaoService {

    private final ManutencaoRepository manutencaoRepository;
    private final VeiculoService veiculoService;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             VeiculoService veiculoService) {
        this.manutencaoRepository = manutencaoRepository;
        this.veiculoService = veiculoService;
    }

    public boolean podeCriar(HttpSession session) {
        return !veiculoService.listarVeiculosGerenciaveis(session).isEmpty()
                || veiculoService.podeCriarOuExcluir(session);
    }

    public boolean podeGerenciarVeiculo(HttpSession session, Veiculo veiculo) {
        return veiculoService.podeEditar(session, veiculo);
    }

    public boolean podeEditarManutencao(HttpSession session, Manutencao manutencao) {
        if (manutencao == null || manutencao.getVeiculo() == null) {
            return false;
        }
        return veiculoService.podeEditar(session, manutencao.getVeiculo());
    }

    public boolean podeExcluir(HttpSession session, Manutencao manutencao) {
        return veiculoService.podeCriarOuExcluir(session) || podeEditarManutencao(session, manutencao);
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

    public List<Veiculo> listarVeiculosPermitidos(HttpSession session) {
        return veiculoService.listarVeiculosGerenciaveis(session);
    }

    public long contarTotal() {
        return manutencaoRepository.count();
    }
}
