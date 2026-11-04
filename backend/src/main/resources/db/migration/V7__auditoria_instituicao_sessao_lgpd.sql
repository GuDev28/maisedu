-- V7: isolamento da auditoria por instituição, revogação de sessões, versão dos termos e anonimização (LGPD).

-- 1) Auditoria por instituição: cada admin só consulta os registros da própria instituição.
ALTER TABLE log_auditoria ADD COLUMN instituicao_id BIGINT;

UPDATE log_auditoria l
   SET instituicao_id = u.instituicao_id
  FROM usuario u
 WHERE l.usuario_id = u.id;

CREATE INDEX idx_log_auditoria_instituicao_criado_em ON log_auditoria(instituicao_id, criado_em DESC);

-- 2) Identificadores normalizados: e-mail e login sempre em minúsculas e sem espaços nas pontas,
--    para que "Prof@Escola" e "prof@escola" não virem duas contas. Login de aluno não pode ter "@",
--    o que impede que ele coincida com o e-mail de outro usuário no campo único de login.
UPDATE usuario SET email = lower(trim(email)) WHERE email IS NOT NULL;
UPDATE usuario SET login = lower(trim(login)) WHERE login IS NOT NULL;

ALTER TABLE usuario ADD CONSTRAINT ck_usuario_email_normalizado
    CHECK (email IS NULL OR email = lower(trim(email)));
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_login_normalizado
    CHECK (login IS NULL OR (login = lower(trim(login)) AND position('@' in login) = 0));

-- 3) Revogação de sessões: o JWT carrega a versão em que foi emitido; logout, troca/redefinição de
--    senha e desativação incrementam a versão e invalidam os tokens antigos.
ALTER TABLE usuario ADD COLUMN versao_sessao INTEGER NOT NULL DEFAULT 0;

-- 4) Versão dos Termos de Uso / Política de Privacidade aceita. Quem já aceitou aceitou a versão 1.
ALTER TABLE usuario ADD COLUMN termos_versao INTEGER;
UPDATE usuario SET termos_versao = 1 WHERE termos_aceitos_em IS NOT NULL;

-- 5) Anonimização (eliminação a pedido do titular).
ALTER TABLE usuario ADD COLUMN anonimizado_em TIMESTAMP;
