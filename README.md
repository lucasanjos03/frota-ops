# FleetOps — Gestão de Frota e Manutenção

## 👥 Integrantes da Dupla

- **Felipe Sorrentino**
- **Lucas Anjos**

---

## 📖 Descrição do Projeto

O **FleetOps** é uma aplicação web desenvolvida para o gerenciamento operacional e controle de manutenção de frotas de veículos.

O projeto é a evolução de um protótipo estático (HTML, CSS e JavaScript) integrado ao ecossistema **Java** com **Spring Boot** e renderização dinâmica no servidor via **Thymeleaf**. Os dados do sistema são mantidos e manipulados em memória durante o ciclo de execução da aplicação através da camada de serviço (`VeiculoService`), dispensando a necessidade de banco de dados nesta etapa.

---

## 🛠️ Tecnologias Utilizadas

- **Java 21**
- **Spring Boot** (Spring Web MVC, Thymeleaf Starter, DevTools)
- **Thymeleaf** (Template Engine com renderização no servidor)
- **HTML5** & **CSS3** (Layout responsivo com identidade visual corporativa)
- **JavaScript** (Aprimoramentos de interface, formatação de inputs e notificações)
- **Maven** (Gerenciador de dependências e build)

---

## 🚀 Instruções para Executar o Projeto

### Pré-requisitos

- **Java Development Kit (JDK 21)** instalado e configurado nas variáveis de ambiente.
- Git instalado.

### Passos para execução

1. **Clone o repositório:**

   ```bash
   git clone https://github.com/lucasanjos03/frota-ops.git
   cd frota-ops
   ```

2. **Execute a aplicação utilizando o Maven Wrapper:**
   - No Linux / macOS:
     ```bash
     ./mvnw spring-boot:run
     ```
   - No Windows (cmd/PowerShell):
     ```cmd
     mvnw.cmd spring-boot:run
     ```

3. **Acesse no navegador:**
   Após a inicialização do Spring Boot, acesse a aplicação em:
   ```
   http://localhost:8080
   ```

---

## 🌐 Principais Rotas Disponíveis

| Método | Rota                      | Descrição                                                                                                                   |
| ------ | ------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `GET`  | `/`                       | **Dashboard / Visão Geral**: Resumo operacional, indicadores da frota e tabela de alertas de manutenções próximas do prazo. |
| `GET`  | `/veiculos`               | **Listagem de Veículos**: Tabela completa com filtros por placa/modelo e por status operacional.                            |
| `GET`  | `/veiculos/novo`          | **Formulário de Cadastro**: Tela para inclusão de novos veículos na frota.                                                  |
| `POST` | `/veiculos/novo`          | **Processamento do Cadastro**: Recebe os dados do veículo, armazena em memória e redireciona para a listagem.               |
| `GET`  | `/veiculos/detalhes/{id}` | **Detalhes do Veículo**: Ficha cadastral com identificação completa e histórico de manutenções registradas.                 |
| `GET`  | `/veiculos/editar/{id}`   | **Formulário de Edição**: Formulário pré-preenchido com os dados atuais do veículo para alteração.                          |
| `POST` | `/veiculos/editar/{id}`   | **Processamento da Edição**: Atualiza as informações do veículo em memória e redireciona para a página de detalhes.         |
| `POST` | `/veiculos/excluir/{id}`  | **Exclusão de Veículo**: Remove o veículo selecionado da memória e redireciona para a listagem.                             |
| `GET`  | `/login`                  | **Tela de Login**: Interface de autenticação de usuários no sistema.                                                        |
| `POST` | `/login`                  | **Acesso ao Sistema**: Processa o login e redireciona para o dashboard.                                                     |

---

## 📁 Estrutura do Projeto

```
src/main/
├── java/br/edu/cesmac/frota_ops/
│   ├── FrotaOpsApplication.java       # Classe principal de inicialização
│   ├── auth/
│   │   └── AuthController.java        # Controlador de login e autenticação
│   ├── home/
│   │   └── HomeController.java        # Controlador do Dashboard e visão geral
│   └── veiculo/
│       ├── Veiculo.java               # Modelo do veículo
│       ├── Manutencao.java            # Modelo de histórico de manutenção
│       ├── VeiculoService.java        # Camada de regras de negócio e dados em memória
│       └── VeiculoController.java     # Controlador CRUD de veículos
└── resources/
    ├── static/
    │   ├── css/styles.css             # Folhas de estilo da interface
    │   └── js/script.js               # Scripts auxiliares de UI (máscaras e toast)
    └── templates/
        ├── auth/login.html            # Tela de autenticação
        ├── home/index.html            # Dashboard com alertas operacionais
        └── veiculo/
            ├── list.html              # Listagem com busca e filtros
            ├── novo.html              # Cadastro de veículo (form binding)
            ├── editar.html            # Edição cadastral
            └── detalhes.html          # Visualização e histórico
```
