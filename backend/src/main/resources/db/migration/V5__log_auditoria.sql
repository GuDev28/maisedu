-- V5: auditoria — log de acessos (login/logout/troca de senha) e das ações de negócio: cadastro, vínculo aluno/professor-turma e avaliação de questão.

CREATE TABLE log_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    usuario_nome VARCHAR(255),
    identificador_acesso VARCHAR(255),
    acao VARCHAR(40) NOT NULL,
    entidade VARCHAR(60),
    entidade_id BIGINT,
    sucesso BOOLEAN NOT NULL,
    detalhes VARCHAR(500),
    ip VARCHAR(45),
    user_agent VARCHAR(500),
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);


CREATE INDEX idx_log_auditoria_usuario_id ON log_auditoria(usuario_id);
CREATE INDEX idx_log_auditoria_acao ON log_auditoria(acao);
CREATE INDEX idx_log_auditoria_criado_em ON log_auditoria(criado_em);
