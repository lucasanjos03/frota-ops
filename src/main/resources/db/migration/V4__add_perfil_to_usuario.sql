-- =============================================================================
-- V4: Adição de perfil de acesso (ADMIN, OPERADOR, MOTORISTA) em public.usuario
-- =============================================================================

ALTER TABLE public.usuario
    ADD COLUMN IF NOT EXISTS perfil varchar(30) DEFAULT 'OPERADOR' NOT NULL;

UPDATE public.usuario
SET perfil = 'ADMIN'
WHERE username = 'admin';

UPDATE public.usuario
SET perfil = 'MOTORISTA'
WHERE id IN (SELECT usuario_id FROM public.motorista WHERE usuario_id IS NOT NULL);

UPDATE public.usuario
SET perfil = 'OPERADOR'
WHERE username = 'operador';

