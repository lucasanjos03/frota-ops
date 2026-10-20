package br.edu.cesmac.frota_ops.motorista;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import jakarta.servlet.http.HttpSession;

@Service
public class MotoristaService {

    private final MotoristaRepository motoristaRepository;
    private final UsuarioService usuarioService;

    public MotoristaService(MotoristaRepository motoristaRepository,
                            UsuarioService usuarioService) {
        this.motoristaRepository = motoristaRepository;
        this.usuarioService = usuarioService;
    }

    public boolean podeCriarOuExcluir(HttpSession session) {
        return usuarioService.obterUsuarioLogado(session).isAdmin();
    }

    public boolean podeEditar(HttpSession session, Long motoristaId) {
        return usuarioService.obterUsuarioLogado(session).podeEditarMotorista(motoristaId);
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
        sincronizarDadosComUsuario(motorista);
        motoristaRepository.save(motorista);
        usuarioService.definirPerfilMotorista(motorista.getUsuario());
    }

    public boolean atualizar(Long id, Motorista dadosAtualizados) {
        Motorista motorista = motoristaRepository.findById(id).orElse(null);
        if (motorista == null) {
            return false;
        }

        motorista.setUsuario(dadosAtualizados.getUsuario());
        motorista.setNome(dadosAtualizados.getNome());
        motorista.setCpf(dadosAtualizados.getCpf());
        motorista.setNumeroCnh(dadosAtualizados.getNumeroCnh());
        motorista.setCategoriaCnh(dadosAtualizados.getCategoriaCnh());
        motorista.setValidadeCnh(dadosAtualizados.getValidadeCnh());
        motorista.setTelefone(dadosAtualizados.getTelefone());
        motorista.setEmail(dadosAtualizados.getEmail());
        motorista.setStatus(dadosAtualizados.getStatus());

        sincronizarDadosComUsuario(motorista);
        motoristaRepository.save(motorista);
        usuarioService.definirPerfilMotorista(motorista.getUsuario());
        return true;
    }

    private void sincronizarDadosComUsuario(Motorista motorista) {
        if (motorista.getUsuario() != null) {
            if (motorista.getNome() == null || motorista.getNome().isBlank()) {
                motorista.setNome(motorista.getUsuario().getNome());
            }
            if (motorista.getEmail() == null || motorista.getEmail().isBlank()) {
                motorista.setEmail(motorista.getUsuario().getEmail());
            }
        }
    }

    public boolean excluir(Long id) {
        if (!motoristaRepository.existsById(id)) {
            return false;
        }
        motoristaRepository.deleteById(id);
        return true;
    }

    public List<Usuario> listarUsuariosDisponiveis(Long usuarioIdAtual) {
        return usuarioService.listarDisponiveisParaMotorista(usuarioIdAtual);
    }

    public long contarTotal() {
        return motoristaRepository.count();
    }
}
