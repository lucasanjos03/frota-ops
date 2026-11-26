const Utils = {
  formatarPlaca(valor) {
    return (valor || '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7);
  },

  formatarCpf(valor) {
    const digits = (valor || '').replace(/\D/g, '').slice(0, 11);
    if (!digits) return '';
    if (digits.length <= 3) return digits;
    if (digits.length <= 6) return `${digits.slice(0, 3)}.${digits.slice(3)}`;
    if (digits.length <= 9) return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`;
    return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
  },

  formatarCnh(valor) {
    return (valor || '').replace(/\D/g, '').slice(0, 11);
  },

  formatarTelefone(valor) {
    const digits = (valor || '').replace(/\D/g, '').slice(0, 11);
    if (!digits) return '';
    if (digits.length <= 2) return `(${digits}`;
    if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
    if (digits.length <= 10) return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
    return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
  },

  formatarApenasDigitos(valor, maxLen) {
    const digits = (valor || '').replace(/\D/g, '');
    return maxLen ? digits.slice(0, maxLen) : digits;
  },

  formatarUsername(valor) {
    return (valor || '').toLowerCase().replace(/[^a-z0-9._-]/g, '').slice(0, 100);
  }
};

const FormMasks = {
  impedirTeclasNaoDigito(e) {
    // Permite teclas de navegação, edição e atalhos (Ctrl/Cmd)
    if (
      ['Backspace', 'Tab', 'Enter', 'Delete', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'].includes(e.key) ||
      e.ctrlKey || e.metaKey || e.altKey
    ) {
      return;
    }
    // Bloqueia qualquer caractere que não seja dígito numérico (0-9)
    if (!/^\d$/.test(e.key)) {
      e.preventDefault();
    }
  },

  impedirTeclasNaoAlfanumericas(e) {
    if (
      ['Backspace', 'Tab', 'Enter', 'Delete', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'].includes(e.key) ||
      e.ctrlKey || e.metaKey || e.altKey
    ) {
      return;
    }
    if (!/^[a-zA-Z0-9]$/.test(e.key)) {
      e.preventDefault();
    }
  },

  iniciar() {
    // 1. Placa de veículo (#plate ou #placa)
    const inputsPlaca = document.querySelectorAll('#plate, #placa');
    inputsPlaca.forEach((input) => {
      input.setAttribute('maxlength', '7');
      input.setAttribute('autocapitalize', 'characters');
      input.setAttribute('spellcheck', 'false');
      input.addEventListener('keydown', this.impedirTeclasNaoAlfanumericas);
      input.addEventListener('input', (e) => {
        e.target.value = Utils.formatarPlaca(e.target.value);
      });
      if (input.value) {
        input.value = Utils.formatarPlaca(input.value);
      }
    });

    // 2. Campo de busca geral (#search ou #q)
    const inputsBusca = document.querySelectorAll('#search, #q');
    inputsBusca.forEach((input) => {
      input.setAttribute('maxlength', '100');
    });

    // 3. CPF do motorista (#cpf)
    const inputsCpf = document.querySelectorAll('#cpf');
    inputsCpf.forEach((input) => {
      input.setAttribute('maxlength', '14');
      input.setAttribute('inputmode', 'numeric');
      input.setAttribute('autocomplete', 'off');
      input.addEventListener('keydown', this.impedirTeclasNaoDigito);
      input.addEventListener('input', (e) => {
        e.target.value = Utils.formatarCpf(e.target.value);
      });
      if (input.value) {
        input.value = Utils.formatarCpf(input.value);
      }
    });

    // 4. CNH do motorista (#numeroCnh)
    const inputsCnh = document.querySelectorAll('#numeroCnh');
    inputsCnh.forEach((input) => {
      input.setAttribute('maxlength', '11');
      input.setAttribute('inputmode', 'numeric');
      input.setAttribute('autocomplete', 'off');
      input.addEventListener('keydown', this.impedirTeclasNaoDigito);
      input.addEventListener('input', (e) => {
        e.target.value = Utils.formatarCnh(e.target.value);
      });
      if (input.value) {
        input.value = Utils.formatarCnh(input.value);
      }
    });

    // 5. Telefone do motorista (#telefone)
    const inputsTelefone = document.querySelectorAll('#telefone');
    inputsTelefone.forEach((input) => {
      input.setAttribute('maxlength', '15');
      input.setAttribute('inputmode', 'numeric');
      input.setAttribute('autocomplete', 'tel');
      input.addEventListener('keydown', this.impedirTeclasNaoDigito);
      input.addEventListener('input', (e) => {
        e.target.value = Utils.formatarTelefone(e.target.value);
      });
      if (input.value) {
        input.value = Utils.formatarTelefone(input.value);
      }
    });

    // 6. Ano de fabricação (#year ou #ano)
    const inputsAno = document.querySelectorAll('#year, #ano');
    inputsAno.forEach((input) => {
      input.setAttribute('maxlength', '4');
      input.setAttribute('inputmode', 'numeric');
      input.addEventListener('keydown', this.impedirTeclasNaoDigito);
      input.addEventListener('input', (e) => {
        e.target.value = Utils.formatarApenasDigitos(e.target.value, 4);
      });
    });

    // 7. Quilometragem (#mileage ou #quilometragem)
    const inputsKm = document.querySelectorAll('#mileage, #quilometragem');
    inputsKm.forEach((input) => {
      input.setAttribute('maxlength', '7');
      input.setAttribute('inputmode', 'numeric');
      input.addEventListener('keydown', this.impedirTeclasNaoDigito);
      input.addEventListener('input', (e) => {
        e.target.value = Utils.formatarApenasDigitos(e.target.value, 7);
      });
    });

    // 8. Username em cadastros (#username fora de form de login)
    const inputsUsername = document.querySelectorAll('#username');
    inputsUsername.forEach((input) => {
      const isLogin = input.closest('form') && input.closest('form').getAttribute('action') && input.closest('form').getAttribute('action').includes('/login');
      if (!isLogin) {
        input.setAttribute('maxlength', '100');
        input.setAttribute('autocorrect', 'off');
        input.setAttribute('autocapitalize', 'none');
        input.setAttribute('spellcheck', 'false');
        input.addEventListener('input', (e) => {
          e.target.value = Utils.formatarUsername(e.target.value);
        });
      }
    });

    // 9. Bloqueio de submissão dupla em formulários (Double Submit Prevention)
    document.querySelectorAll('form').forEach((form) => {
      form.addEventListener('submit', () => {
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn && !submitBtn.disabled) {
          setTimeout(() => {
            submitBtn.disabled = true;
          }, 0);
        }
      });
    });
  }
};

document.addEventListener('DOMContentLoaded', () => {
  FormMasks.iniciar();
});

/* ---------------------------------------------------------------------------
   Gráficos (Chart.js).

   A fonte de dados é exclusivamente um objeto global preparado pelo servidor
   (`window.FLEET_DASHBOARD`) com DTOs agregados já calculados. Nenhuma
   entidade JPA trafega para o navegador e nenhum dado é recalculado aqui: o
   JavaScript apenas desenha.

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

    if (ChartKit.bibliotecaAusente([
      'chart-distribuicao', 'chart-manutencoes', 'chart-tags',
      'chart-quilometragem', 'chart-categoria', 'chart-ranking-manutencoes'
    ])) {
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
    this.quilometragem(dados, t);
    this.categorias(dados, t);
    this.ranking(dados, t);
  },

  quilometragem(dados, t) {
    const veiculos = dados.maioresQuilometragens || [];
    ChartKit.desenhar('chart-quilometragem', veiculos.length > 0, {
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
  },

  categorias(dados, t) {
    const categorias = dados.motoristasPorCategoria || [];
    const total = categorias.reduce((soma, item) => soma + Number(item.total || 0), 0);
    ChartKit.desenhar('chart-categoria', total > 0, {
      type: 'bar',
      data: {
        labels: categorias.map((item) => `Categoria ${item.categoria}`),
        datasets: [
          {
            label: 'Motoristas',
            data: categorias.map((item) => Number(item.total || 0)),
            backgroundColor: t.azulClaro,
            borderRadius: 4,
            maxBarThickness: 48
          }
        ]
      },
      options: {
        ...ChartKit.base(),
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${ctx.parsed.y} motorista(s)`
            }
          }
        },
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
  },

  ranking(dados, t) {
    const veiculos = dados.rankingManutencoes || [];
    ChartKit.desenhar('chart-ranking-manutencoes', veiculos.length > 0, {
      type: 'bar',
      data: {
        labels: veiculos.map((item) => item.placa),
        datasets: [
          {
            label: 'Registros de manutenção',
            data: veiculos.map((item) => Number(item.total || 0)),
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
              label: (ctx) => ` ${ctx.parsed.x} registro(s)`
            }
          }
        },
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

document.addEventListener('DOMContentLoaded', () => {
  DashboardCharts.iniciar();
});
