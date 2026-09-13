-- ============================================================
-- V1__schema_inicial.sql
-- Escopo desta migration: RN1 (vínculo e elegibilidade aluno-turma-professor)
-- Gerado a partir das entidades JPA corrigidas.
-- RBAC completo (permissao/role_permissao) e demais RNs entram em migrations futuras.
-- ============================================================

CREATE TABLE instituicao (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    cnpj            VARCHAR(255) NOT NULL UNIQUE,
    endereco        VARCHAR(255) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL
);

CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    senha           VARCHAR(255) NOT NULL,
    instituicao_id  BIGINT NOT NULL REFERENCES instituicao(id),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL
);

CREATE TABLE role (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL UNIQUE  -- ALUNO, PROFESSOR
);

INSERT INTO role (nome) VALUES ('ALUNO'), ('PROFESSOR');

CREATE TABLE usuario_role (
    usuario_id      BIGINT NOT NULL REFERENCES usuario(id),
    role_id         BIGINT NOT NULL REFERENCES role(id),
    PRIMARY KEY (usuario_id, role_id)
);

CREATE TABLE turma (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    instituicao_id  BIGINT NOT NULL REFERENCES instituicao(id),
    ano_escolar     VARCHAR(255) NOT NULL,  -- SEXTO_ANO, SETIMO_ANO, OITAVO_ANO, NONO_ANO
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL
);

CREATE TABLE aluno_turma (
    id              BIGSERIAL PRIMARY KEY,
    aluno_id        BIGINT NOT NULL REFERENCES usuario(id),
    turma_id        BIGINT NOT NULL REFERENCES turma(id),
    UNIQUE (aluno_id, turma_id)
);

CREATE TABLE professor_turma (
    id              BIGSERIAL PRIMARY KEY,
    professor_id    BIGINT NOT NULL REFERENCES usuario(id),
    turma_id        BIGINT NOT NULL REFERENCES turma(id),
    UNIQUE (professor_id, turma_id)
);

-- Índices de apoio às checagens de elegibilidade feitas na camada de service
-- (role do usuário, instituição da turma vs. instituição do usuário)
CREATE INDEX idx_usuario_instituicao ON usuario(instituicao_id);
CREATE INDEX idx_turma_instituicao ON turma(instituicao_id);
