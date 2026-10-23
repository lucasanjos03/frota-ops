package br.edu.cesmac.frota_ops.home.dashboard;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.edu.cesmac.frota_ops.manutencao.ManutencaoRepository;
import br.edu.cesmac.frota_ops.motorista.Motorista;
import br.edu.cesmac.frota_ops.motorista.MotoristaRepository;
import br.edu.cesmac.frota_ops.tag.TagRepository;
import br.edu.cesmac.frota_ops.usuario.Usuario;
import br.edu.cesmac.frota_ops.usuario.UsuarioService;
import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import br.edu.cesmac.frota_ops.veiculo.VeiculoRepository;
import jakarta.servlet.http.HttpSession;

/**
 * Serviço de estatísticas do dashboard operacional.
 *
 * <p>Todas as agregações são feitas no banco (COUNT/GROUP BY) e expostas em DTOs próprios,
 * evitando carregar tabelas inteiras e evitando serializar entidades JPA para a view.</p>
 *
 * <h2>Janelas temporais (explícitas)</h2>
 * <ul>
 *   <li><b>Revisão vencida</b>: {@code proxima_revisao} anterior a hoje.</li>
 *   <li><b>Revisão próxima</b>: {@code proxima_revisao} entre hoje e hoje + 15 dias — mesma
 *       janela usada por {@code VeiculoService.calcularSituacaoManutencao}, garantindo que o
 *       badge "Atenção" e este indicador nunca divirjam.</li>
 *   <li><b>Revisão em dia</b>: sem revisão agendada ou agendada além da janela de 15 dias.
 *       Revisões futuras fora da janela não viram alerta.</li>
 *   <li><b>CNH</b>: vencida (validade anterior a hoje) ou a vencer em até 60 dias.</li>
 *   <li><b>Manutenções</b>: do primeiro dia do mês (mês atual - 5) até hoje, pela
 *       {@code data_manutencao} (data real da manutenção, não a data de inserção).</li>
 * </ul>
 *
 * <h2>Escopo por perfil</h2>
 * <ul>
 *   <li><b>ADMIN</b> e <b>OPERADOR</b>: frota completa (o OPERADOR já enxerga toda a frota na
 *       listagem {@code /veiculos}, então o dashboard não amplia exposição).</li>
 *   <li><b>MOTORISTA</b>: apenas os veículos vinculados ao próprio cadastro, apenas a própria
 *       CNH e apenas as manutenções dos seus veículos.</li>
 * </ul>
 */
@Service
public class DashboardService {

    /** Janela de antecedência para revisões "próximas" — alinhada a VeiculoService. */
    static final int JANELA_REVISAO_DIAS = 15;
    /** Quantidade de meses exibidos no gráfico de manutenções (mês atual + 5 anteriores). */
    static final int MESES_MANUTENCOES = 6;
    /** Antecedência para alertar sobre vencimento de CNH. */
    static final int JANELA_CNH_DIAS = 60;
    /** Limite de linhas da lista de próximas revisões. */
    static final int LIMITE_REVISOES = 8;

    private static final List<String> STATUS_DOMINIO =
            List.of("Disponível", "Em trânsito", "Em manutenção", "Indisponível");
    private static final Map<String, String> STATUS_COR = Map.of(
            "Disponível", "#247a58",
            "Em trânsito", "#266b98",
            "Em manutenção", "#a66b15",
            "Indisponível", "#b44040");
    private static final Map<String, String> STATUS_BADGE = Map.of(
            "Disponível", "status-success",
            "Em trânsito", "status-info",
            "Em manutenção", "status-warning",
            "Indisponível", "status-danger");
    private static final String COR_NEUTRA = "#6c7b89";
    private static final String BADGE_NEUTRO = "status-info";
    private static final String[] MESES_ABREVIADOS = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};

    private final VeiculoRepository veiculoRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final MotoristaRepository motoristaRepository;
    private final TagRepository tagRepository;
    private final UsuarioService usuarioService;

    public DashboardService(VeiculoRepository veiculoRepository,
                            ManutencaoRepository manutencaoRepository,
                            MotoristaRepository motoristaRepository,
                            TagRepository tagRepository,
                            UsuarioService usuarioService) {
        this.veiculoRepository = veiculoRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.motoristaRepository = motoristaRepository;
        this.tagRepository = tagRepository;
        this.usuarioService = usuarioService;
    }

    public DashboardDTO montar(HttpSession session) {
        Usuario logado = usuarioService.obterUsuarioLogado(session);
        return montar(logado);
    }

    /** Sobrecarga usada nos testes e por quem já tem o usuário resolvido. */
    public DashboardDTO montar(Usuario logado) {
        LocalDate hoje = LocalDate.now();
        boolean restrito = logado != null && logado.isMotorista();
        Long motoristaId = restrito ? idDoMotorista(logado) : null;

        // Perfil MOTORISTA sem cadastro de motorista vinculado: nada pode ser exibido.
        if (restrito && motoristaId == null) {
            return escopoVazio(hoje);
        }

        LocalDate limiteRevisao = hoje.plusDays(JANELA_REVISAO_DIAS);
        LocalDate limiteCnh = hoje.plusDays(JANELA_CNH_DIAS);

        Map<String, Long> contagensPorStatus = contarPorStatus(motoristaId);
        long totalVeiculos = contagensPorStatus.values().stream().mapToLong(Long::longValue).sum();

        long revisoesVencidas = contarRevisoesVencidas(motoristaId, hoje);
        long revisoesProximas = contarRevisoesProximas(motoristaId, hoje, limiteRevisao);
        long revisoesEmDia = Math.max(0, totalVeiculos - revisoesVencidas - revisoesProximas);

        List<CnhAlertaDTO> cnhsAlerta = montarAlertasDeCnh(logado, restrito, motoristaId, hoje, limiteCnh);
        long cnhsVencidas = cnhsAlerta.stream().filter(c -> c.diasRestantes() < 0).count();
        long cnhsVencendo = cnhsAlerta.size() - cnhsVencidas;

        IndicadoresDTO indicadores = new IndicadoresDTO(
                totalVeiculos,
                contagensPorStatus.getOrDefault("Disponível", 0L),
                contagensPorStatus.getOrDefault("Em trânsito", 0L),
                contagensPorStatus.getOrDefault("Em manutenção", 0L),
                contagensPorStatus.getOrDefault("Indisponível", 0L),
                revisoesVencidas,
                revisoesProximas,
                revisoesEmDia,
                cnhsVencidas,
                cnhsVencendo);

        List<DistribuicaoStatusDTO> distribuicao = montarDistribuicao(contagensPorStatus);
        List<SerieMensalDTO> serieMensal = montarSerieMensal(motoristaId, hoje);
        List<VeiculoPorTagDTO> tags = montarVeiculosPorTag(motoristaId);
        List<RevisaoVeiculoDTO> proximasRevisoes = montarProximasRevisoes(motoristaId, hoje, limiteRevisao);
        List<AlertaDTO> alertas = montarAlertas(indicadores);

        return new DashboardDTO(
                restrito ? "Sua frota (" + totalVeiculos + " veículo(s))" : "Frota completa",
                restrito,
                "vencidas ou com vencimento em até " + JANELA_REVISAO_DIAS + " dias",
                "vencidas ou com vencimento em até " + JANELA_CNH_DIAS + " dias",
                "últimos " + MESES_MANUTENCOES + " meses (" + rotuloDe(YearMonth.from(hoje).minusMonths(MESES_MANUTENCOES - 1))
                        + " a " + rotuloDe(YearMonth.from(hoje)) + ")",
                indicadores,
                distribuicao,
                serieMensal,
                serieMensal.stream().anyMatch(m -> m.total() > 0),
                tags,
                tags.stream().filter(t -> t.total() > 0).count() >= 2,
                alertas,
                proximasRevisoes,
                cnhsAlerta);
    }

    private DashboardDTO escopoVazio(LocalDate hoje) {
        List<DistribuicaoStatusDTO> distribuicao = STATUS_DOMINIO.stream()
                .map(status -> new DistribuicaoStatusDTO(status, 0L, STATUS_COR.get(status), STATUS_BADGE.get(status)))
                .toList();
        List<SerieMensalDTO> serieMensal = construirBucketsMensais(Map.of(), hoje);
        return new DashboardDTO(
                "Sem frota vinculada ao seu cadastro",
                true,
                "vencidas ou com vencimento em até " + JANELA_REVISAO_DIAS + " dias",
                "vencidas ou com vencimento em até " + JANELA_CNH_DIAS + " dias",
                "últimos " + MESES_MANUTENCOES + " meses",
                new IndicadoresDTO(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                distribuicao,
                serieMensal,
                false,
                List.of(),
                false,
                List.of(),
                List.of(),
                List.of());
    }

    // ------------------------------------------------------------------
    // Veículos por status
    // ------------------------------------------------------------------

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

    private List<DistribuicaoStatusDTO> montarDistribuicao(Map<String, Long> contagensPorStatus) {
        List<DistribuicaoStatusDTO> distribuicao = new ArrayList<>();
        for (String status : STATUS_DOMINIO) {
            distribuicao.add(new DistribuicaoStatusDTO(
                    status,
                    contagensPorStatus.getOrDefault(status, 0L),
                    STATUS_COR.getOrDefault(status, COR_NEUTRA),
                    STATUS_BADGE.getOrDefault(status, BADGE_NEUTRO)));
        }
        // Status fora do domínio conhecido não deveriam existir (constraint de negócio), mas
        // são mantidos na série para que a soma da distribuição continue igual ao total real.
        contagensPorStatus.forEach((status, total) -> {
            if (!STATUS_DOMINIO.contains(status)) {
                distribuicao.add(new DistribuicaoStatusDTO(status, total, COR_NEUTRA, BADGE_NEUTRO));
            }
        });
        return distribuicao;
    }

    // ------------------------------------------------------------------
    // Revisões
    // ------------------------------------------------------------------

    private long contarRevisoesVencidas(Long motoristaId, LocalDate hoje) {
        return motoristaId == null
                ? veiculoRepository.countByProximaRevisaoBefore(hoje)
                : veiculoRepository.countByMotoristaIdAndProximaRevisaoBefore(motoristaId, hoje);
    }

    private long contarRevisoesProximas(Long motoristaId, LocalDate hoje, LocalDate limite) {
        return motoristaId == null
                ? veiculoRepository.countByProximaRevisaoBetween(hoje, limite)
                : veiculoRepository.countByMotoristaIdAndProximaRevisaoBetween(motoristaId, hoje, limite);
    }

    private List<RevisaoVeiculoDTO> montarProximasRevisoes(Long motoristaId, LocalDate hoje, LocalDate limite) {
        List<Veiculo> veiculos = motoristaId == null
                ? veiculoRepository.findByProximaRevisaoIsNotNullOrderByProximaRevisaoAsc()
                : veiculoRepository.findByMotoristaIdAndProximaRevisaoIsNotNullOrderByProximaRevisaoAsc(motoristaId);

        return veiculos.stream()
                .filter(v -> v.getProximaRevisao() != null && !v.getProximaRevisao().isAfter(limite))
                .limit(LIMITE_REVISOES)
                .map(v -> paraRevisaoDTO(v, hoje))
                .collect(Collectors.toList());
    }

    private RevisaoVeiculoDTO paraRevisaoDTO(Veiculo veiculo, LocalDate hoje) {
        long diasRestantes = ChronoUnit.DAYS.between(hoje, veiculo.getProximaRevisao());
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
    // CNH
    // ------------------------------------------------------------------

    private List<CnhAlertaDTO> montarAlertasDeCnh(Usuario logado, boolean restrito, Long motoristaId,
                                                  LocalDate hoje, LocalDate limite) {
        List<Motorista> candidatos;
        if (restrito) {
            Motorista proprio = logado.getMotorista();
            candidatos = proprio == null ? List.of() : List.of(proprio);
        } else {
            candidatos = motoristaRepository.findByValidadeCnhLessThanEqualOrderByValidadeCnhAsc(limite);
        }

        return candidatos.stream()
                .filter(m -> m.getValidadeCnh() != null && !m.getValidadeCnh().isAfter(limite))
                .map(m -> {
                    long dias = ChronoUnit.DAYS.between(hoje, m.getValidadeCnh());
                    return new CnhAlertaDTO(m.getId(), m.getNome(), m.getNumeroCnh(), m.getValidadeCnh(),
                            dias, dias < 0 ? "Vencida" : "A vencer");
                })
                .sorted((a, b) -> Long.compare(a.diasRestantes(), b.diasRestantes()))
                .collect(Collectors.toList());
    }

    private Long idDoMotorista(Usuario logado) {
        return logado.getMotorista() != null ? logado.getMotorista().getId() : null;
    }

    // ------------------------------------------------------------------
    // Manutenções por mês
    // ------------------------------------------------------------------

    private List<SerieMensalDTO> montarSerieMensal(Long motoristaId, LocalDate hoje) {
        LocalDate inicio = YearMonth.from(hoje).minusMonths(MESES_MANUTENCOES - 1).atDay(1);
        List<ManutencaoRepository.ContagemMensal> contagens = motoristaId == null
                ? manutencaoRepository.contarPorMes(inicio, hoje)
                : manutencaoRepository.contarPorMesDoMotorista(inicio, hoje, motoristaId);

        Map<String, Long> porMes = contagens.stream()
                .collect(Collectors.toMap(ManutencaoRepository.ContagemMensal::getMes,
                        ManutencaoRepository.ContagemMensal::getTotal, Long::sum, LinkedHashMap::new));
        return construirBucketsMensais(porMes, hoje);
    }

    private List<SerieMensalDTO> construirBucketsMensais(Map<String, Long> porMes, LocalDate hoje) {
        List<SerieMensalDTO> serie = new ArrayList<>();
        for (int i = MESES_MANUTENCOES - 1; i >= 0; i--) {
            YearMonth mes = YearMonth.from(hoje).minusMonths(i);
            serie.add(new SerieMensalDTO(mes.toString(), rotuloDe(mes), porMes.getOrDefault(mes.toString(), 0L)));
        }
        return serie;
    }

    private String rotuloDe(YearMonth mes) {
        return MESES_ABREVIADOS[mes.getMonthValue() - 1] + "/"
                + String.format("%02d", Math.floorMod(mes.getYear(), 100));
    }

    // ------------------------------------------------------------------
    // Tags
    // ------------------------------------------------------------------

    private List<VeiculoPorTagDTO> montarVeiculosPorTag(Long motoristaId) {
        List<TagRepository.ContagemPorTag> contagens = motoristaId == null
                ? tagRepository.contarVeiculosPorTag()
                : tagRepository.contarVeiculosPorTagDoMotorista(motoristaId);
        return contagens.stream()
                .map(c -> new VeiculoPorTagDTO(c.getTag(), c.getTotal()))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Alertas priorizados
    // ------------------------------------------------------------------

    private List<AlertaDTO> montarAlertas(IndicadoresDTO indicadores) {
        List<AlertaDTO> alertas = new ArrayList<>();

        if (indicadores.revisoesVencidas() > 0) {
            alertas.add(new AlertaDTO("ALTA", "Revisões vencidas",
                    quantificar(indicadores.revisoesVencidas(), "veículo está com a revisão vencida",
                            "veículos estão com a revisão vencida"),
                    "/veiculos", "Ver veículos"));
        }
        if (indicadores.cnhsVencidas() > 0) {
            alertas.add(new AlertaDTO("ALTA", "CNHs vencidas",
                    quantificar(indicadores.cnhsVencidas(), "motorista está com a CNH vencida",
                            "motoristas estão com a CNH vencida"),
                    "/motoristas", "Ver motoristas"));
        }
        if (indicadores.revisoesProximas() > 0) {
            alertas.add(new AlertaDTO("MEDIA", "Revisões próximas",
                    quantificar(indicadores.revisoesProximas(), "veículo tem revisão nos próximos "
                                    + JANELA_REVISAO_DIAS + " dias",
                            "veículos têm revisão nos próximos " + JANELA_REVISAO_DIAS + " dias"),
                    "/veiculos", "Ver veículos"));
        }
        if (indicadores.cnhsVencendo() > 0) {
            alertas.add(new AlertaDTO("MEDIA", "CNHs a vencer",
                    quantificar(indicadores.cnhsVencendo(), "CNH vence nos próximos " + JANELA_CNH_DIAS + " dias",
                            "CNHs vencem nos próximos " + JANELA_CNH_DIAS + " dias"),
                    "/motoristas", "Ver motoristas"));
        }
        if (indicadores.emManutencao() > 0) {
            alertas.add(new AlertaDTO("INFO", "Veículos em manutenção",
                    quantificar(indicadores.emManutencao(), "veículo está em manutenção",
                            "veículos estão em manutenção"),
                    "/veiculos", "Ver veículos"));
        }
        return alertas;
    }

    private String quantificar(long quantidade, String singular, String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }
}
