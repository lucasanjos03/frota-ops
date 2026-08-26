document.addEventListener('DOMContentLoaded', () => {

  // 1. Validação visual no Formulário de Cadastro
  const formCadastro = document.querySelector('#form-cadastro-veiculo');
  if (formCadastro) {
    formCadastro.addEventListener('submit', (event) => {
      let valido = true;
      const camposObrigatorios = formCadastro.querySelectorAll('[required]');

      camposObrigatorios.forEach(campo => {
        // Limpa erros anteriores
        campo.classList.remove('campo-erro');
        const msgAnterior = campo.parentElement.querySelector('.mensagem-erro');
        if (msgAnterior) {
          msgAnterior.remove();
        }

        // Valida preenchimento
        if (!campo.value.trim()) {
          valido = false;
          campo.classList.add('campo-erro');

          const msg = document.createElement('span');
          msg.className = 'mensagem-erro';
          msg.textContent = 'Este campo é obrigatório.';
          campo.parentElement.appendChild(msg);
        }
      });

      if (!valido) {
        event.preventDefault(); // Impede o envio se houver campos vazios
      }
    });
  }

  // 2. Confirmação de Ação Crítica (Remover / Desativar Veículo)
  const botoesExcluir = document.querySelectorAll('.btn-excluir');
  botoesExcluir.forEach(botao => {
    botao.addEventListener('click', (event) => {
      const confirmacao = confirm('Tem certeza de que deseja remover este veículo da frota?');
      if (!confirmacao) {
        event.preventDefault();
      }
    });
  });

  // 3. Contador de Caracteres no Campo de Observações
  const campoDescricao = document.querySelector('#descricao-manutencao');
  const elementoContador = document.querySelector('#contador-caracteres');

  if (campoDescricao && elementoContador) {
    campoDescricao.addEventListener('input', () => {
      const limite = 200;
      const atual = campoDescricao.value.length;
      elementoContador.textContent = `${atual}/${limite} caracteres`;

      if (atual > limite) {
        elementoContador.style.color = '#dc2626';
      } else {
        elementoContador.style.color = '#64748b';
      }
    });
  }

});