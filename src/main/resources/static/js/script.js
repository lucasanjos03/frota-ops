/**
 * FleetOps - Gestão de Frota e Operações
 * Script inicial Vanilla JS (ES6+)
 * 
 * Estrutura preparada para expansão:
 * 1. Mock Database (localStorage) -> Facilita migração futura para Spring Boot REST API
 * 2. Formatação e Validação (Placas, KM, Datas)
 * 3. Gerenciamento de Telas (Dashboard, Listagem, Cadastro, Edição, Detalhes)
 * 4. Sistema de Notificações (Toast)
 */

// ==========================================================================
// 1. CAMADA DE DADOS (MOCK / LOCALSTORAGE)
// ==========================================================================
// DICA PARA O FUTURO COM SPRING BOOT:
// Quando você criar seu backend em Java (Spring Boot), este objeto 'VehicleService'
// fará chamadas com `fetch('/api/veiculos')` para os endpoints do seu @RestController!

const STORAGE_KEY = 'fleetops_veiculos';

// Dados iniciais para quando o sistema for aberto pela primeira vez
const VEICULOS_INICIAIS = [
  {
    id: 1,
    placa: 'ABC1D23',
    modelo: 'Ford Transit 350',
    ano: 2023,
    quilometragem: 42180,
    status: 'Disponível',
    ultimaRevisao: '2026-06-18',
    proximaRevisao: '2026-09-18',
    observacoes: 'Veículo utilizado em rotas urbanas e regionais. Pneus revisados na última manutenção preventiva.',
    historico: [
      { data: '18/06/2026', km: '36.200 km', servico: 'Revisão preventiva', responsavel: 'Oficina Central', obs: 'Troca de óleo e filtros' },
      { data: '12/03/2026', km: '28.700 km', servico: 'Inspeção de freios', responsavel: 'Equipe interna', obs: 'Sem irregularidades' }
    ]
  },
  {
    id: 2,
    placa: 'DEF4G56',
    modelo: 'Mercedes-Benz Sprinter',
    ano: 2022,
    quilometragem: 67420,
    status: 'Em trânsito',
    ultimaRevisao: '2026-05-10',
    proximaRevisao: '2026-09-04',
    observacoes: 'Veículo dedicado para entregas expressas interestaduais.',
    historico: [
      { data: '10/05/2026', km: '60.100 km', servico: 'Troca de pastilhas', responsavel: 'Mecânica Rápida', obs: 'Substituição completa do kit dianteiro' }
    ]
  },
  {
    id: 3,
    placa: 'GHI7J89',
    modelo: 'Volkswagen Delivery 9.170',
    ano: 2024,
    quilometragem: 18500,
    status: 'Disponível',
    ultimaRevisao: '2026-07-02',
    proximaRevisao: '2026-10-15',
    observacoes: 'Caminhão leve para distribuição metropolitana de carga seca.',
    historico: []
  },
  {
    id: 4,
    placa: 'JKL0M12',
    modelo: 'Iveco Daily 35S14',
    ano: 2021,
    quilometragem: 89300,
    status: 'Em manutenção',
    ultimaRevisao: '2026-04-12',
    proximaRevisao: '2026-08-30',
    observacoes: 'Em manutenção no motor de partida e troca da correia dentada.',
    historico: []
  }
];

const VehicleService = {
  listarTodos() {
    const dados = localStorage.getItem(STORAGE_KEY);
    if (!dados) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(VEICULOS_INICIAIS));
      return VEICULOS_INICIAIS;
    }
    try {
      return JSON.parse(dados);
    } catch (e) {
      console.error('Erro ao ler dados do localStorage:', e);
      return VEICULOS_INICIAIS;
    }
  },

  buscarPorId(id) {
    const lista = this.listarTodos();
    return lista.find(v => String(v.id) === String(id));
  },

  salvar(veiculo) {
    const lista = this.listarTodos();
    const novoId = lista.length > 0 ? Math.max(...lista.map(v => Number(v.id) || 0)) + 1 : 1;
    veiculo.id = novoId;
    veiculo.historico = veiculo.historico || [];
    lista.push(veiculo);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(lista));
    return veiculo;
  },

  atualizar(id, dadosAtualizados) {
    const lista = this.listarTodos();
    const index = lista.findIndex(v => String(v.id) === String(id));
    if (index !== -1) {
      lista[index] = { ...lista[index], ...dadosAtualizados, id: Number(id) };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(lista));
      return lista[index];
    }
    return null;
  },

  excluir(id) {
    let lista = this.listarTodos();
    lista = lista.filter(v => String(v.id) !== String(id));
    localStorage.setItem(STORAGE_KEY, JSON.stringify(lista));
  }
};

// ==========================================================================
// 2. UTILITÁRIOS E FORMATAÇÃO
// ==========================================================================
const Utils = {
  // Converte texto para maiúsculo e padroniza formato de placa
  formatarPlaca(valor) {
    return valor.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7);
  },

  formatarKm(km) {
    if (km === null || km === undefined || km === '') return '-';
    return Number(km).toLocaleString('pt-BR') + ' km';
  },

  formatarData(dataIso) {
    if (!dataIso) return '-';
    const partes = dataIso.split('-');
    if (partes.length === 3) {
      return `${partes[2]}/${partes[1]}/${partes[0]}`;
    }
    return dataIso;
  },

  obterClasseBadge(status) {
    switch ((status || '').toLowerCase()) {
      case 'disponível':
      case 'disponivel':
        return 'status-success';
      case 'em trânsito':
      case 'em transito':
        return 'status-info';
      case 'em manutenção':
      case 'em manutencao':
        return 'status-warning';
      default:
        return 'status-danger';
    }
  },

  obterParametroUrl(nome) {
    const params = new URLSearchParams(window.location.search);
    return params.get(nome);
  },

  obterIdDaUrl() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('id')) return params.get('id');
    const match = window.location.pathname.match(/\/veiculos\/(\d+)/);
    return match ? match[1] : '1';
  },

  // Sistema simples de notificação na tela (Toast)
  mostrarNotificacao(mensagem, tipo = 'sucesso') {
    let toast = document.getElementById('fleet-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'fleet-toast';
      toast.style.position = 'fixed';
      toast.style.bottom = '24px';
      toast.style.right = '24px';
      toast.style.padding = '14px 20px';
      toast.style.borderRadius = '8px';
      toast.style.color = '#fff';
      toast.style.fontWeight = 'bold';
      toast.style.fontSize = '13px';
      toast.style.boxShadow = '0 4px 14px rgba(0,0,0,0.15)';
      toast.style.zIndex = '9999';
      toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
      document.body.appendChild(toast);
    }

    toast.style.backgroundColor = tipo === 'sucesso' ? '#247a58' : '#b44040';
    toast.textContent = mensagem;
    toast.style.opacity = '1';
    toast.style.transform = 'translateY(0)';

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
    }, 3200);
  }
};

// ==========================================================================
// 3. CONTROLADORES ESPECÍFICOS DE CADA TELA
// ==========================================================================

// --- TELA: Dashboard (index.html) ---
function initDashboard() {
  const formFiltros = document.querySelector('.filters');
  const tbody = document.querySelector('.data-table tbody');
  // Se existir .filters, estamos na tela de veículos, não no dashboard
  if (formFiltros || !tbody) return;

  const veiculos = VehicleService.listarTodos();
  
  // Se houver veículos, podemos re-renderizar a tabela de alertas de manutenção
  if (veiculos.length > 0) {
    tbody.innerHTML = '';
    
    // Mostra os veículos cuja próxima revisão está preenchida
    const veiculosComRevisao = veiculos.filter(v => v.proximaRevisao);
    const listaExibicao = veiculosComRevisao.length > 0 ? veiculosComRevisao : veiculos.slice(0, 3);

    listaExibicao.forEach(v => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong class="plate">${v.placa}</strong></td>
        <td>${v.modelo}</td>
        <td>${Utils.formatarData(v.proximaRevisao)}</td>
        <td><span class="status-badge ${Utils.obterClasseBadge(v.status)}">${v.status}</span></td>
        <td class="cell-actions">
          <div class="table-actions">
            <a href="/veiculos/${v.id}">Detalhes</a>
            <a href="/veiculos/${v.id}/editar">Editar</a>
            <button type="button" class="btn-delete" data-id="${v.id}">Excluir</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });

    // Eventos do botão de exclusão
    tbody.querySelectorAll('.btn-delete').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.target.getAttribute('data-id');
        if (confirm('Tem certeza que deseja remover este veículo da frota?')) {
          VehicleService.excluir(id);
          Utils.mostrarNotificacao('Veículo excluído com sucesso!', 'sucesso');
          initDashboard();
        }
      });
    });
  }
}

// --- TELA: Listagem de Veículos (veiculos.html) ---
function initListaVeiculos() {
  const formFiltros = document.querySelector('.filters');
  const tbody = document.querySelector('.data-table tbody');
  const inputBusca = document.getElementById('search');
  const selectStatus = document.getElementById('status');
  const linkLimpar = document.querySelector('.link-button');
  const subtitle = document.querySelector('.page-subtitle');

  // Apenas executa se estiver na tela de listagem de veículos (que possui o formulário .filters)
  if (!formFiltros || !tbody) return;

  function renderizarTabela(lista) {
    tbody.innerHTML = '';

    if (subtitle) {
      subtitle.textContent = `${lista.length} veículo(s) encontrado(s) na operação.`;
    }

    if (lista.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align:center; padding: 32px; color: #6c7b89;">
            Nenhum veículo encontrado com os filtros atuais.
          </td>
        </tr>
      `;
      return;
    }

    lista.forEach(v => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong class="plate">${v.placa}</strong></td>
        <td>${v.modelo}</td>
        <td>${v.ano}</td>
        <td>${Utils.formatarKm(v.quilometragem)}</td>
        <td><span class="status-badge ${Utils.obterClasseBadge(v.status)}">${v.status}</span></td>
        <td class="cell-actions">
          <div class="table-actions">
            <a href="/veiculos/${v.id}">Detalhes</a>
            <a href="/veiculos/${v.id}/editar">Editar</a>
            <button type="button" class="btn-delete" data-id="${v.id}">Excluir</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });

    // Eventos do botão de exclusão
    tbody.querySelectorAll('.btn-delete').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.target.getAttribute('data-id');
        if (confirm('Tem certeza que deseja remover este veículo da frota?')) {
          VehicleService.excluir(id);
          Utils.mostrarNotificacao('Veículo excluído com sucesso!', 'sucesso');
          aplicarFiltros();
        }
      });
    });
  }

  function aplicarFiltros() {
    const termo = (inputBusca ? inputBusca.value : '').toLowerCase().trim();
    const statusFiltro = (selectStatus ? selectStatus.value : '').trim();

    let veiculos = VehicleService.listarTodos();

    if (termo) {
      veiculos = veiculos.filter(v => 
        (v.placa && v.placa.toLowerCase().includes(termo)) ||
        (v.modelo && v.modelo.toLowerCase().includes(termo))
      );
    }

    if (statusFiltro && statusFiltro !== 'Todos os status') {
      veiculos = veiculos.filter(v => v.status === statusFiltro);
    }

    renderizarTabela(veiculos);
  }

  // Filtragem dinâmica enquanto digita ou muda o select
  if (inputBusca) {
    inputBusca.addEventListener('input', () => {
      inputBusca.value = inputBusca.value.toUpperCase();
      aplicarFiltros();
    });
  }

  if (selectStatus) {
    selectStatus.addEventListener('change', aplicarFiltros);
  }

  if (formFiltros) {
    formFiltros.addEventListener('submit', (e) => {
      aplicarFiltros();
    });
  }

  if (linkLimpar) {
    linkLimpar.addEventListener('click', (e) => {
      if (inputBusca) inputBusca.value = '';
      if (selectStatus) selectStatus.value = '';
      aplicarFiltros();
    });
  }

  // Carga inicial
  aplicarFiltros();
}

// --- TELA: Novo Veículo (veiculo-novo.html) ---
function initNovoVeiculo() {
  const form = document.querySelector('.form-card');
  const inputPlaca = document.getElementById('plate');

  if (!form || (!window.location.pathname.includes('/veiculos/novo') && !window.location.pathname.includes('veiculo-novo'))) return;

  // Máscara e maiúsculo automático no campo de placa
  if (inputPlaca) {
    inputPlaca.addEventListener('input', (e) => {
      e.target.value = Utils.formatarPlaca(e.target.value);
    });
  }

  form.addEventListener('submit', (e) => {
    const formData = new FormData(form);
    const novoVeiculo = {
      placa: formData.get('placa')?.toString().trim().toUpperCase(),
      modelo: formData.get('modelo')?.toString().trim(),
      ano: parseInt(formData.get('ano')?.toString() || '0', 10),
      quilometragem: parseInt(formData.get('quilometragem')?.toString() || '0', 10),
      status: formData.get('status')?.toString(),
      ultimaRevisao: formData.get('ultimaRevisao')?.toString() || null,
      proximaRevisao: formData.get('proximaRevisao')?.toString() || null,
      observacoes: formData.get('observacoes')?.toString().trim() || ''
    };

    if (!novoVeiculo.placa || !novoVeiculo.modelo || !novoVeiculo.ano) {
      e.preventDefault();
      Utils.mostrarNotificacao('Por favor, preencha todos os campos obrigatórios (*).', 'erro');
      return;
    }

    VehicleService.salvar(novoVeiculo);
    Utils.mostrarNotificacao('Veículo cadastrado com sucesso!', 'sucesso');
  });
}

// --- TELA: Editar Veículo (veiculo-editar.html) ---
function initEditarVeiculo() {
  const form = document.querySelector('.form-card');
  const inputPlaca = document.getElementById('plate');

  if (!form || (!window.location.pathname.includes('/editar') && !window.location.pathname.includes('veiculo-editar'))) return;

  const id = Utils.obterIdDaUrl();
  const veiculo = VehicleService.buscarPorId(id);

  if (veiculo) {
    // Preenche o formulário com os dados atuais
    const inputModelo = document.getElementById('model');
    const inputAno = document.getElementById('year');
    const inputKm = document.getElementById('mileage');
    const selectStatus = document.getElementById('status');
    const inputUltimaRevisao = document.getElementById('last-review');
    const inputProximaRevisao = document.getElementById('next-review');
    const inputNotas = document.getElementById('notes');

    if (inputPlaca) inputPlaca.value = veiculo.placa || '';
    if (inputModelo) inputModelo.value = veiculo.modelo || '';
    if (inputAno) inputAno.value = veiculo.ano || '';
    if (inputKm) inputKm.value = veiculo.quilometragem || '';
    if (selectStatus) selectStatus.value = veiculo.status || 'Disponível';
    if (inputUltimaRevisao) inputUltimaRevisao.value = veiculo.ultimaRevisao || '';
    if (inputProximaRevisao) inputProximaRevisao.value = veiculo.proximaRevisao || '';
    if (inputNotas) inputNotas.value = veiculo.observacoes || '';

    // Ajusta o link de "Cancelar" para voltar aos detalhes do mesmo veículo
    const btnCancelar = form.querySelector('a.button-secondary');
    if (btnCancelar) {
      btnCancelar.href = `/veiculos/${veiculo.id}`;
    }
  }

  if (inputPlaca) {
    inputPlaca.addEventListener('input', (e) => {
      e.target.value = Utils.formatarPlaca(e.target.value);
    });
  }

  form.addEventListener('submit', (e) => {
    const formData = new FormData(form);
    const dadosAtualizados = {
      placa: formData.get('placa')?.toString().trim().toUpperCase(),
      modelo: formData.get('modelo')?.toString().trim(),
      ano: parseInt(formData.get('ano')?.toString() || '0', 10),
      quilometragem: parseInt(formData.get('quilometragem')?.toString() || '0', 10),
      status: formData.get('status')?.toString(),
      ultimaRevisao: formData.get('ultimaRevisao')?.toString() || null,
      proximaRevisao: formData.get('proximaRevisao')?.toString() || null,
      observacoes: formData.get('observacoes')?.toString().trim() || ''
    };

    if (!dadosAtualizados.placa || !dadosAtualizados.modelo || !dadosAtualizados.ano) {
      e.preventDefault();
      Utils.mostrarNotificacao('Por favor, preencha todos os campos obrigatórios (*).', 'erro');
      return;
    }

    VehicleService.atualizar(id, dadosAtualizados);
    Utils.mostrarNotificacao('Alterações salvas com sucesso!', 'sucesso');
  });
}

// --- TELA: Detalhes do Veículo (veiculo-detalhes.html) ---
function initDetalhesVeiculo() {
  const detailGrid = document.querySelector('.detail-grid');
  if (!detailGrid || window.location.pathname.includes('/editar') || (!window.location.pathname.match(/\/veiculos\/\d+/) && !window.location.pathname.includes('veiculo-detalhes'))) return;

  const id = Utils.obterIdDaUrl();
  const veiculo = VehicleService.buscarPorId(id);

  if (!veiculo) return;

  // Atualiza o botão de editar para apontar para o ID correto
  const btnEditar = document.querySelector('.page-header a.button-primary');
  if (btnEditar) {
    btnEditar.href = `/veiculos/${veiculo.id}/editar`;
  }

  // Preenche a grade de detalhes
  detailGrid.innerHTML = `
    <div class="detail-item"><dt>Placa</dt><dd class="plate">${veiculo.placa}</dd></div>
    <div class="detail-item"><dt>Modelo</dt><dd>${veiculo.modelo}</dd></div>
    <div class="detail-item"><dt>Ano</dt><dd>${veiculo.ano}</dd></div>
    <div class="detail-item"><dt>Quilometragem</dt><dd>${Utils.formatarKm(veiculo.quilometragem)}</dd></div>
    <div class="detail-item"><dt>Status</dt><dd><span class="status-badge ${Utils.obterClasseBadge(veiculo.status)}">${veiculo.status}</span></dd></div>
    <div class="detail-item"><dt>Situação da manutenção</dt><dd>${veiculo.proximaRevisao ? 'Agendada' : 'Em dia'}</dd></div>
    <div class="detail-item"><dt>Última revisão</dt><dd>${Utils.formatarData(veiculo.ultimaRevisao)}</dd></div>
    <div class="detail-item"><dt>Próxima revisão</dt><dd>${Utils.formatarData(veiculo.proximaRevisao)}</dd></div>
    <div class="detail-item"><dt>Código ID</dt><dd>#${veiculo.id}</dd></div>
  `;

  // Preenche observações
  const secoes = document.querySelectorAll('.content-section');
  if (secoes[1] && veiculo.observacoes) {
    const divConteudo = secoes[1].querySelector('div[style*="padding:24px"]');
    if (divConteudo) divConteudo.textContent = veiculo.observacoes;
  }

  // Preenche histórico de manutenção se houver tabela
  if (secoes[2] && veiculo.historico) {
    const tbodyHistorico = secoes[2].querySelector('.data-table tbody');
    if (tbodyHistorico) {
      if (veiculo.historico.length === 0) {
        tbodyHistorico.innerHTML = `
          <tr>
            <td colspan="5" style="text-align:center; padding: 24px; color: #6c7b89;">
              Nenhum histórico de manutenção registrado para este veículo.
            </td>
          </tr>
        `;
      } else {
        tbodyHistorico.innerHTML = '';
        veiculo.historico.forEach(item => {
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td>${item.data}</td>
            <td>${item.km}</td>
            <td>${item.servico}</td>
            <td>${item.responsavel}</td>
            <td>${item.obs}</td>
          `;
          tbodyHistorico.appendChild(tr);
        });
      }
    }
  }
}

// --- TELA: Login (login.html) ---
function initLogin() {
  const formLogin = document.querySelector('.login-card form');
  if (!formLogin) return;

  formLogin.addEventListener('submit', (e) => {
    // Por enquanto no front-end simples:
    // Evita 404 estático e simula redirecionamento para o dashboard

    const user = document.getElementById('username')?.value;
    const pass = document.getElementById('password')?.value;

    if (!user || !pass) {
      e.preventDefault();
      Utils.mostrarNotificacao('Informe usuário e senha.', 'erro');
      return;
    }

    Utils.mostrarNotificacao('Acesso autorizado! Redirecionando...', 'sucesso');
    setTimeout(() => {
      window.location.href = '/';
    }, 1000);
  });
}

// ==========================================================================
// 4. INICIALIZAÇÃO GERAL DO APLICATIVO
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
  // Inicializa o serviço de dados
  VehicleService.listarTodos();

  // Executa os controladores conforme a página carregada
  initDashboard();
  initListaVeiculos();
  initNovoVeiculo();
  initEditarVeiculo();
  initDetalhesVeiculo();
  initLogin();
});
