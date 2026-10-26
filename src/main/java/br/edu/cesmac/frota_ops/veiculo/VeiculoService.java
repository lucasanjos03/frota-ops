package br.edu.cesmac.frota_ops.veiculo;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /** Janela de antecedência para revisões "próximas" — mesma usada pelo dashboard. */
    public static final int JANELA_REVISAO_DIAS = 15;

    /** Tamanho da página da listagem de veículos. */
    public static final int TAMANHO_PAGINA = 10;

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

    /**
     * Listagem paginada da tela de veículos, já restrita ao escopo do perfil: ADMIN e
     * OPERADOR enxergam a frota completa; MOTORISTA enxerga apenas os veículos alocados ao
     * próprio cadastro. A filtragem (busca, status, situação, revisão e vínculo) ocorre no
     * banco, sobre o mesmo conjunto já escopado.
     *
     * @param pagina número da página, começando em 1.
     */
    public Page<Veiculo> listar(HttpSession session, VeiculoFiltro filtro, int pagina) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        Long motoristaId = escopoMotoristaId(logado);

        // MOTORISTA sem cadastro de motorista vinculado não possui frota para exibir.
        if (logado.isMotorista() && motoristaId == null) {
            return Page.empty();
        }

        LocalDate hoje = LocalDate.now();
        Pageable pageable = PageRequest.of(Math.max(pagina - 1, 0), TAMANHO_PAGINA,
                Sort.by(Sort.Direction.ASC, "id"));

        return veiculoRepository.buscarComFiltros(
                motoristaId,
                buscaNormalizada(filtro.q()),
                textoOuNulo(filtro.status()),
                textoOuNulo(filtro.situacao()),
                textoOuNulo(filtro.revisao()),
                textoOuNulo(filtro.vinculo()),
                hoje,
                hoje.plusDays(JANELA_REVISAO_DIAS),
                pageable);
    }

    /**
     * Indicadores da tela e ranking de quilometragem, ambos no escopo do perfil e
     * independentes dos filtros aplicados à tabela.
     */
    public VeiculoPaginaDTO montarResumo(HttpSession session) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        boolean restrito = logado.isMotorista();
        Long motoristaId = escopoMotoristaId(logado);

        if (restrito && motoristaId == null) {
            return new VeiculoPaginaDTO(
                    "Sem frota vinculada ao seu cadastro", true, JANELA_REVISAO_DIAS,
                    new VeiculoIndicadoresDTO(0, 0, 0, 0, 0, 0, 0, 0), List.of());
        }

        Map<String, Long> porStatus = contarPorStatus(motoristaId);
        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();

        LocalDate hoje = LocalDate.now();
        LocalDate limiteRevisao = hoje.plusDays(JANELA_REVISAO_DIAS);
        long revisoesVencidas = motoristaId == null
                ? veiculoRepository.countByProximaRevisaoBefore(hoje)
                : veiculoRepository.countByMotoristaIdAndProximaRevisaoBefore(motoristaId, hoje);
        long revisoesProximas = motoristaId == null
                ? veiculoRepository.countByProximaRevisaoBetween(hoje, limiteRevisao)
                : veiculoRepository.countByMotoristaIdAndProximaRevisaoBetween(motoristaId, hoje, limiteRevisao);
        // Sem motorista atribuído só é um indicador relevante na visão de frota completa.
        long semMotorista = restrito ? 0 : veiculoRepository.countByMotoristaIsNull();

        return new VeiculoPaginaDTO(
                restrito ? "Sua frota (" + total + " veículo(s))" : "Frota completa",
                restrito,
                JANELA_REVISAO_DIAS,
                new VeiculoIndicadoresDTO(total,
                        porStatus.getOrDefault("Disponível", 0L),
                        porStatus.getOrDefault("Em trânsito", 0L),
                        porStatus.getOrDefault("Em manutenção", 0L),
                        porStatus.getOrDefault("Indisponível", 0L),
                        revisoesVencidas,
                        revisoesProximas,
                        semMotorista),
                listarMaioresQuilometragens(motoristaId));
    }

    private Long escopoMotoristaId(Usuario logado) {
        if (!logado.isMotorista() || logado.getMotorista() == null) {
            return null;
        }
        return logado.getMotorista().getId();
    }

    private Map<String, Long> contarPorStatus(Long motoristaId) {
        List<VeiculoRepository.ContagemPorStatus> contagens = motoristaId == null
                ? veiculoRepository.contarAgrupadoPorStatus()
                : veiculoRepository.contarAgrupadoPorStatusDoMotorista(motoristaId);

        Map<String, Long> resultado = new LinkedHashMap<>();
        for (VeiculoRepository.ContagemPorStatus contagem : contagens) {
            resultado.merge(contagem.getStatus(), contagem.getTotal(), Long::sum);
        }
        return resultado;
    }

    private List<VeiculoQuilometragemDTO> listarMaioresQuilometragens(Long motoristaId) {
        List<Veiculo> veiculos = motoristaId == null
                ? veiculoRepository.findTop5ByOrderByQuilometragemDesc()
                : veiculoRepository.findTop5ByMotoristaIdOrderByQuilometragemDesc(motoristaId);

        return veiculos.stream()
                .map(v -> new VeiculoQuilometragemDTO(
                        v.getId(),
                        v.getPlaca(),
                        v.getModelo(),
                        v.getMotorista() != null ? v.getMotorista().getNome() : null,
                        v.getQuilometragem() != null ? v.getQuilometragem() : 0L))
                .collect(Collectors.toList());
    }

    private String buscaNormalizada(String q) {
        return q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
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

    /**
     * Situação de manutenção derivada do veículo — mesma regra reproduzida na consulta
     * filtrada por situação ({@link VeiculoRepository#buscarComFiltros}).
     */
    private String calcularSituacaoManutencao(Veiculo veiculo) {
        if ("Em manutenção".equalsIgnoreCase(veiculo.getStatus())) {
            return "Em manutenção";
        }
        if (veiculo.getProximaRevisao() != null
                && !veiculo.getProximaRevisao().isAfter(LocalDate.now().plusDays(JANELA_REVISAO_DIAS))) {
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
}
