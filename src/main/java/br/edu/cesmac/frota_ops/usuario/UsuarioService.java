package br.edu.cesmac.frota_ops.usuario;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obterUsuarioLogado(HttpSession session) {
        if (session != null) {
            Object attr = session.getAttribute("usuarioLogado");
            if (attr instanceof Usuario u && u.getId() != null) {
                return usuarioRepository.findByIdAndAtivoTrue(u.getId()).orElse(u);
            }
        }
        return new Usuario(null, "admin", "Administrador", "admin@fleetops.com", "", "ADMIN", true);
    }

    public List<Usuario> listarVisiveisPara(Usuario logado) {
        if (logado == null || logado.isAdmin()) {
            return usuarioRepository.findAllByAtivoTrue();
        }
        if (logado.getId() != null) {
            return usuarioRepository.findByIdAndAtivoTrue(logado.getId())
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }
        return Collections.emptyList();
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllByAtivoTrue();
    }

    public List<Usuario> listarDisponiveisParaMotorista(Long usuarioIdAtual) {
        return usuarioRepository.listarDisponiveisParaMotorista(usuarioIdAtual);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findByIdAndAtivoTrue(id);
    }

    public Optional<Usuario> autenticar(String login, String senha) {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            return Optional.empty();
        }
        return usuarioRepository.autenticarUsuarioAtivo(login.trim(), senha);
    }

    public void salvar(Usuario usuario) {
        if (usuario.getUsername() != null) {
            usuario.setUsername(usuario.getUsername().trim().toLowerCase());
        }
        if (usuario.getEmail() != null) {
            usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        }
        if (usuario.getPerfil() == null || usuario.getPerfil().isBlank()) {
            usuario.setPerfil("OPERADOR");
        } else {
            usuario.setPerfil(usuario.getPerfil().trim().toUpperCase());
        }
        usuario.setAtivo(true);
        usuario.setDataModificacao(OffsetDateTime.now());
        usuarioRepository.save(usuario);
    }

    public boolean atualizar(Long id, Usuario dadosAtualizados, Usuario logado) {
        Usuario usuario = usuarioRepository.findByIdAndAtivoTrue(id).orElse(null);
        if (usuario == null) {
            return false;
        }

        if (dadosAtualizados.getUsername() != null) {
            usuario.setUsername(dadosAtualizados.getUsername().trim().toLowerCase());
        }
        usuario.setNome(dadosAtualizados.getNome());
        if (dadosAtualizados.getEmail() != null) {
            usuario.setEmail(dadosAtualizados.getEmail().trim().toLowerCase());
        }
        if (dadosAtualizados.getSenha() != null && !dadosAtualizados.getSenha().isBlank()) {
            usuario.setSenha(dadosAtualizados.getSenha());
        }
        if (logado != null && logado.isAdmin()) {
            if (dadosAtualizados.getPerfil() != null && !dadosAtualizados.getPerfil().isBlank()) {
                usuario.setPerfil(dadosAtualizados.getPerfil().trim().toUpperCase());
            }
        }
        usuario.setDataModificacao(OffsetDateTime.now());

        usuarioRepository.save(usuario);
        return true;
    }

    public void definirPerfilMotorista(Usuario usuario) {
        if (usuario != null && usuario.getId() != null) {
            usuarioRepository.findByIdAndAtivoTrue(usuario.getId()).ifPresent(u -> {
                if (!u.isAdmin()) {
                    u.setPerfil("MOTORISTA");
                    u.setDataModificacao(OffsetDateTime.now());
                    usuarioRepository.save(u);
                }
            });
        }
    }

    /**
     * Soft Delete: marca o usuário como inativo (ativo = false) e registra a data_modificacao.
     */
    public boolean excluir(Long id) {
        Usuario usuario = usuarioRepository.findByIdAndAtivoTrue(id).orElse(null);
        if (usuario == null) {
            return false;
        }
        usuario.setAtivo(false);
        usuario.setDataModificacao(OffsetDateTime.now());
        usuarioRepository.save(usuario);
        return true;
    }

    public long contarTotal() {
        return usuarioRepository.countByAtivoTrue();
    }
}
