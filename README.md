# FleetOps — Gestão de Frota e Manutenção

## 👥 Integrantes da Dupla

- **Felipe Sorrentino**
- **Lucas Anjos**

---

## 📖 Descrição do Projeto

O **FleetOps** é uma aplicação web monolítica desenvolvida para o gerenciamento operacional e controle de manutenção de frotas de veículos.

O projeto utiliza **Java 21** com **Spring Boot**, renderização dinâmica no servidor via **Thymeleaf** e persistência em banco de dados relacional **PostgreSQL**. A arquitetura é dividida por componente de domínio (`veiculo`, `manutencao`, `motorista`, `tag`, `home`, `auth`), onde cada componente encapsula sua Entidade, Repository, Service e Controller, contando com:

- **Versionamento de Banco de Dados com Flyway** (`V1` e `V2`) e validação de schema via Hibernate (`ddl-auto=validate`).
- **Persistência com Spring Data JPA**, substituindo o armazenamento em memória por repositórios JPA.
- **Validação Backend com Jakarta Bean Validation** (`@NotBlank`, `@Size`, `@NotNull`, `@Min`, `@Valid` e `BindingResult`) integrada visualmente aos formulários Thymeleaf (`th:errors` e `th:errorclass`).
- **Feedback de Operações** com mensagens temporárias (*Flash Attributes* via `RedirectAttributes`) e tratamento de registros inexistentes.
- **Relacionamentos JPA**:
  - **`@ManyToOne` (1:N)**: Associação de veículos a um **Motorista** responsável (`<select>` nos formulários) e vínculo do histórico de **Manutenção** ao **Veículo**.
  - **`@ManyToMany` (N:N)**: Classificação de veículos com múltiplas **Tags Operacionais** via tabela associativa (`veiculo_tags`) e seleção por *checkboxes*.

---

## 🛠️ Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.4.4** (Spring Web MVC, Spring Data JPA, Validation, Thymeleaf, DevTools)
- **PostgreSQL** (Banco de dados relacional)
- **Flyway** (Migração e versionamento automático do schema e dados iniciais)
- **Hibernate / Jakarta Persistence (JPA)** (Mapeamento Objeto-Relacional — ORM)
- **Jakarta Bean Validation** (Validação de dados no backend)
- **Thymeleaf** (Template Engine com renderização no servidor)
- **HTML5**, **CSS3** & **JavaScript** (Layout responsivo, máscaras e interações de UI)
- **Maven** (Gerenciador de dependências e build)

---

## 🚀 Instruções para Executar o Projeto

### Pré-requisitos

- **Java Development Kit (JDK 21)** instalado e configurado nas variáveis de ambiente.
- **PostgreSQL** instalado e em execução localmente (porta padrão `5432`).
- **Git** instalado.

### Configuração do Banco de Dados

1. Crie um banco de dados chamado `frota-ops` no seu PostgreSQL:
   ```sql
   CREATE DATABASE "frota-ops";
   ```
2. Por padrão, o arquivo [`application.properties`](src/main/resources/application.properties) conecta em `jdbc:postgresql://localhost:5432/frota-ops` com usuário `postgres` e senha `postgres` (você também pode sobrescrever via variáveis de ambiente `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`).
3. **Não é necessário rodar scripts SQL manualmente**: ao iniciar a aplicação, o **Flyway** executa automaticamente as migrations `V1__create_initial_tables.sql` e `V2__add_motoristas_and_tags.sql`, criando todas as tabelas e inserindo os dados iniciais de exemplo.

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
     .\mvnw.cmd spring-boot:run
     ```

3. **Acesse no navegador:**
   Após a inicialização do Spring Boot (configurado na porta `8081`), acesse a aplicação em:
   ```
   http://localhost:8081
   ```

---

## 🌐 Principais Rotas Disponíveis

| Método | Rota                         | Descrição                                                                                                                               |
| ------ | ---------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `GET`  | `/`                          | **Dashboard / Visão Geral**: Indicadores operacionais (KPIs) da frota e tabela de alertas de manutenções próximas do prazo.             |
| `GET`  | `/veiculos`                  | **Listagem de Veículos**: Tabela com busca por placa, modelo ou motorista, filtro por status e exibição de tags e feedbacks flash.      |
| `GET`  | `/veiculos/novo`             | **Formulário de Veículo**: Tela para inclusão de novos veículos com seleção de motorista (`@ManyToOne`) e tags (`@ManyToMany`).         |
| `POST` | `/veiculos/novo`             | **Cadastro de Veículo**: Valida os dados (`@Valid` + `BindingResult`), aplica regras no `VeiculoService` e persiste no banco.           |
| `GET`  | `/veiculos/detalhes/{id}`    | **Detalhes do Veículo**: Ficha completa do veículo, motorista alocado, tags operacionais e histórico de manutenções.                    |
| `GET`  | `/veiculos/editar/{id}`      | **Edição de Veículo**: Carrega os dados atuais do veículo (e seus relacionamentos) para alteração.                                      |
| `POST` | `/veiculos/editar/{id}`      | **Atualização de Veículo**: Valida os dados enviados, atualiza o registro no banco via `VeiculoService` e redireciona com feedback.     |
| `POST` | `/veiculos/excluir/{id}`     | **Exclusão de Veículo**: Remove o veículo do banco de dados e redireciona para a listagem com mensagem de confirmação.                  |
| `GET`  | `/motoristas`                | **Listagem de Motoristas**: Exibe todos os motoristas cadastrados (`MotoristaController`).                                              |
| `GET`  | `/motoristas/novo`           | **Formulário de Motorista**: Tela de cadastro de novo motorista.                                                                        |
| `POST` | `/motoristas/novo`           | **Cadastro de Motorista**: Valida e salva o motorista via `MotoristaService`.                                                           |
| `GET`  | `/motoristas/editar/{id}`    | **Edição de Motorista**: Carrega os dados do motorista para edição.                                                                     |
| `POST` | `/motoristas/editar/{id}`    | **Atualização de Motorista**: Atualiza o motorista via `MotoristaService`.                                                              |
| `POST` | `/motoristas/excluir/{id}`   | **Exclusão de Motorista**: Exclui o motorista do banco de dados.                                                                        |
| `GET`  | `/manutencoes`               | **Listagem de Manutenções**: Exibe o histórico de manutenções da frota com filtro por veículo (`ManutencaoController`).                 |
| `GET`  | `/manutencoes/novo`          | **Formulário de Manutenção**: Tela de registro de manutenção vinculada a um veículo.                                                    |
| `POST` | `/manutencoes/novo`          | **Cadastro de Manutenção**: Valida e registra a manutenção via `ManutencaoService`.                                                     |
| `GET`  | `/manutencoes/editar/{id}`   | **Edição de Manutenção**: Carrega os dados da manutenção para alteração.                                                                |
| `POST` | `/manutencoes/editar/{id}`   | **Atualização de Manutenção**: Atualiza o registro de manutenção via `ManutencaoService`.                                               |
| `POST` | `/manutencoes/excluir/{id}`  | **Exclusão de Manutenção**: Remove o registro de manutenção.                                                                            |
| `GET`  | `/tags`                      | **Listagem de Tags**: Exibe todas as tags operacionais cadastradas (`TagController`).                                                   |
| `GET`  | `/tags/novo`                 | **Formulário de Tag**: Tela para criar nova tag operacional.                                                                            |
| `POST` | `/tags/novo`                 | **Cadastro de Tag**: Valida e persiste a tag via `TagService`.                                                                          |
| `GET`  | `/tags/editar/{id}`          | **Edição de Tag**: Carrega a tag para edição.                                                                                           |
| `POST` | `/tags/editar/{id}`          | **Atualização de Tag**: Atualiza o nome da tag via `TagService`.                                                                        |
| `POST` | `/tags/excluir/{id}`         | **Exclusão de Tag**: Remove a tag operacional.                                                                                          |
| `GET`  | `/usuarios`                  | **Listagem de Usuários**: Exibe todos os usuários (administradores, operadores e motoristas vinculados).                                |
| `GET`  | `/usuarios/novo`             | **Formulário de Usuário**: Tela de cadastro de novo usuário do sistema.                                                                 |
| `POST` | `/usuarios/novo`             | **Cadastro de Usuário**: Valida e salva o usuário via `UsuarioService`.                                                                 |
| `GET`  | `/usuarios/editar/{id}`      | **Edição de Usuário**: Carrega os dados do usuário para edição.                                                                         |
| `POST` | `/usuarios/editar/{id}`      | **Atualização de Usuário**: Atualiza o usuário via `UsuarioService`.                                                                    |
| `POST` | `/usuarios/excluir/{id}`     | **Exclusão de Usuário**: Remove o usuário do banco de dados.                                                                            |
| `GET`  | `/login`                     | **Tela de Login**: Interface de autenticação validando contra a tabela `usuario`.                                                       |
| `POST` | `/login`                     | **Acesso ao Sistema**: Autentica `username`/`email` e `senha` no banco via `UsuarioService`.                                            |
| `GET`  | `/cadastro`                  | **Criar Conta**: Tela de autocadastro de usuário acessível a partir do login.                                                           |
| `POST` | `/cadastro`                  | **Processar Autocadastro**: Cria a conta na tabela `usuario` e redireciona para o login.                                                |

---

## 📁 Estrutura do Projeto (Dividida por Componente)

```
src/
├── main/
│   ├── java/br/edu/cesmac/frota_ops/
│   │   ├── FrotaOpsApplication.java            # Classe principal de inicialização
│   │   ├── auth/
│   │   │   └── AuthController.java             # Controlador de login e autenticação
│   │   ├── home/
│   │   │   └── HomeController.java             # Controlador do Dashboard e visão geral
│   │   ├── veiculo/
│   │   │   ├── Veiculo.java                    # Entidade JPA Veiculo
│   │   │   ├── VeiculoRepository.java          # Repositório Spring Data JPA de Veiculo
│   │   │   ├── VeiculoService.java             # Regras de negócio e persistência de Veiculo
│   │   │   └── VeiculoController.java          # Controlador CRUD de veículos (injeta apenas VeiculoService)
│   │   ├── manutencao/
│   │   │   ├── Manutencao.java                 # Entidade JPA Manutencao
│   │   │   ├── ManutencaoRepository.java       # Repositório Spring Data JPA de Manutencao
│   │   │   ├── ManutencaoService.java          # Regras de negócio e persistência de Manutencao
│   │   │   └── ManutencaoController.java       # Controlador CRUD de manutenções (injeta apenas ManutencaoService)
│   │   ├── motorista/
│   │   │   ├── Motorista.java                  # Entidade JPA Motorista
│   │   │   ├── MotoristaRepository.java        # Repositório Spring Data JPA de Motorista
│   │   │   ├── MotoristaService.java           # Regras de negócio e persistência de Motorista
│   │   │   └── MotoristaController.java        # Controlador CRUD de motoristas (injeta apenas MotoristaService)
│   │   ├── tag/
│   │   │   ├── Tag.java                        # Entidade JPA Tag
│   │   │   ├── TagRepository.java              # Repositório Spring Data JPA de Tag
│   │   │   ├── TagService.java                 # Regras de negócio e persistência de Tag
│   │   │   └── TagController.java              # Controlador CRUD de tags (injeta apenas TagService)
│   │   └── usuario/
│   │       ├── Usuario.java                    # Entidade JPA Usuario (1:1 opcional com Motorista)
│   │       ├── UsuarioRepository.java          # Repositório Spring Data JPA de Usuario
│   │       ├── UsuarioService.java             # Regras de negócio, autenticação e persistência de Usuario
│   │       └── UsuarioController.java          # Controlador CRUD de usuários (injeta apenas UsuarioService)
│   └── resources/
│       ├── application.properties              # Configurações de porta (8081), PostgreSQL, JPA (validate) e Flyway
│       ├── application-dev.properties          # Configurações de ambiente de desenvolvimento
│       ├── db/migration/
│       │   ├── V1__create_initial_tables.sql            # Migration inicial (tabelas veiculo, manutencao, usuario e seed)
│       │   ├── V2__add_motoristas_and_tags.sql          # Migration de relacionamentos (motorista, tags, veiculo_tags e seed)
│       │   ├── V3__link_motoristas_to_usuarios.sql      # Migration de vínculo obrigatório Motorista -> Usuario
│       │   ├── V4__add_perfil_to_usuario.sql            # Migration de perfis de acesso (ADMIN, OPERADOR, MOTORISTA)
│       │   └── V5__add_data_modificacao_to_usuario.sql  # Migration de data_modificacao e Soft Delete em usuario
│       ├── static/
│       │   ├── css/styles.css                  # Folha de estilos principal
│       │   └── js/script.js                    # Scripts auxiliares de UI
│       └── templates/
│           ├── auth/                           # Telas de login e autocadastro de conta (login, cadastro)
│           ├── home/index.html                 # Dashboard com KPIs e alertas operacionais
│           ├── veiculo/                        # Telas do componente Veículo (list, novo, editar, detalhes)
│           ├── motorista/                      # Telas do componente Motorista (list, novo, editar)
│           ├── manutencao/                     # Telas do componente Manutenção (list, novo, editar)
│           ├── tag/                            # Telas do componente Tag (list, novo, editar)
│           └── usuario/                        # Telas do componente Usuário (list, novo, editar)
└── test/
    └── java/br/edu/cesmac/frota_ops/
        └── FrotaOpsApplicationTests.java       # Testes de integração de todos os componentes
```
