-- =============================================================================
-- V1: Criação das tabelas base do FleetOps (usuario, veiculo, manutencao)
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'situacao_manutencao_enum') THEN
        CREATE TYPE public.situacao_manutencao_enum AS ENUM (
            'Em dia',
            'Atenção',
            'Em manutenção'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'status_veiculo_enum') THEN
        CREATE TYPE public.status_veiculo_enum AS ENUM (
            'Disponível',
            'Em trânsito',
            'Em manutenção',
            'Indisponível'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.usuario (
    id bigserial NOT NULL,
    username varchar(100) NOT NULL,
    nome varchar(150) NOT NULL,
    email varchar(150) NOT NULL,
    senha varchar(255) NOT NULL,
    ativo bool DEFAULT true NOT NULL,
    data_criacao timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT usuario_pkey PRIMARY KEY (id),
    CONSTRAINT usuario_username_key UNIQUE (username),
    CONSTRAINT usuario_email_key UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS public.veiculo (
    id bigserial NOT NULL,
    placa varchar(10) NOT NULL,
    modelo varchar(120) NOT NULL,
    ano int4 NOT NULL,
    quilometragem int8 DEFAULT 0 NOT NULL,
    status varchar(30) DEFAULT 'Disponível'::character varying NOT NULL,
    situacao_manutencao varchar(30) DEFAULT 'Em dia'::character varying NULL,
    ultima_revisao date NULL,
    proxima_revisao date NULL,
    quilometragem_ultima_revisao int8 NULL,
    observacoes text NULL,
    data_criacao timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
    data_atualizacao timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT veiculo_pkey PRIMARY KEY (id),
    CONSTRAINT veiculo_placa_key UNIQUE (placa),
    CONSTRAINT chk_veiculo_ano CHECK ((ano >= 1900)),
    CONSTRAINT chk_veiculo_km CHECK ((quilometragem >= 0)),
    CONSTRAINT chk_veiculo_km_revisao CHECK (((quilometragem_ultima_revisao IS NULL) OR (quilometragem_ultima_revisao >= 0)))
);

CREATE INDEX IF NOT EXISTS idx_veiculo_placa ON public.veiculo USING btree (placa);
CREATE INDEX IF NOT EXISTS idx_veiculo_proxima_revisao ON public.veiculo USING btree (proxima_revisao);
CREATE INDEX IF NOT EXISTS idx_veiculo_status ON public.veiculo USING btree (status);

CREATE TABLE IF NOT EXISTS public.manutencao (
    id bigserial NOT NULL,
    veiculo_id int8 NOT NULL,
    data_manutencao date NOT NULL,
    quilometragem int8 NOT NULL,
    servico_realizado varchar(255) NOT NULL,
    responsavel varchar(150) NOT NULL,
    observacao text NULL,
    data_registro timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT manutencao_pkey PRIMARY KEY (id),
    CONSTRAINT chk_manutencao_km CHECK ((quilometragem >= 0)),
    CONSTRAINT fk_manutencao_veiculo FOREIGN KEY (veiculo_id) REFERENCES public.veiculo(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_manutencao_data ON public.manutencao USING btree (data_manutencao DESC);
CREATE INDEX IF NOT EXISTS idx_manutencao_veiculo_id ON public.manutencao USING btree (veiculo_id);

-- Carga inicial de Veículos
INSERT INTO public.veiculo (id, placa, modelo, ano, quilometragem, status, ultima_revisao, proxima_revisao, quilometragem_ultima_revisao, observacoes, situacao_manutencao)
VALUES
    (1, 'ABC1D23', 'Ford Transit 350', 2023, 42180, 'Disponível', '2026-06-18', '2026-09-18', 36200, 'Veículo utilizado em rotas urbanas e regionais. Pneus revisados na última manutenção preventiva.', 'Em dia'),
    (2, 'DEF4G56', 'Mercedes-Benz Sprinter', 2022, 67420, 'Em trânsito', '2026-03-12', '2026-09-04', 28700, 'Veículo de longa distância, utilizado principalmente em rodovias.', 'Atenção'),
    (3, 'GHI7J89', 'Volkswagen Delivery', 2021, 112300, 'Em manutenção', '2026-05-20', '2026-11-20', 98500, 'Em revisão geral de freios e suspensão.', 'Em manutenção'),
    (4, 'JKL0M12', 'Iveco Daily', 2024, 15800, 'Disponível', '2026-07-01', '2026-10-01', 12000, 'Veículo novo, adquirido recentemente.', 'Em dia')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('public.veiculo', 'id'), COALESCE((SELECT MAX(id) FROM public.veiculo), 1));

-- Carga inicial de Manutenções
INSERT INTO public.manutencao (id, veiculo_id, data_manutencao, quilometragem, servico_realizado, responsavel, observacao)
VALUES
    (1, 1, '2026-06-18', 36200, 'Revisão preventiva', 'Oficina Central', 'Troca de óleo e filtros'),
    (2, 1, '2026-03-12', 28700, 'Inspeção de freios', 'Equipe interna', 'Sem irregularidades'),
    (3, 2, '2026-03-12', 28700, 'Revisão preventiva', 'Oficina Central', 'Troca de pastilhas')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('public.manutencao', 'id'), COALESCE((SELECT MAX(id) FROM public.manutencao), 1));

-- Usuário padrão
INSERT INTO public.usuario (id, username, nome, email, senha, ativo)
VALUES (1, 'admin', 'Administrador FleetOps', 'admin@fleetops.com', '$2a$10$wN1rL9rUvWqXv9V4aA5N7.yL6YhYyQ4x9h9U8Ggq.4n3W.Z1/d5gW', TRUE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('public.usuario', 'id'), COALESCE((SELECT MAX(id) FROM public.usuario), 1));
