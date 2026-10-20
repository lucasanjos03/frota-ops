-- =============================================================================
-- V5: Adição de data_modificacao em public.usuario para suporte a Soft Delete
-- =============================================================================

ALTER TABLE public.usuario
    ADD COLUMN IF NOT EXISTS data_modificacao timestamptz DEFAULT CURRENT_TIMESTAMP NULL;

UPDATE public.usuario
SET data_modificacao = COALESCE(data_criacao, CURRENT_TIMESTAMP)
WHERE data_modificacao IS NULL;

