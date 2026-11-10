# FleetOps — Gestão de Frota e Manutenção

## 👥 Integrantes da Dupla

- **Felipe Sorrentino**
- **Lucas Anjos**

---

## 📖 Descrição do Projeto

O **FleetOps** é uma aplicação web monolítica desenvolvida para o gerenciamento operacional e controle de manutenção de frotas de veículos.

O projeto utiliza **Java 21** com **Spring Boot**, renderização dinâmica no servidor via **Thymeleaf** e persistência em banco de dados relacional **PostgreSQL**. A arquitetura é dividida por componente de domínio (`veiculo`, `manutencao`, `motorista`, `tag`, `usuario`, `home`, `auth`), onde cada componente encapsula sua Entidade, Repository, Service e Controller, contando com:

- **Versionamento de Banco de Dados com Flyway** (`V1` a `V5`) e validação de schema via Hibernate (`ddl-auto=validate`).
- **Persistência com Spring Data JPA**, substituindo o armazenamento em memória por repositórios JPA.
- **Validação Backend com Jakarta Bean Validation** (`@NotBlank`, `@Size`, `@NotNull`, `@Min`, `@Valid` e `BindingResult`) integrada visualmente aos formulários Thymeleaf (`th:errors` e `th:errorclass`).
- **Feedback de Operações** com mensagens temporárias (*Flash Attributes* via `RedirectAttributes`) e tratamento de registros inexistentes.
- **Autenticação e Controle de Acesso**: `HandlerInterceptor` próprio (`AuthInterceptor` + `WebConfig`, sem Spring Security), que exige usuário autenticado em todas as rotas — exceto as públicas (`/login`, recursos estáticos e `/error`). O login grava o usuário na sessão; o logout (`POST /logout`) invalida a sessão.
- **Senhas com Hash BCrypt**: as senhas são armazenadas como hash BCrypt (`spring-security-crypto`). Contas legadas em texto plano são convertidas automaticamente para hash no primeiro login bem-sucedido (*upgrade transparente*).
- **DTOs de Leitura (*records*)**: as views e o JavaScript recebem apenas dados já agregados; nenhuma entidade JPA trafega para a camada de apresentação.
- **Relacionamentos JPA**:
  - **`@OneToOne` (1:1)**: Vínculo entre **Motorista** e **Usuário** do sistema (`usuario_id`), garantindo que todo motorista possua conta vinculada.
  - **`@ManyToOne` (1:N)**: Associação de veículos a um **Motorista** responsável (`<select>` nos formulários) e vínculo do histórico de **Manutenção** ao **Veículo**.
  - **`@ManyToMany` (N:N)**: Classificação de veículos com múltiplas **Tags Operacionais** via tabela associativa (`veiculo_tags`) e seleção por *checkboxes*.
- **Controle de Perfis de Acesso (`ADMIN`, `OPERADOR`, `MOTORISTA`) e Soft Delete**:
  - **`ADMIN`**: Acesso total a todos os módulos e operações do sistema.
  - **`OPERADOR`**: Visualização da frota, motoristas, manutenções e tags, com edição restrita ao próprio usuário.
  - **`MOTORISTA`**: Além das permissões de visualização e edição do próprio usuário/motorista, gerencia seus próprios veículos, manutenções e tags operacionais.
  - **Exclusão Lógica (*Soft Delete*)**: A exclusão de usuário marca `ativo = false` e registra `data_modificacao`, ocultando o registro das listagens e bloqueando o login sem perda de integridade referencial.

---

## 🛠️ Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.4.4** (Spring Web MVC, Spring Data JPA, Validation, Thymeleaf, DevTools)
- **PostgreSQL** (Banco de dados relacional)
- **Flyway** (Migração e versionamento automático do schema e dados iniciais)
- **Hibernate / Jakarta Persistence (JPA)** (Mapeamento Objeto-Relacional — ORM)
- **Jakarta Bean Validation** (Validação de dados no backend)
- **Spring Security Crypto / BCrypt** (Hash de senhas — dependência isolada, sem a cadeia de filtros do Spring Security)
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
3. **Não é necessário rodar scripts SQL manualmente**: ao iniciar a aplicação, o **Flyway** executa automaticamente as migrations `V1` a `V5` em `src/main/resources/db/migration/`, criando todas as tabelas, relacionamentos, perfis de acesso e inserindo os dados iniciais de exemplo.

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
| `GET`  | `/veiculos`                  | **Listagem de Veículos**: Tabela **paginada** (10 por página) com busca por placa, modelo ou motorista, filtros por status, situação, revisão e vínculo, e exibição de tags e feedbacks flash. |
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
| `GET`  | `/manutencoes`               | **Histórico de Manutenções**: Tabela **paginada** (10 por página) com filtros por veículo, serviço (texto) e período, além dos indicadores e do painel de revisões do escopo (`ManutencaoController`). |
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
| `GET`  | `/usuarios`                  | **Listagem de Usuários**: Exibe todos os usuários ativos (administradores, operadores e motoristas vinculados).                         |
| `GET`  | `/usuarios/novo`             | **Formulário de Usuário**: Tela de cadastro de novo usuário do sistema.                                                                 |
| `POST` | `/usuarios/novo`             | **Cadastro de Usuário**: Valida e salva o usuário via `UsuarioService`.                                                                 |
| `GET`  | `/usuarios/editar/{id}`      | **Edição de Usuário**: Carrega os dados do usuário para edição.                                                                         |
| `POST` | `/usuarios/editar/{id}`      | **Atualização de Usuário**: Atualiza o usuário via `UsuarioService`.                                                                    |
| `POST` | `/usuarios/excluir/{id}`     | **Exclusão Lógica de Usuário**: Realiza *Soft Delete* (`ativo = false` e atualiza `data_modificacao`), ocultando o usuário do sistema.  |
| `GET`  | `/login`                     | **Tela de Login**: Interface de autenticação (rota pública).                                                                            |
| `POST` | `/login`                     | **Acesso ao Sistema**: Autentica `username`/`email` e `senha` via `UsuarioService`, conferindo o hash **BCrypt** e gravando o usuário na sessão. |
| `POST` | `/logout`                    | **Sair**: Invalida a sessão (`session.invalidate()`) e redireciona para o login.                                                        |
| `GET`  | `/cadastro`                  | **Criar Usuário**: Formulário de criação de usuário **restrito a ADMIN** (o autocadastro público foi desativado).                       |
| `POST` | `/cadastro`                  | **Cadastrar Usuário**: Cria a conta na tabela `usuario` (**restrito a ADMIN**) e redireciona para `/usuarios`.                          |

---

## 📁 Estrutura do Projeto (Dividida por Componente)

```
src/
├── main/
│   ├── java/br/edu/cesmac/frota_ops/
│   │   ├── FrotaOpsApplication.java            # Classe principal de inicialização
│   │   ├── auth/
│   │   │   ├── AuthController.java             # Controlador de login, logout e cadastro (restrito a ADMIN)
│   │   │   ├── AuthInterceptor.java            # Interceptor que exige usuário autenticado nas rotas protegidas
│   │   │   ├── WebConfig.java                  # Registro do interceptor e definição das rotas públicas
│   │   │   └── SessionControllerAdvice.java    # Expõe o usuário logado e o perfil às views
│   │   ├── home/
│   │   │   ├── HomeController.java             # Controlador do Dashboard e visão geral
│   │   │   └── dashboard/
│   │   │       ├── DashboardService.java       # Agregações do dashboard (KPIs, séries, alertas)
│   │   │       ├── DashboardDTO.java           # Agregado completo do dashboard
│   │   │       ├── IndicadoresDTO.java         # Indicadores numéricos (cartões)
│   │   │       ├── DistribuicaoStatusDTO.java  # Distribuição da frota por status (gráfico de rosca)
│   │   │       ├── SerieMensalDTO.java         # Série mensal de manutenções
│   │   │       ├── VeiculoPorTagDTO.java       # Contagem de veículos por tag
│   │   │       ├── RevisaoVeiculoDTO.java      # Linha de próxima revisão (reuso nos módulos)
│   │   │       ├── CnhAlertaDTO.java           # Motorista com CNH vencida/a vencer
│   │   │       └── AlertaDTO.java              # Alerta operacional priorizado
│   │   ├── veiculo/
│   │   │   ├── Veiculo.java                    # Entidade JPA Veiculo
│   │   │   ├── VeiculoRepository.java          # Repositório Spring Data JPA (agregações + busca paginada)
│   │   │   ├── VeiculoService.java             # Regras de negócio, escopo por perfil e indicadores
│   │   │   ├── VeiculoController.java          # Controlador CRUD de veículos (listagem paginada)
│   │   │   ├── VeiculoFiltro.java              # Critérios de filtro da listagem (record)
│   │   │   ├── VeiculoPaginaDTO.java           # Resumo analítico da tela
│   │   │   ├── VeiculoIndicadoresDTO.java      # Indicadores (cartões)
│   │   │   └── VeiculoQuilometragemDTO.java    # Ranking de quilometragem
│   │   ├── manutencao/
│   │   │   ├── Manutencao.java                 # Entidade JPA Manutencao
│   │   │   ├── ManutencaoRepository.java       # Repositório Spring Data JPA (agregações + histórico paginado)
│   │   │   ├── ManutencaoService.java          # Regras de negócio, resumo analítico e histórico paginado
│   │   │   ├── ManutencaoController.java       # Controlador CRUD de manutenções (histórico paginado, 10/pág.)
│   │   │   ├── ManutencaoFiltro.java           # Critérios de filtro do histórico (record)
│   │   │   ├── ManutencaoPaginaDTO.java        # Resumo analítico da tela
│   │   │   ├── ManutencaoIndicadoresDTO.java   # Indicadores (histórico realizado x previsão de revisão)
│   │   │   ├── ManutencaoMesDTO.java           # Mês da série mensal (com intervalo exato)
│   │   │   └── ManutencaoRankingVeiculoDTO.java # Ranking de veículos por nº de registros
│   │   ├── motorista/
│   │   │   ├── Motorista.java                  # Entidade JPA Motorista (1:1 com Usuario)
│   │   │   ├── MotoristaRepository.java        # Repositório Spring Data JPA (agregações + busca filtrada)
│   │   │   ├── MotoristaService.java           # Regras de negócio, resumo e sincronização com Usuario
│   │   │   ├── MotoristaController.java        # Controlador CRUD de motoristas
│   │   │   ├── MotoristaFiltro.java            # Critérios de filtro da listagem (record)
│   │   │   ├── MotoristaPaginaDTO.java         # Resumo analítico da tela
│   │   │   ├── MotoristaIndicadoresDTO.java    # Indicadores (cartões)
│   │   │   ├── MotoristaCategoriaDTO.java      # Distribuição por categoria de CNH
│   │   │   └── MotoristaDocumentoDTO.java      # CNHs vencidas / a vencer
│   │   ├── tag/
│   │   │   ├── Tag.java                        # Entidade JPA Tag
│   │   │   ├── TagRepository.java              # Repositório Spring Data JPA (contagem de veículos por tag)
│   │   │   ├── TagService.java                 # Regras de negócio e contagem por tag no escopo do perfil
│   │   │   ├── TagController.java              # Controlador CRUD de tags
│   │   │   └── TagPaginaDTO.java               # Resumo da tela de tags
│   │   └── usuario/
│   │       ├── Usuario.java                    # Entidade JPA Usuario (1:1 com Motorista, perfil e Soft Delete)
│   │       ├── UsuarioRepository.java          # Repositório Spring Data JPA de Usuario
│   │       ├── UsuarioService.java             # Autenticação (BCrypt), regras de negócio e persistência
│   │       ├── UsuarioController.java          # Controlador CRUD de usuários
│   │       └── UsuarioPaginaDTO.java           # Resumo de usuários ativos por perfil
│   └── resources/
│       ├── application.properties              # Configurações de porta (8081), PostgreSQL, JPA (validate) e Flyway
│       ├── db/migration/
│       │   ├── V1__create_initial_tables.sql            # Migration inicial (tabelas veiculo, manutencao, usuario e seed)
│       │   ├── V2__add_motoristas_and_tags.sql          # Migration de relacionamentos (motorista, tags, veiculo_tags e seed)
│       │   ├── V3__link_motoristas_to_usuarios.sql      # Migration de vínculo obrigatório Motorista -> Usuario
│       │   ├── V4__add_perfil_to_usuario.sql            # Migration de perfis de acesso (ADMIN, OPERADOR, MOTORISTA)
│       │   └── V5__add_data_modificacao_to_usuario.sql  # Migration de data_modificacao e Soft Delete em usuario
│       ├── static/
│       │   ├── css/styles.css                  # Folha de estilos principal (design system)
│       │   └── js/
│       │       ├── script.js                   # Scripts auxiliares de UI (gráficos, interações)
│       │       └── vendor/chart.umd.min.js     # Chart.js 4.4.7 (biblioteca local)
│       └── templates/
│           ├── fragments/layout.html           # Fragmentos reutilizáveis (head, header, breadcrumbs, flash, empty state)
│           ├── auth/                           # Telas de login e cadastro de usuário (login, cadastro)
│           ├── home/index.html                 # Dashboard com KPIs, gráficos e alertas operacionais
│           ├── veiculo/                        # Telas do componente Veículo (list, novo, editar, detalhes)
│           ├── motorista/                      # Telas do componente Motorista (list, novo, editar, detalhes)
│           ├── manutencao/                     # Telas do componente Manutenção (list, novo, editar)
│           ├── tag/                            # Telas do componente Tag (list, novo, editar)
│           └── usuario/                        # Telas do componente Usuário (list, novo, editar)
└── test/
    └── java/br/edu/cesmac/frota_ops/
        ├── FrotaOpsApplicationTests.java                  # Testes de integração gerais
        ├── auth/AuthPaginaTests.java                      # Login, logout e cadastro
        ├── home/dashboard/DashboardServiceTests.java      # Agregações do dashboard
        ├── veiculo/VeiculoPaginaTests.java                # Tela de veículos
        ├── motorista/MotoristaPaginaTests.java            # Tela de motoristas
        ├── manutencao/ManutencaoPaginaTests.java          # Tela de manutenções (paginação e filtros)
        ├── tag/TagPaginaTests.java                        # Tela de tags
        └── usuario/
            ├── UsuarioPaginaTests.java                    # Tela de usuários
            └── UsuarioSenhaTests.java                     # Hash BCrypt e upgrade de senha no login
```
