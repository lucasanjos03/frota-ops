package br.edu.cesmac.frota_ops.tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.usuario.Usuario;
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

    /**
     * Contagem de veículos por tag no escopo do perfil, indexada pelo nome da tag.
     * MOTORISTA recebe apenas a contagem dos próprios veículos; as demais tags ficam
     * ausentes do mapa (ausência = zero veículos no escopo).
     */
    public Map<String, Long> contarVeiculosPorTag(HttpSession session) {
        Long motoristaId = escopoMotoristaId(usuarioService.obterUsuarioLogado(session));

        Map<String, Long> contagens = new LinkedHashMap<>();
        List<TagRepository.ContagemPorTag> resultado = motoristaId != null
                ? tagRepository.contarVeiculosPorTagDoMotorista(motoristaId)
                : tagRepository.contarVeiculosPorTag();
        for (TagRepository.ContagemPorTag contagem : resultado) {
            contagens.put(contagem.getTag(), contagem.getTotal());
        }
        return contagens;
    }

    /** Resumo da tela de tags: totalizadores compactos e contagem por tag no escopo do perfil. */
    public TagPaginaDTO montarResumo(HttpSession session) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        Long motoristaId = escopoMotoristaId(logado);

        Map<String, Long> veiculosPorTag = contarVeiculosPorTag(session);
        long totalTags = tagRepository.count();
        long tagsEmUso = veiculosPorTag.values().stream().filter(total -> total > 0).count();
        long tagsSemVeiculos = Math.max(0, totalTags - tagsEmUso);

        return new TagPaginaDTO(totalTags, tagsEmUso, tagsSemVeiculos,
                motoristaId != null, veiculosPorTag);
    }

    /** Id do motorista vinculado ao usuário logado, ou {@code null} quando o escopo é a frota. */
    private Long escopoMotoristaId(Usuario logado) {
        if (logado != null && logado.isMotorista() && logado.getMotorista() != null) {
            return logado.getMotorista().getId();
        }
        return null;
    }
}
