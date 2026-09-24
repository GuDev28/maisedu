-- Migration referente à RN2 (validação colaborativa de questões)
-- Baseada nas entidades JPA.

CREATE TABLE questao (
    id              BIGSERIAL PRIMARY KEY,
    enunciado       TEXT NOT NULL,
    autor_id        BIGINT NOT NULL REFERENCES usuario(id),
    status          VARCHAR(255) NOT NULL,  -- PENDENTE, APROVADA, REJEITADA
    criado_em       TIMESTAMP NOT NULL
);

CREATE TABLE avaliacao_questao (
    id              BIGSERIAL PRIMARY KEY,
    questao_id      BIGINT NOT NULL REFERENCES questao(id),
    professor_id    BIGINT NOT NULL REFERENCES usuario(id),
    voto            VARCHAR(255) NOT NULL,  -- APROVAR, REJEITAR
    avaliado_em     TIMESTAMP NOT NULL,
    UNIQUE (questao_id, professor_id)
);

-- Índices de apoio às buscas de questões pendentes de validação
CREATE INDEX idx_questao_status ON questao(status);
CREATE INDEX idx_questao_autor ON questao(autor_id);
