-- E-mail do responsável legal pelo aluno. Recebe o login e a senha provisória do aluno (no cadastro e
-- quando a escola gera uma senha nova); o aluno continua sem e-mail próprio. Não é único: irmãos
-- podem ter o mesmo responsável. Contas antigas ficam sem e-mail até a escola informar.
ALTER TABLE usuario ADD COLUMN email_responsavel VARCHAR(255);

-- Marcação provisória: a alternativa que o aluno escolheu mas ainda não confirmou. Pode ser trocada à
-- vontade e não conta como resposta (RN04 continua valendo só para a tabela resposta); ao confirmar,
-- vira resposta e a marcação é limpa.
ALTER TABLE atividade_aluno_questao ADD COLUMN alternativa_marcada_id BIGINT REFERENCES alternativa (id);
ALTER TABLE atividade_aluno_questao ADD COLUMN marcada_em TIMESTAMP;

-- Como na RN04: a alternativa marcada precisa ser da questão entregue (conferido também no banco).
CREATE FUNCTION fn_marcacao_alternativa_da_questao() RETURNS trigger AS $$
BEGIN
    IF NEW.alternativa_marcada_id IS NOT NULL AND NOT EXISTS (
        SELECT 1
          FROM alternativa alt
         WHERE alt.id = NEW.alternativa_marcada_id
           AND alt.questao_id = NEW.questao_id
    ) THEN
        RAISE EXCEPTION 'A alternativa % não pertence à questão entregue %',
            NEW.alternativa_marcada_id, NEW.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_marcacao_alternativa_da_questao
    BEFORE INSERT OR UPDATE OF alternativa_marcada_id ON atividade_aluno_questao
    FOR EACH ROW EXECUTE FUNCTION fn_marcacao_alternativa_da_questao();
