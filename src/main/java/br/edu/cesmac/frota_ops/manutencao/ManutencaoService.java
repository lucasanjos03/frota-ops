package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.home.dashboard.RevisaoVeiculoDTO;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoRepository;
import br.edu.cesmac.frota_ops.veiculo.VeiculoService;
import jakarta.servlet.http.HttpSession;

/**
 * Serviço do domínio de manutenções: CRUD, histórico filtrado e o resumo analítico da tela.
 *
 * <p>As agregações do resumo são feitas no banco (COUNT/GROUP BY) e expostas em DTOs
 * próprios, no mesmo padrão do dashboard principal.</p>
 *
 * <h2>Escopo por perfil</h2>
 * <ul>
 *   <li><b>ADMIN</b> e <b>OPERADOR</b>: frota completa.</li>
 *   <li><b>MOTORISTA</b>: apenas os veículos vinculados ao próprio cadastro — histórico,
 *       série mensal, ranking e painel de revisões seguem o mesmo recorte.</li>
 * </ul>
 *
 * <h2>Separação entre histórico e previsão</h2>
 * <p>O histórico conta manutenções <em>já realizadas</em>, por {@code data_manutencao}. O
 * painel de revisões é <em>previsão</em>, derivado de {@code veiculo.proxima_revisao}. As
 * duas séries nunca são somadas: aparecem em cartões/listas distintos.</p>
 */
@Service
public class ManutencaoService {

    /** Meses exibidos no gráfico de manutenções realizadas (mês atual + 11 anteriores). */
    public static final int MESES_GRAFICO = 12;

    /** Janela de antecedência para revisões previstas — mesma usada por VeiculoService. */
    public static final int JANELA_REVISAO_DIAS = VeiculoService.JANELA_REVISAO_DIAS;

    /** Quantidade de veículos exibidos no ranking de registros de manutenção. */
    public static final int LIMITE_RANKING = 5;

    private static final String[] MESES_ABREVIADOS = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};

    private final ManutencaoRepository manutencaoRepository;
    private final VeiculoRepository veiculoRepository;
    private final VeiculoService veiculoService;
    private final UsuarioService usuarioService;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             VeiculoRepository veiculoRepository,
                             VeiculoService veiculoService,
                             UsuarioService usuarioService) {
        this.manutencaoRepository = manutencaoRepository;
        this.veiculoRepository = veiculoRepository;
        this.veiculoService = veiculoService;
        this.usuarioService = usuarioService;
    }

    // ------------------------------------------------------------------
    // Permissões (inalteradas)
    // ------------------------------------------------------------------

    public boolean podeCriar(HttpSession session) {
        return !veiculoService.listarVeiculosGerenciaveis(session).isEmpty()
                || veiculoService.podeCriarOuExcluir(session);
    }

    public boolean podeGerenciarVeiculo(HttpSession session, Veiculo veiculo) {
        return veiculoService.podeEditar(session, veiculo);
    }

    public boolean podeEditarManutencao(HttpSession session, Manutencao manutencao) {
        if (manutencao == null || manutencao.getVeiculo() == null) {
            return false;
        }
        return veiculoService.podeEditar(session, manutencao.getVeiculo());
    }

    public boolean podeExcluir(HttpSession session, Manutencao manutencao) {
        return veiculoService.podeCriarOuExcluir(session) || podeEditarManutencao(session, manutencao);
    }

    // ------------------------------------------------------------------
    // Histórico filtrado
    // ------------------------------------------------------------------

    /** Histórico no escopo do perfil, já recortado pelos filtros da tabela. */
    public List<Manutencao> listar(HttpSession session, ManutencaoFiltro filtro) {
        return listar(usuarioService.obterUsuarioLogado(session), filtro);
    }

    /** Sobrecarga usada nos testes e por quem já tem o usuário resolvido. */
    public List<Manutencao> listar(Usuario logado, ManutencaoFiltro filtro) {
        Long motoristaId = escopoMotoristaId(logado);

        // MOTORISTA sem cadastro de motorista vinculado não possui histórico visível.
        if (ehMotorista(logado) && motoristaId == null) {
            return List.of();
        }

        return manutencaoRepository.buscarComFiltros(
                motoristaId,
                filtro.veiculoId(),
                buscaNormalizada(filtro.servico()),
                filtro.de(),
                filtro.ate());
    }

    public List<Manutencao> buscarPorVeiculo(Long veiculoId) {
        return manutencaoRepository.findByVeiculoIdOrderByDataDesc(veiculoId);
    }

    public Optional<Manutencao> buscarPorId(Long id) {
        return manutencaoRepository.findById(id);
    }

    // ------------------------------------------------------------------
    // Resumo analítico
    // ------------------------------------------------------------------

    /** Resumo da tela usando a data corrente como referência. */
    public ManutencaoPaginaDTO montarResumo(HttpSession session) {
        return montarResumo(session, LocalDate.now());
    }

    /**
     * Resumo da tela com data de referência explícita. O controlador calcula a data uma única
     * vez e a repassa, de modo que indicadores, série mensal, painel de revisões e histórico
     * filtrado compartilhem exatamente a mesma noção de "hoje".
     */
    public ManutencaoPaginaDTO montarResumo(HttpSession session, LocalDate referencia) {
        return montarResumo(usuarioService.obterUsuarioLogado(session), referencia);
    }

    /**
     * Resumo analítico na data de referência informada.
     *
     * <p>Indicadores, série mensal, ranking e painel de revisões <strong>não</strong> são
     * afetados pelos filtros da tabela: descrevem sempre o escopo completo do perfil.</p>
     */
    public ManutencaoPaginaDTO montarResumo(Usuario logado, LocalDate referencia) {
        boolean restrito = ehMotorista(logado);
        Long motoristaId = escopoMotoristaId(logado);

        if (restrito && motoristaId == null) {
            return escopoVazio(referencia);
        }

        LocalDate inicioMes = referencia.withDayOfMonth(1);
        long totalRegistros = manutencaoRepository.contarEscopado(motoristaId);
        long registrosNoMes = manutencaoRepository.contarNoIntervalo(inicioMes, referencia, motoristaId);

        List<RevisaoVeiculoDTO> atrasadas = new ArrayList<>();
        List<RevisaoVeiculoDTO> previstas = new ArrayList<>();
        separarRevisoes(motoristaId, referencia, referencia.plusDays(JANELA_REVISAO_DIAS), atrasadas, previstas);

        ManutencaoIndicadoresDTO indicadores = new ManutencaoIndicadoresDTO(
                totalRegistros, registrosNoMes, atrasadas.size(), previstas.size());

        String escopoDescricao = restrito
                ? "Sua frota (" + veiculoRepository.countByMotoristaId(motoristaId) + " veículo(s))"
                : "Frota completa";

        return new ManutencaoPaginaDTO(
                escopoDescricao,
                restrito,
                referencia,
                inicioMes,
                MESES_GRAFICO,
                JANELA_REVISAO_DIAS,
                indicadores,
                montarSerieMensal(motoristaId, referencia),
                listarRanking(motoristaId),
                atrasadas,
                previstas);
    }

    private ManutencaoPaginaDTO escopoVazio(LocalDate referencia) {
        return new ManutencaoPaginaDTO(
                "Sem veículos vinculados ao seu cadastro",
                true,
                referencia,
                referencia.withDayOfMonth(1),
                MESES_GRAFICO,
                JANELA_REVISAO_DIAS,
                new ManutencaoIndicadoresDTO(0, 0, 0, 0),
                construirBucketsMensais(Map.of(), referencia),
                List.of(),
                List.of(),
                List.of());
    }

    /**
     * Série dos últimos {@value #MESES_GRAFICO} meses. Meses sem registro entram com zero —
     * o eixo temporal é contínuo, não uma lista apenas dos meses que tiveram manutenção.
     */
    private List<ManutencaoMesDTO> montarSerieMensal(Long motoristaId, LocalDate referencia) {
        LocalDate inicio = YearMonth.from(referencia).minusMonths(MESES_GRAFICO - 1).atDay(1);
        List<ManutencaoRepository.ContagemMensal> contagens = motoristaId == null
                ? manutencaoRepository.contarPorMes(inicio, referencia)
                : manutencaoRepository.contarPorMesDoMotorista(inicio, referencia, motoristaId);

        Map<String, Long> porMes = contagens.stream()
                .collect(Collectors.toMap(ManutencaoRepository.ContagemMensal::getMes,
                        ManutencaoRepository.ContagemMensal::getTotal, Long::sum, LinkedHashMap::new));
        return construirBucketsMensais(porMes, referencia);
    }

    private List<ManutencaoMesDTO> construirBucketsMensais(Map<String, Long> porMes, LocalDate referencia) {
        List<ManutencaoMesDTO> serie = new ArrayList<>();
        for (int i = MESES_GRAFICO - 1; i >= 0; i--) {
            YearMonth mes = YearMonth.from(referencia).minusMonths(i);
            serie.add(new ManutencaoMesDTO(
                    mes.toString(),
                    rotuloDe(mes),
                    porMes.getOrDefault(mes.toString(), 0L),
                    mes.atDay(1),
                    mes.atEndOfMonth()));
        }
        return serie;
    }

    private String rotuloDe(YearMonth mes) {
        return MESES_ABREVIADOS[mes.getMonthValue() - 1] + "/"
                + String.format("%02d", Math.floorMod(mes.getYear(), 100));
    }

    private List<ManutencaoRankingVeiculoDTO> listarRanking(Long motoristaId) {
        return manutencaoRepository
                .contarAgrupadoPorVeiculo(motoristaId, PageRequest.of(0, LIMITE_RANKING))
                .stream()
                .map(contagem -> new ManutencaoRankingVeiculoDTO(
                        contagem.getVeiculoId(),
                        contagem.getPlaca(),
                        contagem.getModelo(),
                        contagem.getMotorista(),
                        contagem.getTotal()))
                .collect(Collectors.toList());
    }

    /**
     * Separa as revisões do cadastro de veículos em duas séries independentes: atrasadas
     * (data prevista anterior à referência) e previstas (da referência até a janela de
     * antecedência). Revisões agendadas além da janela não entram em nenhuma das listas.
     */
    private void separarRevisoes(Long motoristaId, LocalDate referencia, LocalDate limite,
                                 List<RevisaoVeiculoDTO> atrasadas, List<RevisaoVeiculoDTO> previstas) {
        List<Veiculo> veiculos = motoristaId == null
                ? veiculoRepository.findByProximaRevisaoIsNotNullOrderByProximaRevisaoAsc()
                : veiculoRepository.findByMotoristaIdAndProximaRevisaoIsNotNullOrderByProximaRevisaoAsc(motoristaId);

        for (Veiculo veiculo : veiculos) {
            LocalDate proxima = veiculo.getProximaRevisao();
            if (proxima == null) {
                continue;
            }
            if (proxima.isBefore(referencia)) {
                atrasadas.add(paraRevisaoDTO(veiculo, referencia));
            } else if (!proxima.isAfter(limite)) {
                previstas.add(paraRevisaoDTO(veiculo, referencia));
            }
        }
    }

    private RevisaoVeiculoDTO paraRevisaoDTO(Veiculo veiculo, LocalDate referencia) {
        long diasRestantes = ChronoUnit.DAYS.between(referencia, veiculo.getProximaRevisao());
        return new RevisaoVeiculoDTO(
                veiculo.getId(),
                veiculo.getPlaca(),
                veiculo.getModelo(),
                veiculo.getMotorista() != null ? veiculo.getMotorista().getNome() : null,
                veiculo.getProximaRevisao(),
                diasRestantes,
                diasRestantes < 0 ? "Vencida" : "Próxima");
    }

    // ------------------------------------------------------------------
    // Veículos (formulários e filtros)
    // ------------------------------------------------------------------

    /**
     * Veículos visíveis na tela de manutenções: ADMIN e OPERADOR enxergam a frota completa
     * (mesmo recorte do histórico) e MOTORISTA apenas os veículos do próprio cadastro.
     */
    public List<Veiculo> listarVeiculosNoEscopo(HttpSession session) {
        return listarVeiculosNoEscopo(usuarioService.obterUsuarioLogado(session));
    }

    public List<Veiculo> listarVeiculosNoEscopo(Usuario logado) {
        Long motoristaId = escopoMotoristaId(logado);
        if (ehMotorista(logado)) {
            return motoristaId == null
                    ? List.of()
                    : veiculoRepository.findByMotoristaIdOrderByPlacaAsc(motoristaId);
        }
        return veiculoRepository.findAll();
    }

    /** Veículos que o usuário pode registrar/editar em manutenção — regra do formulário. */
    public List<Veiculo> listarVeiculosPermitidos(HttpSession session) {
        return veiculoService.listarVeiculosGerenciaveis(session);
    }

    // ------------------------------------------------------------------
    // CRUD (inalterado)
    // ------------------------------------------------------------------

    public Manutencao criarNovaManutencao(Long veiculoId) {
        Manutencao manutencao = new Manutencao();
        manutencao.setData(LocalDate.now());
        if (veiculoId != null) {
            veiculoService.buscarPorId(veiculoId).ifPresent(veiculo -> {
                manutencao.setVeiculo(veiculo);
                manutencao.setQuilometragem(veiculo.getQuilometragem());
            });
        }
        return manutencao;
    }

    public void salvar(Manutencao manutencao) {
        manutencao.setId(null);
        manutencaoRepository.save(manutencao);

        if (manutencao.getVeiculo() != null) {
            veiculoService.atualizarAposManutencao(
                    manutencao.getVeiculo(),
                    manutencao.getData(),
                    manutencao.getQuilometragem()
            );
        }
    }

    public boolean atualizar(Long id, Manutencao dadosAtualizados) {
        Manutencao manutencao = manutencaoRepository.findById(id).orElse(null);
        if (manutencao == null) {
            return false;
        }

        manutencao.setVeiculo(dadosAtualizados.getVeiculo());
        manutencao.setData(dadosAtualizados.getData());
        manutencao.setQuilometragem(dadosAtualizados.getQuilometragem());
        manutencao.setServicoRealizado(dadosAtualizados.getServicoRealizado());
        manutencao.setResponsavel(dadosAtualizados.getResponsavel());
        manutencao.setObservacao(dadosAtualizados.getObservacao());

        manutencaoRepository.save(manutencao);

        if (manutencao.getVeiculo() != null) {
            veiculoService.atualizarAposManutencao(
                    manutencao.getVeiculo(),
                    manutencao.getData(),
                    manutencao.getQuilometragem()
            );
        }
        return true;
    }

    public boolean excluir(Long id) {
        if (!manutencaoRepository.existsById(id)) {
            return false;
        }
        manutencaoRepository.deleteById(id);
        return true;
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private boolean ehMotorista(Usuario logado) {
        return logado != null && logado.isMotorista();
    }

    private Long escopoMotoristaId(Usuario logado) {
        if (!ehMotorista(logado) || logado.getMotorista() == null) {
            return null;
        }
        return logado.getMotorista().getId();
    }

    private String buscaNormalizada(String texto) {
        return texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
    }
}
