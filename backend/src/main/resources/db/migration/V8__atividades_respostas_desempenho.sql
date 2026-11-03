-- V8: ano escolar da questão, atividades (RF15), snapshot das questões entregues (RN03),
-- respostas (RN04), desempenho por tópico e nível (RN05) e prática adaptativa (RN06).

-- Ano escolar da questão (6º ao 9º ano). Nulo apenas nas questões criadas antes desta migration.
ALTER TABLE questao ADD COLUMN ano_escolar VARCHAR(20);
ALTER TABLE questao ADD CONSTRAINT ck_questao_ano_escolar
    CHECK (ano_escolar IS NULL OR ano_escolar IN ('SEXTO_ANO', 'SETIMO_ANO', 'OITAVO_ANO', 'NONO_ANO'));
ALTER TABLE questao ADD CONSTRAINT ck_questao_status
    CHECK (status IN ('RASCUNHO', 'PENDENTE', 'APROVADA', 'REJEITADA'));

-- Busca do banco compartilhado e do motor adaptativo: questões aprovadas por tópico e nível.
CREATE INDEX idx_questao_aprovada_topico_nivel ON questao(topico_id, nivel_dificuldade) WHERE status = 'APROVADA';

ALTER TABLE turma ADD CONSTRAINT ck_turma_ano_escolar
    CHECK (ano_escolar IN ('SEXTO_ANO', 'SETIMO_ANO', 'OITAVO_ANO', 'NONO_ANO'));

-- Atividade regular, criada pelo professor para uma turma em que leciona.
CREATE TABLE atividade (
    id              BIGSERIAL PRIMARY KEY,
    titulo          VARCHAR(150) NOT NULL,
    turma_id        BIGINT NOT NULL REFERENCES turma(id),
    professor_id    BIGINT NOT NULL REFERENCES usuario(id),
    criado_em       TIMESTAMP NOT NULL
);

CREATE INDEX idx_atividade_turma ON atividade(turma_id);

CREATE TABLE atividade_questao (
    id              BIGSERIAL PRIMARY KEY,
    atividade_id    BIGINT NOT NULL REFERENCES atividade(id) ON DELETE CASCADE,
    questao_id      BIGINT NOT NULL REFERENCES questao(id),
    ordem           SMALLINT NOT NULL CHECK (ordem >= 1),
    CONSTRAINT uk_atividade_questao UNIQUE (atividade_id, questao_id),
    CONSTRAINT uk_atividade_questao_ordem UNIQUE (atividade_id, ordem) DEFERRABLE INITIALLY DEFERRED
);

-- Atividade individual do aluno: regular (com atividade de origem) ou adaptativa (sem origem).
-- UNIQUE (aluno_id, atividade_id): cada aluno inicia uma atividade regular uma única vez; como NULLs
-- são distintos, o aluno pode ter várias práticas adaptativas.
CREATE TABLE atividade_aluno (
    id              BIGSERIAL PRIMARY KEY,
    aluno_id        BIGINT NOT NULL REFERENCES usuario(id),
    atividade_id    BIGINT REFERENCES atividade(id),
    tipo            VARCHAR(20) NOT NULL,
    iniciada_em     TIMESTAMP NOT NULL,
    concluida_em    TIMESTAMP,
    CONSTRAINT uk_atividade_aluno UNIQUE (aluno_id, atividade_id),
    CONSTRAINT ck_atividade_aluno_tipo CHECK (
        (tipo = 'REGULAR' AND atividade_id IS NOT NULL) OR (tipo = 'ADAPTATIVA' AND atividade_id IS NULL)),
    CONSTRAINT ck_atividade_aluno_conclusao CHECK (concluida_em IS NULL OR concluida_em >= iniciada_em)
);

CREATE INDEX idx_atividade_aluno_aluno ON atividade_aluno(aluno_id, tipo);

-- Snapshot imutável das questões entregues ao aluno (RN03).
CREATE TABLE atividade_aluno_questao (
    id                  BIGSERIAL PRIMARY KEY,
    atividade_aluno_id  BIGINT NOT NULL REFERENCES atividade_aluno(id),
    questao_id          BIGINT NOT NULL REFERENCES questao(id),
    ordem               SMALLINT NOT NULL CHECK (ordem >= 1),
    CONSTRAINT uk_atividade_aluno_questao UNIQUE (atividade_aluno_id, questao_id),
    CONSTRAINT uk_atividade_aluno_questao_ordem UNIQUE (atividade_aluno_id, ordem)
);

-- Resposta: uma única por questão entregue (RN04). Aluno, questão e acerto são derivados.
CREATE TABLE resposta (
    id                          BIGSERIAL PRIMARY KEY,
    atividade_aluno_questao_id  BIGINT NOT NULL UNIQUE REFERENCES atividade_aluno_questao(id),
    alternativa_id              BIGINT NOT NULL REFERENCES alternativa(id),
    respondida_em               TIMESTAMP NOT NULL
);

-- RN04 também no banco: a alternativa escolhida precisa pertencer à questão entregue.
CREATE FUNCTION fn_resposta_alternativa_da_questao() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM atividade_aluno_questao aaq
          JOIN alternativa alt ON alt.questao_id = aaq.questao_id
         WHERE aaq.id = NEW.atividade_aluno_questao_id
           AND alt.id = NEW.alternativa_id
    ) THEN
        RAISE EXCEPTION 'A alternativa % não pertence à questão entregue %',
            NEW.alternativa_id, NEW.atividade_aluno_questao_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_resposta_alternativa_da_questao
    BEFORE INSERT OR UPDATE ON resposta
    FOR EACH ROW EXECUTE FUNCTION fn_resposta_alternativa_da_questao();

-- Desempenho acumulado por aluno, tópico e nível (RN05).
CREATE TABLE desempenho_topico (
    id                  BIGSERIAL PRIMARY KEY,
    aluno_id            BIGINT NOT NULL REFERENCES usuario(id),
    topico_id           BIGINT NOT NULL REFERENCES topico(id),
    nivel_dificuldade   VARCHAR(10) NOT NULL,
    total_respostas     INTEGER NOT NULL DEFAULT 0,
    total_acertos       INTEGER NOT NULL DEFAULT 0,
    percentual_acerto   NUMERIC(5, 2) NOT NULL DEFAULT 0,
    atualizado_em       TIMESTAMP NOT NULL,
    CONSTRAINT uk_desempenho_topico UNIQUE (aluno_id, topico_id, nivel_dificuldade),
    CONSTRAINT ck_desempenho_nivel CHECK (nivel_dificuldade IN ('FACIL', 'MEDIO', 'DIFICIL')),
    CONSTRAINT ck_desempenho_contadores CHECK (total_respostas >= 0 AND total_acertos BETWEEN 0 AND total_respostas),
    CONSTRAINT ck_desempenho_percentual CHECK (percentual_acerto BETWEEN 0 AND 100)
);
