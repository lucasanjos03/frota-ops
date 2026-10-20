-- =============================================================================
-- V2: Limpeza de tabelas ociosas e configuração dos Relacionamentos JPA 
-- 1:N -> public.motorista vinculado a public.veiculo (id_motorista)
-- N:N -> public.tags vinculada a public.veiculo via public.veiculo_tags
-- =============================================================================




-- 2. Cria tabela motorista
CREATE TABLE IF NOT EXISTS public.motorista (
    id bigserial NOT NULL,
    usuario_id int8 NULL,
    nome varchar(150) NOT NULL,
    cpf varchar(14) NOT NULL,
    numero_cnh varchar(20) NOT NULL,
    categoria_cnh varchar(5) NOT NULL,
    validade_cnh date NOT NULL,
    telefone varchar(20) NULL,
    email varchar(150) NULL,
    status varchar(30) DEFAULT 'Ativo'::character varying NOT NULL,
    data_admissao date NULL,
    data_criacao timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT motorista_pkey PRIMARY KEY (id),
    CONSTRAINT motorista_cpf_key UNIQUE (cpf),
    CONSTRAINT motorista_numero_cnh_key UNIQUE (numero_cnh),
    CONSTRAINT motorista_usuario_id_key UNIQUE (usuario_id),
    CONSTRAINT fk_motorista_usuario FOREIGN KEY (usuario_id) REFERENCES public.usuario(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_motorista_status ON public.motorista USING btree (status);
CREATE INDEX IF NOT EXISTS idx_motorista_validade_cnh ON public.motorista USING btree (validade_cnh);

-- 3. Seed de Motoristas na tabela public.motorista
INSERT INTO public.motorista (id, nome, cpf, numero_cnh, categoria_cnh, validade_cnh, telefone, email, status, data_admissao)
VALUES
    (1, 'Carlos Eduardo Silva', '111.222.333-44', '12345678901', 'D', '2028-05-15', '(82) 99911-2233', 'carlos.silva@fleetops.com', 'Ativo', '2023-02-10'),
    (2, 'Fernanda Lima Souza', '222.333.444-55', '23456789012', 'E', '2027-11-20', '(82) 99822-3344', 'fernanda.souza@fleetops.com', 'Ativo', '2022-08-01'),
    (3, 'Roberto Alves Santos', '333.444.555-66', '34567890123', 'C', '2029-01-30', '(82) 99733-4455', 'roberto.santos@fleetops.com', 'Ativo', '2021-04-12'),
    (4, 'Juliana Costa Mendes', '444.555.666-77', '45678901234', 'B', '2028-09-10', '(82) 99644-5566', 'juliana.mendes@fleetops.com', 'Ativo', '2024-01-15')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('public.motorista', 'id'), COALESCE((SELECT MAX(id) FROM public.motorista), 1));

-- 4. Relacionamento 1:N (ManyToOne) entre Veiculo e Motorista
ALTER TABLE public.veiculo ADD COLUMN IF NOT EXISTS id_motorista int8 NULL;

UPDATE public.veiculo SET id_motorista = NULL WHERE id_motorista IS NOT NULL AND id_motorista NOT IN (SELECT id FROM public.motorista);

ALTER TABLE public.veiculo
    ADD CONSTRAINT fk_veiculo_motorista
    FOREIGN KEY (id_motorista)
    REFERENCES public.motorista(id)
    ON DELETE SET NULL;

-- Associando motoristas aos veículos iniciais
UPDATE public.veiculo SET id_motorista = 1 WHERE id = 1;
UPDATE public.veiculo SET id_motorista = 2 WHERE id = 2;
UPDATE public.veiculo SET id_motorista = 3 WHERE id = 3;
UPDATE public.veiculo SET id_motorista = 4 WHERE id = 4;

-- 5. Relacionamento N:N (ManyToMany) entre Veiculo e Tag via tabela associativa veiculo_tags
CREATE TABLE IF NOT EXISTS public.tags (
    id bigserial NOT NULL,
    nome varchar(100) NOT NULL,
    CONSTRAINT tags_pkey PRIMARY KEY (id),
    CONSTRAINT tags_nome_key UNIQUE (nome)
);

CREATE TABLE IF NOT EXISTS public.veiculo_tags (
    id_veiculo int8 NOT NULL,
    id_tag int8 NOT NULL,
    CONSTRAINT veiculo_tags_pkey PRIMARY KEY (id_veiculo, id_tag),
    CONSTRAINT fk_veiculo_tags_veiculo FOREIGN KEY (id_veiculo) REFERENCES public.veiculo(id) ON DELETE CASCADE,
    CONSTRAINT fk_veiculo_tags_tag FOREIGN KEY (id_tag) REFERENCES public.tags(id) ON DELETE CASCADE
);

-- 6. Seed de Tags e vínculos iniciais
INSERT INTO public.tags (id, nome)
VALUES
    (1, 'Rastreado GPS'),
    (2, 'Frota Própria'),
    (3, 'Longa Distância'),
    (4, 'Carga Refrigerada'),
    (5, 'Rota Urbana'),
    (6, 'Prioridade Operacional')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('public.tags', 'id'), COALESCE((SELECT MAX(id) FROM public.tags), 1));

INSERT INTO public.veiculo_tags (id_veiculo, id_tag)
VALUES
    (1, 1),
    (1, 2),
    (1, 5),
    (2, 1),
    (2, 3),
    (3, 2),
    (3, 6),
    (4, 1),
    (4, 2)
ON CONFLICT (id_veiculo, id_tag) DO NOTHING;
