package br.edu.cesmac.frota_ops.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.cesmac.frota_ops.entities.Manutencao;
import br.edu.cesmac.frota_ops.entities.Veiculo;
import br.edu.cesmac.frota_ops.repositories.ManutencaoRepository;
import br.edu.cesmac.frota_ops.repositories.VeiculoRepository;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ManutencaoRepository manutencaoRepository;

    public VeiculoService(VeiculoRepository veiculoRepository, ManutencaoRepository manutencaoRepository) {
        this.veiculoRepository = veiculoRepository;
        this.manutencaoRepository = manutencaoRepository;
    }

    public List<Veiculo> listarTodos() {
        return veiculoRepository.findAll();
    }

    public List<Veiculo> filtrar(String q, String status) {
        boolean hasQ = q != null && !q.isBlank();
        boolean hasStatus = status != null && !status.isBlank();

        if (!hasQ && !hasStatus) {
            return veiculoRepository.findAll();
        }

        return veiculoRepository.filtrar(hasQ ? q.trim() : null, hasStatus ? status.trim() : null);
    }

    public Optional<Veiculo> buscarPorId(Long id) {
        return veiculoRepository.findById(id);
    }

    @Transactional
    public void salvar(Veiculo veiculo) {
        veiculoRepository.save(veiculo);
    }

    @Transactional
    public void atualizar(Long id, Veiculo dadosAtualizados) {
        veiculoRepository.findById(id).ifPresent(veiculoExistente -> {
            veiculoExistente.setPlaca(dadosAtualizados.getPlaca());
            veiculoExistente.setModelo(dadosAtualizados.getModelo());
            veiculoExistente.setAno(dadosAtualizados.getAno());
            veiculoExistente.setQuilometragem(dadosAtualizados.getQuilometragem());
            veiculoExistente.setStatus(dadosAtualizados.getStatus());
            veiculoExistente.setUltimaRevisao(dadosAtualizados.getUltimaRevisao());
            veiculoExistente.setProximaRevisao(dadosAtualizados.getProximaRevisao());
            veiculoExistente.setObservacoes(dadosAtualizados.getObservacoes());

            if (dadosAtualizados.getSituacaoManutencao() != null) {
                veiculoExistente.setSituacaoManutencao(dadosAtualizados.getSituacaoManutencao());
            }
            if (dadosAtualizados.getQuilometragemUltimaRevisao() != null) {
                veiculoExistente.setQuilometragemUltimaRevisao(dadosAtualizados.getQuilometragemUltimaRevisao());
            }

            veiculoRepository.save(veiculoExistente);
        });
    }

    @Transactional
    public void excluir(Long id) {
        veiculoRepository.deleteById(id);
    }

    public List<Manutencao> buscarManutencoesPorVeiculo(Long veiculoId) {
        return manutencaoRepository.findByVeiculoIdOrderByDataDesc(veiculoId);
    }

    public List<Veiculo> buscarVeiculosAlerta() {
        return veiculoRepository.findByProximaRevisaoIsNotNullOrderByProximaRevisaoAsc();
    }

    public long contarTotal() {
        return veiculoRepository.count();
    }

    public long contarPorStatus(String status) {
        return veiculoRepository.countByStatusIgnoreCase(status);
    }
}
