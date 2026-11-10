package br.edu.cesmac.frota_ops.usuario;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Codificador de senhas (BCrypt). Senhas novas são gravadas como hash; senhas legadas em
     * texto plano são reconhecidas no login e convertidas para hash no primeiro acesso.
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Usuário autenticado da sessão, ou {@code null} quando não há login ativo.
     *
     * <p>Não existe fallback para um usuário padrão: o acesso às rotas protegidas é
     * garantido pelo {@link br.edu.cesmac.frota_ops.auth.AuthInterceptor}, que redireciona
     * visitantes não autenticados para o login antes que qualquer controller seja executado.</p>
     */
    public Usuario obterUsuarioLogado(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object attr = session.getAttribute("usuarioLogado");
        if (attr instanceof Usuario u && u.getId() != null) {
            return usuarioRepository.findByIdAndAtivoTrue(u.getId()).orElse(u);
        }
        return null;
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
        return usuarioRepository.findAllByAtivoTrue().stream()
                .filter(u -> u.getMotorista() == null
                        || (usuarioIdAtual != null && u.getId().equals(usuarioIdAtual)))
                .collect(Collectors.toList());
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findByIdAndAtivoTrue(id);
    }

    public Optional<Usuario> autenticar(String login, String senha) {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            return Optional.empty();
        }
        String identificador = login.trim();
        Optional<Usuario> encontrado = usuarioRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identificador, identificador)
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()));
        if (encontrado.isEmpty() || !senhaConfere(senha, encontrado.get())) {
            return Optional.empty();
        }

        Usuario usuario = encontrado.get();
        converterSenhaLegadaParaHash(usuario, senha);
        return Optional.of(usuario);
    }

    /**
     * Confere a senha informada contra a armazenada. Valores no formato BCrypt ({@code $2...})
     * são verificados pelo codificador; valores em texto plano (base legada) são comparados
     * diretamente.
     */
    private boolean senhaConfere(String senhaInformada, Usuario usuario) {
        String armazenada = usuario.getSenha();
        if (armazenada == null) {
            return false;
        }
        return ehHashBcrypt(armazenada)
                ? passwordEncoder.matches(senhaInformada, armazenada)
                : senhaInformada.equals(armazenada);
    }

    /**
     * Upgrade transparente: quando a senha armazenada ainda está em texto plano e o login
     * confere, o valor é regravado como hash BCrypt. Assim as bases legadas migram sozinhas,
     * a cada primeiro acesso, sem migração de dados dedicada.
     */
    private void converterSenhaLegadaParaHash(Usuario usuario, String senhaInformada) {
        String armazenada = usuario.getSenha();
        if (armazenada != null && !ehHashBcrypt(armazenada)) {
            usuario.setSenha(passwordEncoder.encode(senhaInformada));
            usuario.setDataModificacao(OffsetDateTime.now());
            usuarioRepository.save(usuario);
        }
    }

    /** Reconhece o formato de hash BCrypt ({@code $2a$}, {@code $2b$} ou {@code $2y$}). */
    private boolean ehHashBcrypt(String valor) {
        return valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$");
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
        if (usuario.getSenha() != null && !usuario.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
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
            usuario.setSenha(passwordEncoder.encode(dadosAtualizados.getSenha()));
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

    /**
     * Resumo de usuários ativos por perfil. Destinado à visão ADMIN; a contagem considera
     * toda a base ativa, independentemente do recorte de visibilidade da listagem.
     */
    public UsuarioPaginaDTO montarResumo() {
        long total = 0;
        long administradores = 0;
        long operadores = 0;
        long motoristas = 0;

        for (UsuarioRepository.ContagemPorPerfil contagem : usuarioRepository.contarAtivosPorPerfil()) {
            long quantidade = contagem.getTotal();
            total += quantidade;
            String perfil = contagem.getPerfil() == null ? "" : contagem.getPerfil().trim().toUpperCase();
            switch (perfil) {
                case "ADMIN" -> administradores = quantidade;
                case "OPERADOR" -> operadores = quantidade;
                case "MOTORISTA" -> motoristas = quantidade;
                default -> {
                    // Perfis não mapeados entram apenas no total.
                }
            }
        }

        return new UsuarioPaginaDTO(total, administradores, operadores, motoristas);
    }
}
