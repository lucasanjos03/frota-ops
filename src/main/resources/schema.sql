-- =============================================================================
-- FleetOps - DDL de Inicialização do Banco de Dados PostgreSQL
-- Sistema de Gestão de Frota e Manutenção
-- =============================================================================

-- 1. Criação de Enums (Opcional, mas recomendado para consistência de dados)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'status_veiculo_enum') THEN
        CREATE TYPE status_veiculo_enum AS ENUM (
            'Disponível',
            'Em trânsito',
            'Em manutenção',
            'Indisponível'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'situacao_manutencao_enum') THEN
        CREATE TYPE situacao_manutencao_enum AS ENUM (
            'Em dia',
            'Atenção',
            'Em manutenção'
        );
    END IF;
END $$;

-- 2. Tabela de Usuários (para autenticação / login)
CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de Veículos
CREATE TABLE IF NOT EXISTS veiculo (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(10) NOT NULL UNIQUE,
    modelo VARCHAR(120) NOT NULL,
    ano INTEGER NOT NULL,
    quilometragem BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'Disponível',
    situacao_manutencao VARCHAR(30) DEFAULT 'Em dia',
    ultima_revisao DATE,
    proxima_revisao DATE,
    quilometragem_ultima_revisao BIGINT,
    observacoes TEXT,
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_veiculo_ano CHECK (ano >= 1900),
    CONSTRAINT chk_veiculo_km CHECK (quilometragem >= 0),
    CONSTRAINT chk_veiculo_km_revisao CHECK (quilometragem_ultima_revisao IS NULL OR quilometragem_ultima_revisao >= 0)
);

-- 4. Tabela de Manutenções (Histórico de Manutenções do Veículo)
CREATE TABLE IF NOT EXISTS manutencao (
    id BIGSERIAL PRIMARY KEY,
    veiculo_id BIGINT NOT NULL,
    data_manutencao DATE NOT NULL,
    quilometragem BIGINT NOT NULL,
    servico_realizado VARCHAR(255) NOT NULL,
    responsavel VARCHAR(150) NOT NULL,
    observacao TEXT,
    data_registro TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_manutencao_veiculo
        FOREIGN KEY (veiculo_id) 
        REFERENCES veiculo(id) 
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_manutencao_km CHECK (quilometragem >= 0)
);

-- 5. Índices para Otimização de Consultas Frequentes
CREATE INDEX IF NOT EXISTS idx_veiculo_placa ON veiculo(placa);
CREATE INDEX IF NOT EXISTS idx_veiculo_status ON veiculo(status);
CREATE INDEX IF NOT EXISTS idx_veiculo_proxima_revisao ON veiculo(proxima_revisao);
CREATE INDEX IF NOT EXISTS idx_manutencao_veiculo_id ON manutencao(veiculo_id);
CREATE INDEX IF NOT EXISTS idx_manutencao_data ON manutencao(data_manutencao DESC);

-- 6. Carga de Dados Iniciais (Seed compatível com VeiculoService em memória)
INSERT INTO veiculo (id, placa, modelo, ano, quilometragem, status, ultima_revisao, proxima_revisao, quilometragem_ultima_revisao, observacoes, situacao_manutencao)
VALUES
    (1, 'ABC1D23', 'Ford Transit 350', 2023, 42180, 'Disponível', '2026-06-18', '2026-09-18', 36200, 'Veículo utilizado em rotas urbanas e regionais. Pneus revisados na última manutenção preventiva.', 'Em dia'),
    (2, 'DEF4G56', 'Mercedes-Benz Sprinter', 2022, 67420, 'Em trânsito', '2026-03-12', '2026-09-04', 28700, 'Veículo de longa distância, utilizado principalmente em rodovias.', 'Atenção'),
    (3, 'GHI7J89', 'Volkswagen Delivery', 2021, 112300, 'Em manutenção', '2026-05-20', '2026-11-20', 98500, 'Em revisão geral de freios e suspensão.', 'Em manutenção'),
    (4, 'JKL0M12', 'Iveco Daily', 2024, 15800, 'Disponível', '2026-07-01', '2026-10-01', 12000, 'Veículo novo, adquirido recentemente.', 'Em dia')
ON CONFLICT (id) DO NOTHING;

-- Ajusta a sequência da tabela veiculo após inserção manual com id
SELECT setval(pg_get_serial_sequence('veiculo', 'id'), COALESCE((SELECT MAX(id) FROM veiculo), 1));

INSERT INTO manutencao (id, veiculo_id, data_manutencao, quilometragem, servico_realizado, responsavel, observacao)
VALUES
    (1, 1, '2026-06-18', 36200, 'Revisão preventiva', 'Oficina Central', 'Troca de óleo e filtros'),
    (2, 1, '2026-03-12', 28700, 'Inspeção de freios', 'Equipe interna', 'Sem irregularidades'),
    (3, 2, '2026-03-12', 28700, 'Revisão preventiva', 'Oficina Central', 'Troca de pastilhas')
ON CONFLICT (id) DO NOTHING;

-- Ajusta a sequência da tabela manutencao após inserção manual com id
SELECT setval(pg_get_serial_sequence('manutencao', 'id'), COALESCE((SELECT MAX(id) FROM manutencao), 1));

-- Usuário padrão de acesso inicial (senha de exemplo ou hash bcrypt)
INSERT INTO usuario (id, username, nome, email, senha, ativo)
VALUES (1, 'admin', 'Administrador FleetOps', 'admin@fleetops.com', '$2a$10$wN1rL9rUvWqXv9V4aA5N7.yL6YhYyQ4x9h9U8Ggq.4n3W.Z1/d5gW', TRUE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('usuario', 'id'), COALESCE((SELECT MAX(id) FROM usuario), 1));
