-- =============================================================================
-- V3: Vínculo obrigatório entre Motorista e Usuário (public.usuario)
-- =============================================================================

-- 1. Ajusta senha padrão do administrador para login direto (admin / admin123)
UPDATE public.usuario
SET senha = 'admin123'
WHERE username = 'admin';

-- 2. Cria usuários para os motoristas existentes e um usuário administrativo/comum adicional
INSERT INTO public.usuario (id, username, nome, email, senha, ativo)
VALUES
    (2, 'carlos.silva', 'Carlos Eduardo Silva', 'carlos.silva@fleetops.com', '123456', TRUE),
    (3, 'fernanda.souza', 'Fernanda Lima Souza', 'fernanda.souza@fleetops.com', '123456', TRUE),
    (4, 'roberto.santos', 'Roberto Alves Santos', 'roberto.santos@fleetops.com', '123456', TRUE),
    (5, 'juliana.mendes', 'Juliana Costa Mendes', 'juliana.mendes@fleetops.com', '123456', TRUE),
    (6, 'operador', 'Marcos Oliveira (Operador)', 'operador@fleetops.com', '123456', TRUE)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('public.usuario', 'id'), COALESCE((SELECT MAX(id) FROM public.usuario), 1));

-- 3. Vincula cada motorista existente ao seu respectivo usuário do sistema
UPDATE public.motorista SET usuario_id = 2 WHERE id = 1 AND usuario_id IS NULL;
UPDATE public.motorista SET usuario_id = 3 WHERE id = 2 AND usuario_id IS NULL;
UPDATE public.motorista SET usuario_id = 4 WHERE id = 3 AND usuario_id IS NULL;
UPDATE public.motorista SET usuario_id = 5 WHERE id = 4 AND usuario_id IS NULL;

-- Garante que qualquer eventual motorista sem usuário receba vínculo antes do NOT NULL
UPDATE public.motorista SET usuario_id = 1 WHERE usuario_id IS NULL;

-- 4. Torna usuario_id obrigatório em public.motorista
ALTER TABLE public.motorista
    ALTER COLUMN usuario_id SET NOT NULL;

