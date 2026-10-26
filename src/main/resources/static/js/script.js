const Utils = {
  formatarPlaca(valor) {
    return (valor || '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7);
  },

  // Sistema de notificação na tela (Toast)
  mostrarNotificacao(mensagem, tipo = 'sucesso') {
    let toast = document.getElementById('fleet-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'fleet-toast';
      document.body.appendChild(toast);
    }

    toast.textContent = mensagem;
    const tipoClasse = tipo === 'sucesso' ? 'fleet-toast-success' : 'fleet-toast-error';
    toast.className = `fleet-toast ${tipoClasse} show`;

    clearTimeout(toast.timeoutId);
    toast.timeoutId = setTimeout(() => {
      toast.classList.remove('show');
    }, 3200);
  }
};

document.addEventListener('DOMContentLoaded', () => {
  // Formatação automática do campo de placa (letras maiúsculas e sem caracteres especiais)
  const inputPlaca = document.getElementById('plate');
  if (inputPlaca) {
    inputPlaca.addEventListener('input', (e) => {
      e.target.value = Utils.formatarPlaca(e.target.value);
    });
  }

  // Conversão para maiúsculas na busca por placa
  const inputBusca = document.getElementById('search');
  if (inputBusca) {
    inputBusca.addEventListener('input', () => {
      inputBusca.value = inputBusca.value.toUpperCase();
    });
  }
});

/* ---------------------------------------------------------------------------
   Gráficos (Chart.js).

   A fonte de dados é exclusivamente um objeto global preparado pelo servidor
   (`window.FLEET_DASHBOARD` ou `window.FLEET_VEICULOS`) com DTOs agregados já
   calculados. Nenhuma entidade JPA trafega para o navegador e nenhum dado é
   recalculado aqui: o JavaScript apenas desenha.

   As informações também são renderizadas pelo Thymeleaf em listas de legenda,
   portanto continuam disponíveis para leitores de tela mesmo se o Chart.js
   falhar.
   --------------------------------------------------------------------------- */
const ChartKit = {
  canvas(graficoId) {
    return document.getElementById(graficoId);
  },

  estado(graficoId) {
    const canvas = this.canvas(graficoId);
    return canvas && canvas.parentElement
      ? canvas.parentElement.querySelector('.chart-state')
      : null;
  },

  definirMensagem(graficoId, mensagem) {
    const estado = this.estado(graficoId);
    if (estado) {
      estado.textContent = mensagem;
      estado.hidden = false;
    }
  },

  // Lê os tokens do design system para manter os gráficos alinhados ao CSS.
  token(nome, padrao) {
    const valor = getComputedStyle(document.documentElement).getPropertyValue(nome);
    return valor && valor.trim() ? valor.trim() : padrao;
  },

  tokens() {
    return {
      azul: this.token('--color-blue-600', '#2378a9'),
      azulClaro: this.token('--color-sky-400', '#6bb4d8'),
      grade: this.token('--border-soft', '#e9eef2'),
      rotulo: this.token('--color-ink-soft', '#364858')
    };
  },

  base() {
    const semAnimacao = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: !semAnimacao
    };
  },

  bibliotecaAusente(ids) {
    if (typeof Chart !== 'undefined') {
      return false;
    }
    ids.forEach((id) =>
      this.definirMensagem(id, 'Gráfico indisponível: biblioteca não carregada.')
    );
    return true;
  },

  desenhar(graficoId, temDados, config) {
    if (!this.canvas(graficoId)) {
      return;
    }

    if (!temDados) {
      this.definirMensagem(graficoId, 'Sem dados para exibir.');
      return;
    }

    try {
      new Chart(this.canvas(graficoId), config);
      const estado = this.estado(graficoId);
      if (estado) {
        estado.hidden = true;
      }
    } catch (erro) {
      this.definirMensagem(graficoId, 'Não foi possível renderizar o gráfico.');
    }
  }
};

const DashboardCharts = {
  iniciar() {
    const dados = window.FLEET_DASHBOARD;
    if (!dados) {
      return;
    }

    if (ChartKit.bibliotecaAusente(['chart-distribuicao', 'chart-manutencoes', 'chart-tags'])) {
      return;
    }

    const t = ChartKit.tokens();

    const status = dados.distribuicaoStatus || [];
    const totalFrota = status.reduce((soma, item) => soma + Number(item.total || 0), 0);

    ChartKit.desenhar('chart-distribuicao', totalFrota > 0, {
      type: 'doughnut',
      data: {
        labels: status.map((item) => item.status),
        datasets: [
          {
            data: status.map((item) => item.total),
            backgroundColor: status.map((item) => item.cor || t.azulClaro),
            borderColor: '#ffffff',
            borderWidth: 2
          }
        ]
      },
      options: {
        ...ChartKit.base(),
        cutout: '58%',
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${ctx.label}: ${ctx.parsed} veículo(s)`
            }
          }
        }
      }
    });

    const meses = dados.manutencoesPorMes || [];
    const totalManutencoes = meses.reduce((soma, item) => soma + Number(item.total || 0), 0);

    ChartKit.desenhar('chart-manutencoes', totalManutencoes > 0, {
      type: 'bar',
      data: {
        labels: meses.map((item) => item.rotulo),
        datasets: [
          {
            label: 'Manutenções',
            data: meses.map((item) => item.total),
            backgroundColor: t.azul,
            borderRadius: 4,
            maxBarThickness: 40
          }
        ]
      },
      options: {
        ...ChartKit.base(),
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { color: t.rotulo } },
          y: {
            beginAtZero: true,
            ticks: { precision: 0, color: t.rotulo },
            grid: { color: t.grade }
          }
        }
      }
    });

    const tags = dados.veiculosPorTag || [];
    const totalTags = tags.reduce((soma, item) => soma + Number(item.total || 0), 0);

    ChartKit.desenhar('chart-tags', totalTags > 0, {
      type: 'bar',
      data: {
        labels: tags.map((item) => item.tag),
        datasets: [
          {
            label: 'Veículos',
            data: tags.map((item) => item.total),
            backgroundColor: t.azulClaro,
            borderRadius: 4,
            maxBarThickness: 26
          }
        ]
      },
      options: {
        ...ChartKit.base(),
        indexAxis: 'y',
        plugins: { legend: { display: false } },
        scales: {
          x: {
            beginAtZero: true,
            ticks: { precision: 0, color: t.rotulo },
            grid: { color: t.grade }
          },
          y: { grid: { display: false }, ticks: { color: t.rotulo } }
        }
      }
    });
  }
};

/* Gráfico da página de veículos: ranking de quilometragem atual (hodômetro).
   Não representa consumo, desgaste ou falha mecânica — apenas o valor do
   cadastro de cada veículo. */
const VeiculoCharts = {
  iniciar() {
    const dados = window.FLEET_VEICULOS;
    if (!dados) {
      return;
    }

    const graficoId = 'chart-quilometragem';
    if (ChartKit.bibliotecaAusente([graficoId])) {
      return;
    }

    const veiculos = dados.maioresQuilometragens || [];
    const t = ChartKit.tokens();

    ChartKit.desenhar(graficoId, veiculos.length > 0, {
      type: 'bar',
      data: {
        labels: veiculos.map((item) => item.placa),
        datasets: [
          {
            label: 'Quilometragem atual',
            data: veiculos.map((item) => Number(item.quilometragem || 0)),
            backgroundColor: t.azul,
            borderRadius: 4,
            maxBarThickness: 26
          }
        ]
      },
      options: {
        ...ChartKit.base(),
        indexAxis: 'y',
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              title: (itens) => {
                const item = veiculos[itens[0].dataIndex];
                return item ? `${item.placa} · ${item.modelo}` : '';
              },
              label: (ctx) => ` ${Number(ctx.parsed.x).toLocaleString('pt-BR')} km`
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            ticks: {
              color: t.rotulo,
              callback: (valor) => Number(valor).toLocaleString('pt-BR')
            },
            grid: { color: t.grade }
          },
          y: { grid: { display: false }, ticks: { color: t.rotulo } }
        }
      }
    });
  }
};

document.addEventListener('DOMContentLoaded', () => {
  DashboardCharts.iniciar();
  VeiculoCharts.iniciar();
});
