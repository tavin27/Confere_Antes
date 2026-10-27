-- =====================================================================
-- ÍNDICES DE BUSCA (PostgreSQL)
-- Rodado depois de schema.sql; apply_schema.py aplica os dois arquivos.
-- Para Flyway, converta em uma migration versionada, por exemplo V2__search_indexes.sql.
-- Não repete nenhum índice que já existe no schema.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Extensões (exigem permissão de criar extensão; em banco gerenciado,
-- habilite como admin)
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- busca parcial (LIKE '%x%') e similaridade
CREATE EXTENSION IF NOT EXISTS unaccent;   -- ignorar acentos

-- unaccent não é IMMUTABLE; este wrapper permite usá-lo em índices
CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT AS
$$ SELECT public.unaccent('public.unaccent', $1) $$;


-- ---------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------
-- Igualdade case-insensitive (login, "esqueci a senha").
-- Também impede 'Joao@x.com' e 'joao@x.com' como contas diferentes.
-- Se falhar, já existem e-mails/usernames duplicados quando comparados em minúsculas.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower
    ON users (lower(email));
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower
    ON users (lower(username));

-- Busca parcial / autocomplete: ILIKE '%silva%'
CREATE INDEX IF NOT EXISTS idx_users_username_trgm
    ON users USING GIN (username gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_email_trgm
    ON users USING GIN (email gin_trgm_ops);

-- Telefone normalizado (só dígitos): acha "(11) 98765-4321" digitando "11987654321"
CREATE INDEX IF NOT EXISTS idx_users_phone_digits
    ON users (regexp_replace(phone, '\D', '', 'g'));
CREATE INDEX IF NOT EXISTS idx_users_phone_trgm
    ON users USING GIN ((regexp_replace(phone, '\D', '', 'g')) gin_trgm_ops);

-- Listagem do painel: só usuários ativos, mais recentes primeiro
CREATE INDEX IF NOT EXISTS idx_users_active_created
    ON users (created_at DESC) WHERE active;


-- ---------------------------------------------------------------------
-- ALERTS
-- ---------------------------------------------------------------------
-- Mesmo número/contato de origem atacando várias vítimas
CREATE INDEX IF NOT EXISTS idx_alerts_origin_number
    ON alerts (origin_number);

-- Busca textual em português, sem acento
CREATE INDEX IF NOT EXISTS idx_alerts_desc_fts
    ON alerts USING GIN (to_tsvector('portuguese', f_unaccent(description_alert)));


-- ---------------------------------------------------------------------
-- TRANSACTIONS
-- ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_transactions_client_fts
    ON transactions USING GIN (to_tsvector('portuguese', f_unaccent(coalesce(description_client, ''))));