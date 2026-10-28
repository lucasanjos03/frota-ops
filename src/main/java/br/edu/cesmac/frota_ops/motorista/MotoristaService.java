package br.edu.cesmac.frota_ops.motorista;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoRepository;
import jakarta.servlet.http.HttpSession;

@Service
public class MotoristaService {

    /** Janela de antecedência para CNHs "a vencer" na tela de motoristas. */
    public static final int JANELA_CNH_DIAS = 30;

    private final MotoristaRepository motoristaRepository;
    private final UsuarioService usuarioService;
    private final VeiculoRepository veiculoRepository;

    public MotoristaService(MotoristaRepository motoristaRepository,
                            UsuarioService usuarioService,
                            VeiculoRepository veiculoRepository) {
        this.motoristaRepository = motoristaRepository;
        this.usuarioService = usuarioService;
        this.veiculoRepository = veiculoRepository;
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

    /** Listagem filtrada usando a data corrente como referência. */
    public List<Motorista> listar(MotoristaFiltro filtro) {
        return listar(filtro, LocalDate.now());
    }

    /**
     * Listagem filtrada com data de referência explícita. A data é parâmetro (e não
     * {@code LocalDate.now()} interno) para que classificação de CNH e testes compartilhem
     * exatamente a mesma noção de "hoje".
     */
    public List<Motorista> listar(MotoristaFiltro filtro, LocalDate referencia) {
        return motoristaRepository.buscarComFiltros(
                buscaNormalizada(filtro.q()),
                textoOuNulo(filtro.status()),
                textoOuNulo(filtro.categoria()),
                textoOuNulo(filtro.cnh()),
                textoOuNulo(filtro.vinculo()),
                referencia,
                referencia.plusDays(JANELA_CNH_DIAS));
    }

    /** Categorias de CNH existentes no cadastro, para as opções do filtro. */
    public List<String> listarCategorias() {
        return motoristaRepository.listarCategorias();
    }

    /** Indicadores, distribuição por categoria e documentos em atenção na data corrente. */
    public MotoristaPaginaDTO montarResumo() {
        return montarResumo(LocalDate.now());
    }

    /**
     * Resumo analítico da tela na data de referência informada.
     *
     * <p>Os cartões e o painel de documentos <strong>não</strong> são afetados pelos filtros
     * da tabela: descrevem sempre a equipe completa.</p>
     */
    public MotoristaPaginaDTO montarResumo(LocalDate referencia) {
        Map<String, Long> porStatus = contarPorStatus();
        long total = porStatus.values().stream().mapToLong(Long::longValue).sum();
        LocalDate limiteCnh = referencia.plusDays(JANELA_CNH_DIAS);

        MotoristaIndicadoresDTO indicadores = new MotoristaIndicadoresDTO(
                total,
                porStatus.getOrDefault("Ativo", 0L),
                porStatus.getOrDefault("Inativo", 0L),
                motoristaRepository.countByValidadeCnhBefore(referencia),
                motoristaRepository.countByValidadeCnhBetween(referencia, limiteCnh),
                motoristaRepository.contarSemVeiculo());

        return new MotoristaPaginaDTO(
                JANELA_CNH_DIAS,
                referencia,
                indicadores,
                listarPorCategoria(),
                listarDocumentosEmAtencao(referencia, limiteCnh));
    }

    /** Veículos atribuídos ao motorista — a relação real é 1:N (um motorista, N veículos). */
    public List<Veiculo> listarVeiculosDoMotorista(Long motoristaId) {
        return veiculoRepository.findByMotoristaIdOrderByPlacaAsc(motoristaId);
    }

    private Map<String, Long> contarPorStatus() {
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (MotoristaRepository.ContagemPorStatus contagem : motoristaRepository.contarAgrupadoPorStatus()) {
            resultado.merge(contagem.getStatus(), contagem.getTotal(), Long::sum);
        }
        return resultado;
    }

    private List<MotoristaCategoriaDTO> listarPorCategoria() {
        return motoristaRepository.contarAgrupadoPorCategoria().stream()
                .map(contagem -> new MotoristaCategoriaDTO(contagem.getCategoria(), contagem.getTotal()))
                .collect(Collectors.toList());
    }

    /**
     * CNHs vencidas ou dentro da janela de antecedência, da mais crítica para a menos
     * crítica. O vencimento no próprio dia de referência é tratado como "A vencer" (a CNH
     * vale até a data impressa).
     */
    private List<MotoristaDocumentoDTO> listarDocumentosEmAtencao(LocalDate referencia, LocalDate limiteCnh) {
        return motoristaRepository.findByValidadeCnhLessThanEqualOrderByValidadeCnhAsc(limiteCnh).stream()
                .filter(motorista -> motorista.getValidadeCnh() != null)
                .map(motorista -> {
                    long dias = ChronoUnit.DAYS.between(referencia, motorista.getValidadeCnh());
                    return new MotoristaDocumentoDTO(
                            motorista.getId(),
                            motorista.getNome(),
                            motorista.getNumeroCnh(),
                            motorista.getValidadeCnh(),
                            dias,
                            dias < 0 ? "Vencida" : "A vencer");
                })
                .collect(Collectors.toList());
    }

    private String buscaNormalizada(String q) {
        return q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
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
