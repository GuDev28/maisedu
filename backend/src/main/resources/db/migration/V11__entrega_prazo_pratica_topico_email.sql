-- V11: entrega única da atividade com prazo, desempenho contabilizado no fechamento, prática
-- adaptativa por tópico e situação do último e-mail de acesso enviado.

-- 1) Prazo da atividade regular. Atividades que já existiam ganham 7 dias a partir da criação.
--    fechada_em: momento em que a rotina de fechamento contabilizou o desempenho (RN05) das
--    respostas enviadas. Até o prazo, o aluno não vê a correção nem o efeito no desempenho.
ALTER TABLE atividade ADD COLUMN prazo TIMESTAMP;
UPDATE atividade SET prazo = criado_em + INTERVAL '7 days';
ALTER TABLE atividade ALTER COLUMN prazo SET NOT NULL;
ALTER TABLE atividade ADD COLUMN fechada_em TIMESTAMP;
ALTER TABLE atividade ADD CONSTRAINT ck_atividade_prazo CHECK (prazo > criado_em);

-- A rotina de fechamento procura atividades vencidas e ainda não fechadas.
CREATE INDEX idx_atividade_prazo_aberta ON atividade(prazo) WHERE fechada_em IS NULL;

-- 2) Resposta contabilizada no desempenho (RN05). Cada resposta entra uma única vez:
--    prática adaptativa no envio; atividade regular no fechamento do prazo.
--    As respostas que já existiam foram contabilizadas na hora em que foram dadas (fluxo antigo).
ALTER TABLE resposta ADD COLUMN contabilizada_em TIMESTAMP;
UPDATE resposta SET contabilizada_em = respondida_em;

-- 3) Prática adaptativa por tópico: o aluno escolhe o tópico (nula na prática recomendada pelo motor).
ALTER TABLE atividade_aluno ADD COLUMN topico_id BIGINT REFERENCES topico(id);
ALTER TABLE atividade_aluno ADD CONSTRAINT ck_atividade_aluno_topico
    CHECK (topico_id IS NULL OR tipo = 'ADAPTATIVA');

-- Práticas abertas do aluno (uma por tópico e uma recomendada).
CREATE INDEX idx_atividade_aluno_pratica_aberta ON atividade_aluno(aluno_id, topico_id)
    WHERE tipo = 'ADAPTATIVA' AND concluida_em IS NULL;

-- 4) Situação do último e-mail com dados de acesso (senha provisória) enviado ao usuário ou ao
--    responsável do aluno: o professor e o admin veem quando o envio falhou. Não guarda o endereço.
ALTER TABLE usuario ADD COLUMN email_acesso_status VARCHAR(20);
ALTER TABLE usuario ADD COLUMN email_acesso_em TIMESTAMP;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_email_acesso_status
    CHECK (email_acesso_status IS NULL OR email_acesso_status IN ('ENVIADO', 'FALHOU', 'NAO_CONFIGURADO'));
