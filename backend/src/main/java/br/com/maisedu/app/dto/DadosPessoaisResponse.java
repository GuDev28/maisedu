package br.com.maisedu.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Exportação dos dados pessoais de um usuário (LGPD, art. 18, II e V), em JSON. */
public record DadosPessoaisResponse(
        LocalDateTime geradoEm,
        Cadastro cadastro,
        List<String> turmas,
        List<QuestaoAutoria> questoesDeAutoria,
        List<RespostaExportada> respostas,
        List<DesempenhoExportado> desempenho,
        List<RegistroAuditoria> registrosDeAuditoria) {

    public record Cadastro(Long id, String nome, String email, String login, List<String> papeis,
                           String instituicao, boolean ativo, LocalDateTime criadoEm,
                           LocalDateTime termosAceitosEm, Integer versaoTermosAceita) {
    }

    public record QuestaoAutoria(Long id, String enunciado, String topico, String status, LocalDateTime criadoEm) {
    }

    public record RespostaExportada(LocalDateTime respondidaEm, String atividade, Long questaoId,
                                    String alternativaEscolhida, boolean acertou) {
    }

    public record DesempenhoExportado(String topico, String nivel, int totalRespostas, int totalAcertos,
                                      BigDecimal percentualAcerto) {
    }

    public record RegistroAuditoria(LocalDateTime data, String acao, boolean sucesso, String ip, String navegador,
                                    String detalhes) {
    }
}
