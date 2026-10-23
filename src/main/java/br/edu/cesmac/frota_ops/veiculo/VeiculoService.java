package br.edu.cesmac.frota_ops.veiculo;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.motorista.Motorista;
import br.edu.cesmac.frota_ops.motorista.MotoristaService;
import br.edu.cesmac.frota_ops.tag.Tag;
import br.edu.cesmac.frota_ops.tag.TagService;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import jakarta.servlet.http.HttpSession;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final MotoristaService motoristaService;
    private final TagService tagService;
    private final UsuarioService usuarioService;

    public VeiculoService(VeiculoRepository veiculoRepository,
                          MotoristaService motoristaService,
                          TagService tagService,
                          UsuarioService usuarioService) {
        this.veiculoRepository = veiculoRepository;
        this.motoristaService = motoristaService;
        this.tagService = tagService;
        this.usuarioService = usuarioService;
    }

    public boolean podeCriarOuExcluir(HttpSession session) {
        return usuarioService.obterUsuarioLogado(session).isAdmin();
    }

    public boolean podeEditar(HttpSession session, Veiculo veiculo) {
        if (veiculo == null) {
            return false;
        }
        Long motoristaId = veiculo.getMotorista() != null ? veiculo.getMotorista().getId() : null;
        return usuarioService.obterUsuarioLogado(session).podeEditarVeiculo(motoristaId);
    }

    public List<Veiculo> listarVeiculosGerenciaveis(HttpSession session) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        if (logado.isAdmin()) {
            return veiculoRepository.findAll();
        }
        if (logado.isMotorista() && logado.getMotorista() != null && logado.getMotorista().getId() != null) {
            Long meuMotoristaId = logado.getMotorista().getId();
            return veiculoRepository.findAll().stream()
                    .filter(v -> v.getMotorista() != null && meuMotoristaId.equals(v.getMotorista().getId()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public List<Veiculo> listarTodos() {
        return veiculoRepository.findAll();
    }

    public List<Veiculo> filtrar(String q, String status) {
        List<Veiculo> filtrados = veiculoRepository.findAll();

        if (q != null && !q.isBlank()) {
            String busca = q.toLowerCase();
            filtrados = filtrados.stream()
                    .filter(v -> (v.getPlaca() != null && v.getPlaca().toLowerCase().contains(busca))
                            || (v.getModelo() != null && v.getModelo().toLowerCase().contains(busca))
                            || (v.getMotorista() != null && v.getMotorista().getNome().toLowerCase().contains(busca)))
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
        return veiculoRepository.findById(id);
    }

    public void salvar(Veiculo veiculo) {
        if (veiculo.getPlaca() != null) {
            veiculo.setPlaca(veiculo.getPlaca().trim().toUpperCase());
        }
        if (veiculo.getStatus() == null || veiculo.getStatus().isBlank()) {
            veiculo.setStatus("Disponível");
        }
        if (veiculo.getUltimaRevisao() != null && veiculo.getQuilometragemUltimaRevisao() == null) {
            veiculo.setQuilometragemUltimaRevisao(veiculo.getQuilometragem());
        }
        veiculo.setSituacaoManutencao(calcularSituacaoManutencao(veiculo));

        veiculoRepository.save(veiculo);
    }

    public boolean atualizar(Long id, Veiculo dadosAtualizados, HttpSession session) {
        Veiculo veiculo = veiculoRepository.findById(id).orElse(null);
        if (veiculo == null) {
            return false;
        }

        Usuario logado = usuarioService.obterUsuarioLogado(session);

        if (dadosAtualizados.getPlaca() != null) {
            veiculo.setPlaca(dadosAtualizados.getPlaca().trim().toUpperCase());
        }
        veiculo.setModelo(dadosAtualizados.getModelo());
        veiculo.setAno(dadosAtualizados.getAno());
        veiculo.setQuilometragem(dadosAtualizados.getQuilometragem());
        veiculo.setStatus(dadosAtualizados.getStatus());
        veiculo.setUltimaRevisao(dadosAtualizados.getUltimaRevisao());
        veiculo.setProximaRevisao(dadosAtualizados.getProximaRevisao());
        veiculo.setObservacoes(dadosAtualizados.getObservacoes());
        if (logado.isAdmin()) {
            veiculo.setMotorista(dadosAtualizados.getMotorista());
        }
        veiculo.setTags(dadosAtualizados.getTags());

        if (dadosAtualizados.getQuilometragemUltimaRevisao() != null) {
            veiculo.setQuilometragemUltimaRevisao(dadosAtualizados.getQuilometragemUltimaRevisao());
        } else if (veiculo.getUltimaRevisao() != null && veiculo.getQuilometragemUltimaRevisao() == null) {
            veiculo.setQuilometragemUltimaRevisao(veiculo.getQuilometragem());
        }

        veiculo.setSituacaoManutencao(calcularSituacaoManutencao(veiculo));

        veiculoRepository.save(veiculo);
        return true;
    }

    public boolean excluir(Long id) {
        if (!veiculoRepository.existsById(id)) {
            return false;
        }
        veiculoRepository.deleteById(id);
        return true;
    }

    public void atualizarAposManutencao(Veiculo veiculo, LocalDate dataManutencao, Long kmManutencao) {
        if (veiculo.getUltimaRevisao() == null || !dataManutencao.isBefore(veiculo.getUltimaRevisao())) {
            veiculo.setUltimaRevisao(dataManutencao);
            veiculo.setQuilometragemUltimaRevisao(kmManutencao);
        }
        if (kmManutencao != null && kmManutencao > veiculo.getQuilometragem()) {
            veiculo.setQuilometragem(kmManutencao);
        }
        veiculo.setSituacaoManutencao(calcularSituacaoManutencao(veiculo));
        veiculoRepository.save(veiculo);
    }

    private String calcularSituacaoManutencao(Veiculo veiculo) {
        if ("Em manutenção".equalsIgnoreCase(veiculo.getStatus())) {
            return "Em manutenção";
        }
        if (veiculo.getProximaRevisao() != null
                && !veiculo.getProximaRevisao().isAfter(LocalDate.now().plusDays(15))) {
            return "Atenção";
        }
        return "Em dia";
    }

    public List<Motorista> listarMotoristas() {
        return motoristaService.listarTodos();
    }

    public List<Tag> listarTags() {
        return tagService.listarTodas();
    }

    public long contarTotal() {
        return veiculoRepository.count();
    }
}
