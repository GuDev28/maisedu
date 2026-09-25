-- V4: suporte ao login (autenticação, bloqueio por tentativas, senha provisória)

ALTER TABLE usuario ADD COLUMN tentativas_falhas INTEGER NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN bloqueado_ate TIMESTAMP;

ALTER TABLE usuario ADD COLUMN senha_temporaria BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_usuario_bloqueado_ate ON usuario(bloqueado_ate) WHERE bloqueado_ate IS NOT NULL;