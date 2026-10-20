package br.edu.cesmac.frota_ops.tag;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import jakarta.servlet.http.HttpSession;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final UsuarioService usuarioService;

    public TagService(TagRepository tagRepository,
                      UsuarioService usuarioService) {
        this.tagRepository = tagRepository;
        this.usuarioService = usuarioService;
    }

    public boolean podeGerenciar(HttpSession session) {
        return usuarioService.obterUsuarioLogado(session).podeGerenciarTags();
    }

    public boolean podeExcluir(HttpSession session) {
        return usuarioService.obterUsuarioLogado(session).isAdmin();
    }

    public List<Tag> listarTodas() {
        return tagRepository.findAll();
    }

    public Optional<Tag> buscarPorId(Long id) {
        return tagRepository.findById(id);
    }

    public void salvar(Tag tag) {
        if (tag.getNome() != null) {
            tag.setNome(tag.getNome().trim());
        }
        tagRepository.save(tag);
    }

    public boolean atualizar(Long id, Tag dadosAtualizados) {
        Tag tag = tagRepository.findById(id).orElse(null);
        if (tag == null) {
            return false;
        }

        if (dadosAtualizados.getNome() != null) {
            tag.setNome(dadosAtualizados.getNome().trim());
        }

        tagRepository.save(tag);
        return true;
    }

    public boolean excluir(Long id) {
        if (!tagRepository.existsById(id)) {
            return false;
        }
        tagRepository.deleteById(id);
        return true;
    }

    public long contarTotal() {
        return tagRepository.count();
    }
}
