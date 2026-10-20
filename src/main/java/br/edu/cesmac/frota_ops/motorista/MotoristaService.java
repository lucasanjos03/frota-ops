package br.edu.cesmac.frota_ops.motorista;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class MotoristaService {

    private final MotoristaRepository motoristaRepository;

    public MotoristaService(MotoristaRepository motoristaRepository) {
        this.motoristaRepository = motoristaRepository;
    }

    public List<Motorista> listarTodos() {
        return motoristaRepository.findAll();
    }

    public Optional<Motorista> buscarPorId(Long id) {
        return motoristaRepository.findById(id);
    }

    public void salvar(Motorista motorista) {
        if (motorista.getStatus() == null || motorista.getStatus().isBlank()) {
            motorista.setStatus("Ativo");
        }
        motoristaRepository.save(motorista);
    }

    public boolean atualizar(Long id, Motorista dadosAtualizados) {
        Motorista motorista = motoristaRepository.findById(id).orElse(null);
        if (motorista == null) {
            return false;
        }

        motorista.setNome(dadosAtualizados.getNome());
        motorista.setCpf(dadosAtualizados.getCpf());
        motorista.setNumeroCnh(dadosAtualizados.getNumeroCnh());
        motorista.setCategoriaCnh(dadosAtualizados.getCategoriaCnh());
        motorista.setValidadeCnh(dadosAtualizados.getValidadeCnh());
        motorista.setTelefone(dadosAtualizados.getTelefone());
        motorista.setEmail(dadosAtualizados.getEmail());
        motorista.setStatus(dadosAtualizados.getStatus());

        motoristaRepository.save(motorista);
        return true;
    }

    public boolean excluir(Long id) {
        if (!motoristaRepository.existsById(id)) {
            return false;
        }
        motoristaRepository.deleteById(id);
        return true;
    }

    public long contarTotal() {
        return motoristaRepository.count();
    }
}
