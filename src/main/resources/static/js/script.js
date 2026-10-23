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
   Gráficos do dashboard operacional (Chart.js).

   A fonte de dados é exclusivamente `window.FLEET_DASHBOARD`, um objeto com
   DTOs agregados já calculados no servidor. Nenhuma entidade JPA trafega para
   o navegador e nenhum dado é recalculado aqui: o JavaScript apenas desenha.

   As listas de legenda (contagem por status, série mensal e contagem por tag)
   são renderizadas pelo Thymeleaf, portanto continuam disponíveis para
   leitores de tela mesmo se o Chart.js falhar.
   --------------------------------------------------------------------------- */
const DashboardCharts = {
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

  base() {
    const semAnimacao = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: !semAnimacao
    };
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
  },

  iniciar() {
    const dados = window.FLEET_DASHBOARD;
    if (!dados) {
      return;
    }

    if (typeof Chart === 'undefined') {
      ['chart-distribuicao', 'chart-manutencoes', 'chart-tags'].forEach((id) =>
        this.definirMensagem(id, 'Gráfico indisponível: biblioteca não carregada.')
      );
      return;
    }

    const azul = this.token('--color-blue-600', '#2378a9');
    const azulClaro = this.token('--color-sky-400', '#6bb4d8');
    const grade = this.token('--border-soft', '#e9eef2');
    const rotulo = this.token('--color-ink-soft', '#364858');

    const status = dados.distribuicaoStatus || [];
    const totalFrota = status.reduce((soma, item) => soma + Number(item.total || 0), 0);

    this.desenhar('chart-distribuicao', totalFrota > 0, {
      type: 'doughnut',
      data: {
        labels: status.map((item) => item.status),
        datasets: [
          {
            data: status.map((item) => item.total),
            backgroundColor: status.map((item) => item.cor || azulClaro),
            borderColor: '#ffffff',
            borderWidth: 2
          }
        ]
      },
      options: {
        ...this.base(),
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

    this.desenhar('chart-manutencoes', totalManutencoes > 0, {
      type: 'bar',
      data: {
        labels: meses.map((item) => item.rotulo),
        datasets: [
          {
            label: 'Manutenções',
            data: meses.map((item) => item.total),
            backgroundColor: azul,
            borderRadius: 4,
            maxBarThickness: 40
          }
        ]
      },
      options: {
        ...this.base(),
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { color: rotulo } },
          y: {
            beginAtZero: true,
            ticks: { precision: 0, color: rotulo },
            grid: { color: grade }
          }
        }
      }
    });

    const tags = dados.veiculosPorTag || [];
    const totalTags = tags.reduce((soma, item) => soma + Number(item.total || 0), 0);

    this.desenhar('chart-tags', totalTags > 0, {
      type: 'bar',
      data: {
        labels: tags.map((item) => item.tag),
        datasets: [
          {
            label: 'Veículos',
            data: tags.map((item) => item.total),
            backgroundColor: azulClaro,
            borderRadius: 4,
            maxBarThickness: 26
          }
        ]
      },
      options: {
        ...this.base(),
        indexAxis: 'y',
        plugins: { legend: { display: false } },
        scales: {
          x: {
            beginAtZero: true,
            ticks: { precision: 0, color: rotulo },
            grid: { color: grade }
          },
          y: { grid: { display: false }, ticks: { color: rotulo } }
        }
      }
    });
  }
};

document.addEventListener('DOMContentLoaded', () => {
  DashboardCharts.iniciar();
});
