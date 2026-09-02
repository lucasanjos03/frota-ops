/**
 * FleetOps - Sistema de Gestão de Frota
 * Módulo de Persistência Local (localStorage) e Máscaras de Dados (Vanilla JS)
 */

const CHAVE_STORAGE = 'fleetops_veiculos';

// ---------------------------------------------------------------------------
// 1. Banco de Dados Local (localStorage)
// ---------------------------------------------------------------------------

/**
 * Retorna todos os veículos salvos no localStorage.
 * Caso seja o primeiro acesso, inicializa com dados padrão de exemplo.
 */
function obterVeiculos() {
  const dados = localStorage.getItem(CHAVE_STORAGE);
  if (!dados) {
    const dadosIniciais = [
      {
        id: '1',
        placa: 'ABC-1234',
        modelo: 'Volvo FH',
        ano: '2022',
        km: 145000,
        status: 'manutencao',
        motorista: 'Carlos Eduardo',
        ultimaRevisao: '12/10/2025',
        manutencoes: [
          {
            id: 'm1',
            data: '12/10/2025',
            tipo: 'Preventiva',
            descricao: 'Troca de óleo e filtros do motor.',
            custo: 1200
          },
          {
            id: 'm2',
            data: '02/05/2025',
            tipo: 'Corretiva',
            descricao: 'Substituição das pastilhas de freio traseiras.',
            custo: 850
          }
        ]
      },
      {
        id: '2',
        placa: 'XYZ-9876',
        modelo: 'Mercedes-Benz Actros',
        ano: '2021',
        km: 110000,
        status: 'disponivel',
        motorista: 'Ana Souza',
        ultimaRevisao: '05/01/2026',
        manutencoes: [
          {
            id: 'm3',
            data: '05/01/2026',
            tipo: 'Preventiva',
            descricao: 'Revisão periódica de 100k km.',
            custo: 950
          }
        ]
      },
      {
        id: '3',
        placa: 'KLT-4411',
        modelo: 'Scania R450',
        ano: '2023',
        km: 82000,
        status: 'transito',
        motorista: 'Roberto Lima',
        ultimaRevisao: '15/08/2025',
        manutencoes: []
      },
      {
        id: '4',
        placa: 'BRA-2E19',
        modelo: 'Volkswagen Meteor',
        ano: '2024',
        km: 35000,
        status: 'disponivel',
        motorista: 'Mariana Santos',
        ultimaRevisao: '20/02/2026',
        manutencoes: []
      }
    ];
    salvarVeiculos(dadosIniciais);
    return dadosIniciais;
  }

  try {
    return JSON.parse(dados);
  } catch (e) {
    console.error('Erro ao ler dados do localStorage:', e);
    return [];
  }
}

/**
 * Salva o array de veículos no localStorage.
 */
function salvarVeiculos(veiculos) {
  localStorage.setItem(CHAVE_STORAGE, JSON.stringify(veiculos));
}

/**
 * Busca um veículo pelo seu ID ou Placa.
 */
function obterVeiculoPorId(id) {
  const veiculos = obterVeiculos();
  return veiculos.find(v => String(v.id) === String(id) || v.placa === id);
}

/**
 * Remove um veículo pelo seu ID.
 */
function excluirVeiculo(id) {
  const veiculos = obterVeiculos();
  const novaLista = veiculos.filter(v => String(v.id) !== String(id));
  salvarVeiculos(novaLista);
}

// ---------------------------------------------------------------------------
// 2. Máscaras e Formatadores de Dados
// ---------------------------------------------------------------------------

/**
 * Máscara para Placa Veicular (Padrão Antigo ABC-1234 e Mercosul ABC-1D23)
 * Força letras maiúsculas e insere hífen após os 3 primeiros caracteres.
 */
function aplicarMascaraPlaca(input) {
  if (!input) return;
  input.addEventListener('input', (e) => {
    let valor = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
    if (valor.length > 7) {
      valor = valor.substring(0, 7);
    }
    if (valor.length > 3) {
      valor = valor.substring(0, 3) + '-' + valor.substring(3);
    }
    e.target.value = valor;
  });
}

/**
 * Máscara para Quilometragem (formatação com separadores de milhar)
 */
function aplicarMascaraKm(input) {
  if (!input) return;
  input.addEventListener('input', (e) => {
    let valor = e.target.value.replace(/\D/g, '');
    if (!valor) {
      e.target.value = '';
      return;
    }
    e.target.value = Number(valor).toLocaleString('pt-BR');
  });
}

/**
 * Máscara para Ano de Fabricação (limita a 4 dígitos numéricos)
 */
function aplicarMascaraAno(input) {
  if (!input) return;
  input.addEventListener('input', (e) => {
    let valor = e.target.value.replace(/\D/g, '');
    if (valor.length > 4) {
      valor = valor.substring(0, 4);
    }
    e.target.value = valor;
  });
}

/**
 * Máscara para Moeda Brasileira (R$)
 * Converte digitação contínua em formato monetário (ex: 1200 -> R$ 12,00)
 */
function aplicarMascaraMoeda(input) {
  if (!input) return;
  input.addEventListener('input', (e) => {
    let valor = e.target.value.replace(/\D/g, '');
    if (!valor) {
      e.target.value = '';
      return;
    }
    const numero = Number(valor) / 100;
    e.target.value = numero.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  });
}

/**
 * Extrai o valor numérico float de uma string mascarada em moeda (R$).
 */
function converterMoedaParaNumero(textoMoeda) {
  if (!textoMoeda) return 0;
  const limpo = textoMoeda.replace(/\D/g, '');
  return Number(limpo) / 100;
}

/**
 * Converte número para formato de moeda brasileira formatado.
 */
function formatarMoeda(valor) {
  return Number(valor || 0).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  });
}

/**
 * Retorna o rótulo humanizado do status do veículo.
 */
function formatarStatus(status) {
  const map = {
    disponivel: 'Disponível',
    transito: 'Em Trânsito',
    manutencao: 'Em Manutenção'
  };
  return map[status] || status;
}

/**
 * Lê parâmetros da URL (ex: pagina.html?id=123)
 */
function obterParametroUrl(parametro) {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get(parametro);
}

// ---------------------------------------------------------------------------
// 3. Inicialização e Controle das Telas
// ---------------------------------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {

  // --- TELA: Dashboard (index.html) ---
  const elTotalVeiculos = document.querySelector('#total-veiculos');
  if (elTotalVeiculos) {
    const veiculos = obterVeiculos();

    const total = veiculos.length;
    const transito = veiculos.filter(v => v.status === 'transito').length;
    const manutencao = veiculos.filter(v => v.status === 'manutencao').length;
    const disponiveis = veiculos.filter(v => v.status === 'disponivel').length;

    elTotalVeiculos.textContent = total;
    document.querySelector('#total-transito').textContent = transito;
    document.querySelector('#total-manutencao').textContent = manutencao;
    document.querySelector('#total-disponiveis').textContent = disponiveis;

    // Renderiza alertas de manutenção no Dashboard
    const tbodyAlertas = document.querySelector('#tabela-alertas-corpo');
    if (tbodyAlertas) {
      tbodyAlertas.innerHTML = '';
      const veiculosAlerta = veiculos.filter(v => v.status === 'manutencao' || (v.manutencoes && v.manutencoes.length > 0));

      if (veiculosAlerta.length === 0) {
        tbodyAlertas.innerHTML = `
          <tr>
            <td colspan="5" class="tabela-vazia">Nenhum alerta de manutenção pendente no momento.</td>
          </tr>
        `;
      } else {
        veiculosAlerta.forEach(v => {
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td><strong>${v.placa}</strong></td>
            <td>${v.modelo}</td>
            <td>${v.ultimaRevisao || 'Não informada'}</td>
            <td><span class="badge badge-${v.status}">${formatarStatus(v.status)}</span></td>
            <td><a href="detalhes.html?id=${v.id}">Ver Detalhes</a></td>
          `;
          tbodyAlertas.appendChild(tr);
        });
      }
    }
  }

  // --- TELA: Cadastro de Veículo (cadastro.html) ---
  const formCadastro = document.querySelector('#form-cadastro-veiculo');
  if (formCadastro) {
    const inputPlaca = document.querySelector('#placa');
    const inputAno = document.querySelector('#ano');
    const inputKm = document.querySelector('#km');
    const inputMotorista = document.querySelector('#motorista');
    const msgSucesso = document.querySelector('#mensagem-sucesso');

    // Aplicação das máscaras
    aplicarMascaraPlaca(inputPlaca);
    aplicarMascaraAno(inputAno);
    aplicarMascaraKm(inputKm);

    // Remove erros visuais conforme o usuário digita
    formCadastro.querySelectorAll('input, select').forEach(campo => {
      campo.addEventListener('input', () => {
        campo.classList.remove('campo-erro');
        const msgAnterior = campo.parentElement.querySelector('.mensagem-erro');
        if (msgAnterior) msgAnterior.remove();
      });
    });

    formCadastro.addEventListener('submit', (event) => {
      event.preventDefault();
      let valido = true;
      const camposObrigatorios = formCadastro.querySelectorAll('[required]');

      // Limpa erros anteriores
      formCadastro.querySelectorAll('.mensagem-erro').forEach(m => m.remove());
      formCadastro.querySelectorAll('.campo-erro').forEach(c => c.classList.remove('campo-erro'));

      camposObrigatorios.forEach(campo => {
        if (!campo.value.trim()) {
          valido = false;
          campo.classList.add('campo-erro');
          const msg = document.createElement('span');
          msg.className = 'mensagem-erro';
          msg.textContent = 'Este campo é obrigatório.';
          campo.parentElement.appendChild(msg);
        }
      });

      // Validação da Placa (formato mínimo de 7 caracteres com hífen: ABC-1234 ou ABC-1D23)
      if (inputPlaca.value.trim() && inputPlaca.value.trim().length < 8) {
        valido = false;
        inputPlaca.classList.add('campo-erro');
        const msg = document.createElement('span');
        msg.className = 'mensagem-erro';
        msg.textContent = 'Informe uma placa válida no formato ABC-1234 ou ABC-1D23.';
        inputPlaca.parentElement.appendChild(msg);
      }

      // Validação de ano coerente
      const anoNum = Number(inputAno.value);
      const anoAtual = new Date().getFullYear();
      if (inputAno.value.trim() && (anoNum < 1950 || anoNum > anoAtual + 1)) {
        valido = false;
        inputAno.classList.add('campo-erro');
        const msg = document.createElement('span');
        msg.className = 'mensagem-erro';
        msg.textContent = `Ano inválido (deve ser entre 1950 e ${anoAtual + 1}).`;
        inputAno.parentElement.appendChild(msg);
      }

      // Verifica se a placa já está cadastrada no sistema
      const veiculos = obterVeiculos();
      const placaExistente = veiculos.find(v => v.placa === inputPlaca.value.trim());
      if (placaExistente) {
        valido = false;
        inputPlaca.classList.add('campo-erro');
        const msg = document.createElement('span');
        msg.className = 'mensagem-erro';
        msg.textContent = 'Já existe um veículo cadastrado com esta placa.';
        inputPlaca.parentElement.appendChild(msg);
      }

      if (!valido) return;

      // Cria o novo veículo e salva no localStorage
      const novoVeiculo = {
        id: Date.now().toString(),
        placa: inputPlaca.value.trim(),
        modelo: document.querySelector('#modelo').value.trim(),
        ano: inputAno.value.trim(),
        km: Number(inputKm.value.replace(/\D/g, '')),
        motorista: inputMotorista ? inputMotorista.value.trim() || 'Não atribuído' : 'Não atribuído',
        status: document.querySelector('#status').value,
        ultimaRevisao: new Date().toLocaleDateString('pt-BR'),
        manutencoes: []
      };

      veiculos.push(novoVeiculo);
      salvarVeiculos(veiculos);

      // Feedback visual
      if (msgSucesso) {
        msgSucesso.textContent = `Veículo ${novoVeiculo.placa} cadastrado com sucesso! Redirecionando...`;
        msgSucesso.style.display = 'block';
      }
      formCadastro.reset();

      setTimeout(() => {
        window.location.href = 'listagem.html';
      }, 1200);
    });
  }

  // --- TELA: Listagem de Veículos (listagem.html) ---
  const tbodyListagem = document.querySelector('#tabela-veiculos-corpo');
  if (tbodyListagem) {
    function renderizarTabela() {
      const veiculos = obterVeiculos();
      tbodyListagem.innerHTML = '';

      if (veiculos.length === 0) {
        tbodyListagem.innerHTML = `
          <tr>
            <td colspan="6" class="tabela-vazia">
              Nenhum veículo cadastrado na frota. <a href="cadastro.html">Clique aqui para cadastrar um novo veículo</a>.
            </td>
          </tr>
        `;
        return;
      }

      veiculos.forEach(v => {
        const tr = document.createElement('tr');
        const kmFormatado = Number(v.km || 0).toLocaleString('pt-BR') + ' km';

        tr.innerHTML = `
          <td><strong>${v.placa}</strong></td>
          <td>${v.modelo}</td>
          <td>${v.ano}</td>
          <td>${kmFormatado}</td>
          <td><span class="badge badge-${v.status}">${formatarStatus(v.status)}</span></td>
          <td>
            <a href="detalhes.html?id=${v.id}">Detalhes</a> | 
            <a href="edicao.html?id=${v.id}">Editar/OS</a> | 
            <button class="btn btn-danger btn-excluir" data-id="${v.id}" data-placa="${v.placa}">Excluir</button>
          </td>
        `;
        tbodyListagem.appendChild(tr);
      });

      // Vincula a confirmação de exclusão nos botões renderizados
      tbodyListagem.querySelectorAll('.btn-excluir').forEach(btn => {
        btn.addEventListener('click', () => {
          const id = btn.getAttribute('data-id');
          const placa = btn.getAttribute('data-placa');
          const confirmacao = confirm(`Tem certeza de que deseja remover o veículo de placa ${placa} da frota?`);
          if (confirmacao) {
            excluirVeiculo(id);
            renderizarTabela();
          }
        });
      });
    }

    renderizarTabela();
  }

  // --- TELA: Ficha Técnica / Detalhes (detalhes.html) ---
  const cardDetalhes = document.querySelector('#card-detalhes');
  if (cardDetalhes) {
    const id = obterParametroUrl('id');
    const veiculos = obterVeiculos();
    const veiculo = id ? obterVeiculoPorId(id) : veiculos[0]; // Se não passar id, carrega o primeiro para visualização

    const titulo = document.querySelector('#titulo-detalhes');
    const tbodyHistorico = document.querySelector('#tabela-historico-corpo');
    const btnNovaManutencao = document.querySelector('#btn-nova-manutencao');

    if (!veiculo) {
      titulo.textContent = 'Veículo Não Encontrado';
      cardDetalhes.innerHTML = `
        <p>O veículo solicitado não foi encontrado no sistema.</p>
        <p><a href="listagem.html" class="btn" style="text-decoration: none; display: inline-block; margin-top: 1rem;">Voltar para a Listagem</a></p>
      `;
      if (btnNovaManutencao) btnNovaManutencao.style.display = 'none';
      if (tbodyHistorico) tbodyHistorico.innerHTML = '<tr><td colspan="4" class="tabela-vazia">Nenhum dado disponível.</td></tr>';
      return;
    }

    titulo.textContent = `Ficha Técnica - Placa ${veiculo.placa}`;
    const kmFormatado = Number(veiculo.km || 0).toLocaleString('pt-BR') + ' km';

    cardDetalhes.innerHTML = `
      <p><strong>Modelo:</strong> ${veiculo.modelo}</p>
      <p><strong>Ano:</strong> ${veiculo.ano}</p>
      <p><strong>Quilometragem Atual:</strong> ${kmFormatado}</p>
      <p><strong>Status Atual:</strong> <span class="badge badge-${veiculo.status}">${formatarStatus(veiculo.status)}</span></p>
      <p><strong>Motorista Atribuído:</strong> ${veiculo.motorista || 'Não atribuído'}</p>
    `;

    if (btnNovaManutencao) {
      btnNovaManutencao.href = `edicao.html?id=${veiculo.id}`;
    }

    // Histórico de manutenções
    if (tbodyHistorico) {
      tbodyHistorico.innerHTML = '';
      const historico = veiculo.manutencoes || [];

      if (historico.length === 0) {
        tbodyHistorico.innerHTML = `
          <tr>
            <td colspan="4" class="tabela-vazia">Nenhum histórico de manutenção registrado para este veículo.</td>
          </tr>
        `;
      } else {
        historico.forEach(m => {
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td>${m.data}</td>
            <td><span class="badge badge-${m.tipo === 'Urgência' ? 'urgencia' : 'manutencao'}">${m.tipo}</span></td>
            <td>${m.descricao}</td>
            <td><strong>${formatarMoeda(m.custo)}</strong></td>
          `;
          tbodyHistorico.appendChild(tr);
        });
      }
    }
  }

  // --- TELA: Registro de Manutenção / Edição (edicao.html) ---
  const formEdicao = document.querySelector('#form-edicao-manutencao');
  if (formEdicao) {
    const inputCusto = document.querySelector('#custo');
    const campoDescricao = document.querySelector('#descricao-manutencao');
    const elementoContador = document.querySelector('#contador-caracteres');
    const inputVeiculoId = document.querySelector('#veiculo-id');
    const inputVeiculoTexto = document.querySelector('#veiculo');
    const selectVeiculoDropdown = document.querySelector('#veiculo-dropdown');
    const grupoTexto = document.querySelector('#grupo-veiculo-texto');
    const grupoDropdown = document.querySelector('#grupo-veiculo-select');
    const msgSucesso = document.querySelector('#mensagem-sucesso');

    // Aplicação da máscara de moeda no custo
    aplicarMascaraMoeda(inputCusto);

    // Contador dinâmico de caracteres
    if (campoDescricao && elementoContador) {
      campoDescricao.addEventListener('input', () => {
        const limite = 200;
        const atual = campoDescricao.value.length;
        elementoContador.textContent = `${atual}/${limite} caracteres`;
        elementoContador.style.color = atual > limite ? '#dc2626' : '#64748b';
      });
    }

    // Detecção do veículo selecionado (por URL ou por dropdown)
    const idUrl = obterParametroUrl('id');
    const veiculos = obterVeiculos();
    let veiculoAtual = idUrl ? obterVeiculoPorId(idUrl) : null;

    if (veiculoAtual) {
      inputVeiculoId.value = veiculoAtual.id;
      inputVeiculoTexto.value = `${veiculoAtual.modelo} (Placa: ${veiculoAtual.placa})`;
      const selectStatus = document.querySelector('#novo-status');
      if (selectStatus && veiculoAtual.status) {
        selectStatus.value = veiculoAtual.status;
      }
    } else {
      // Se não veio ID na URL, exibe dropdown com todos os veículos cadastrados
      grupoTexto.style.display = 'none';
      grupoDropdown.style.display = 'block';

      selectVeiculoDropdown.innerHTML = '<option value="">Selecione um veículo da frota...</option>';
      veiculos.forEach(v => {
        const opt = document.createElement('option');
        opt.value = v.id;
        opt.textContent = `${v.placa} - ${v.modelo}`;
        selectVeiculoDropdown.appendChild(opt);
      });
    }

    // Limpa erros ao digitar
    formEdicao.querySelectorAll('input, select, textarea').forEach(c => {
      c.addEventListener('input', () => {
        c.classList.remove('campo-erro');
        const msgAnterior = c.parentElement.querySelector('.mensagem-erro');
        if (msgAnterior) msgAnterior.remove();
      });
    });

    // Submissão da Ordem de Serviço
    formEdicao.addEventListener('submit', (event) => {
      event.preventDefault();
      let valido = true;

      // Limpa mensagens de erro anteriores
      formEdicao.querySelectorAll('.mensagem-erro').forEach(m => m.remove());
      formEdicao.querySelectorAll('.campo-erro').forEach(c => c.classList.remove('campo-erro'));

      const idFinal = veiculoAtual ? veiculoAtual.id : selectVeiculoDropdown.value;
      if (!idFinal) {
        valido = false;
        selectVeiculoDropdown.classList.add('campo-erro');
        const msg = document.createElement('span');
        msg.className = 'mensagem-erro';
        msg.textContent = 'Por favor, selecione um veículo.';
        selectVeiculoDropdown.parentElement.appendChild(msg);
      }

      if (!inputCusto.value.trim()) {
        valido = false;
        inputCusto.classList.add('campo-erro');
        const msg = document.createElement('span');
        msg.className = 'mensagem-erro';
        msg.textContent = 'Informe o custo do serviço.';
        inputCusto.parentElement.appendChild(msg);
      }

      if (!campoDescricao.value.trim()) {
        valido = false;
        campoDescricao.classList.add('campo-erro');
        const msg = document.createElement('span');
        msg.className = 'mensagem-erro';
        msg.textContent = 'A descrição do laudo/serviço é obrigatória.';
        campoDescricao.parentElement.appendChild(msg);
      }

      if (!valido) return;

      // Gravação da nova Ordem de Serviço no veículo
      const veiculosLista = obterVeiculos();
      const veiculoIdx = veiculosLista.findIndex(v => String(v.id) === String(idFinal));

      if (veiculoIdx !== -1) {
        const dataHoje = new Date().toLocaleDateString('pt-BR');
        const tipoServico = document.querySelector('#tipo').value;
        const novoStatus = document.querySelector('#novo-status').value;
        const custoNumerico = converterMoedaParaNumero(inputCusto.value);

        const novaManutencao = {
          id: 'os-' + Date.now(),
          data: dataHoje,
          tipo: tipoServico,
          descricao: campoDescricao.value.trim(),
          custo: custoNumerico
        };

        if (!veiculosLista[veiculoIdx].manutencoes) {
          veiculosLista[veiculoIdx].manutencoes = [];
        }

        // Adiciona a manutenção no início do histórico
        veiculosLista[veiculoIdx].manutencoes.unshift(novaManutencao);
        veiculosLista[veiculoIdx].status = novoStatus;
        veiculosLista[veiculoIdx].ultimaRevisao = dataHoje;

        salvarVeiculos(veiculosLista);

        if (msgSucesso) {
          msgSucesso.textContent = 'Ordem de serviço registrada com sucesso! Redirecionando...';
          msgSucesso.style.display = 'block';
        }

        setTimeout(() => {
          window.location.href = `detalhes.html?id=${idFinal}`;
        }, 1200);
      }
    });
  }

});