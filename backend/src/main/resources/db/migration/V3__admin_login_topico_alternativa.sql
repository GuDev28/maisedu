
-- adiciona papel de adminstrador
INSERT INTO role (nome) VALUES ('ADMIN');


-- e-mail só para professor/admin; aluno entra com login gerado
ALTER TABLE usuario ALTER COLUMN email DROP NOT NULL;
ALTER TABLE usuario ADD COLUMN login VARCHAR(50) UNIQUE;

-- Todo usuário precisa de pelo menos um identificador para autenticar
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_identificador
    CHECK (email IS NOT NULL OR login IS NOT NULL);

-- tópicos (lista fixa, sem CRUD)
CREATE TABLE topico (
    id    BIGSERIAL PRIMARY KEY,
    nome  VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO topico (nome) VALUES
    ('Números naturais e operações'),
    ('Números inteiros'),
    ('Frações'),
    ('Números decimais'),
    ('Potenciação e radiciação'),
    ('Porcentagem'),
    ('Razão e proporção'),
    ('Expressões algébricas'),
    ('Equações do 1º grau'),
    ('Sistemas de equações do 1º grau'),
    ('Equações do 2º grau'),
    ('Ângulos'),
    ('Triângulos e quadriláteros'),
    ('Circunferência e círculo'),
    ('Teorema de Pitágoras'),
    ('Perímetro e área'),
    ('Volume e capacidade'),
    ('Probabilidade'),
    ('Gráficos e tabelas'),
    ('Média, moda e mediana');


-- adiciona em questão o tópico e nível de dificuldade
ALTER TABLE questao ADD COLUMN topico_id BIGINT REFERENCES topico(id);
ALTER TABLE questao ADD COLUMN nivel_dificuldade VARCHAR(10);  -- FACIL, MEDIO, DIFICIL

ALTER TABLE questao ALTER COLUMN topico_id SET NOT NULL;
ALTER TABLE questao ALTER COLUMN nivel_dificuldade SET NOT NULL;
ALTER TABLE questao ADD CONSTRAINT ck_questao_nivel
    CHECK (nivel_dificuldade IN ('FACIL', 'MEDIO', 'DIFICIL'));

CREATE INDEX idx_questao_topico_nivel ON questao(topico_id, nivel_dificuldade);

-- exatamente 5 alternativas por questão e exatamente uma correta

CREATE TABLE alternativa (
    id           BIGSERIAL PRIMARY KEY,
    questao_id   BIGINT   NOT NULL REFERENCES questao(id) ON DELETE CASCADE,
    texto        TEXT     NOT NULL,
    correta      BOOLEAN  NOT NULL DEFAULT FALSE,
    ordem        SMALLINT NOT NULL,  -- 1 = A, 2 = B, 3 = C, 4 = D, 5 = E
    CONSTRAINT ck_alternativa_ordem CHECK (ordem BETWEEN 1 AND 5),


    -- uma alternativa por posição (A, B, C, D, E) em cada questão
    CONSTRAINT uk_alternativa_ordem UNIQUE (questao_id, ordem)
        DEFERRABLE INITIALLY DEFERRED,

    -- no máximo uma alternativa correta por questão
    CONSTRAINT ex_alternativa_uma_correta EXCLUDE (questao_id WITH =) WHERE (correta)
        DEFERRABLE INITIALLY DEFERRED
);
