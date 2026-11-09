-- RN02: quem rejeita uma questão explica o motivo, para o autor saber o que corrigir.
-- Opcional na aprovação; a obrigatoriedade na rejeição é conferida no serviço (AvaliacaoQuestaoService).
ALTER TABLE avaliacao_questao ADD COLUMN comentario VARCHAR(500);
