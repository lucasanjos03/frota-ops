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
