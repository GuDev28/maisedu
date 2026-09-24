package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.AvaliacaoQuestaoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.*;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AvaliacaoQuestaoService {

    public static final int VOTOS_PARA_DECIDIR = 3;
    public static final int MAXIMO_VOTOS = 5;

    private final QuestaoRepository questaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;

    @Transactional
    public AvaliacaoQuestaoResponse avaliar(Long questaoId, Long professorId, VotoAvaliacao voto) {
        if (voto == null) {
            throw new NegocioException("Informe o voto (APROVAR ou REJEITAR)");
        }

        // trava a questão até o fim da transação: votos simultâneos são apurados um de cada vez
        Questao questao = questaoRepository.findByIdParaVotacao(questaoId)
                .orElseThrow(() -> new NegocioException("Questão não encontrada"));

        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new NegocioException("Professor não encontrado"));

        if (!professor.possuiRole(RoleNome.PROFESSOR)) {
            throw new NegocioException("Usuário informado não possui papel de professor");
        }

        if (!professor.isAtivo()) {
            throw new NegocioException("Professor inativo não pode avaliar questões");
        }

        if (questao.isAutor(professorId)) {
            throw new NegocioException("O autor da questão não pode avaliar a própria questão");
        }

        if (questao.getStatus() == StatusQuestao.RASCUNHO) {
            throw new NegocioException("Questão ainda é um rascunho: o autor precisa enviá-la para validação antes");
        }
        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new NegocioException("A validação desta questão já foi encerrada");
        }

        if (avaliacaoQuestaoRepository.existsByQuestaoIdAndProfessorId(questaoId, professorId)) {
            throw new NegocioException("Professor já avaliou esta questão");
        }

        if (avaliacaoQuestaoRepository.countByQuestaoId(questaoId) >= MAXIMO_VOTOS) {
            // não deve acontecer (o quórum fecha antes), mas protege contra dados inconsistentes
            throw new NegocioException("A questão já recebeu o máximo de " + MAXIMO_VOTOS + " votos");
        }

        avaliacaoQuestaoRepository.save(new AvaliacaoQuestao(questao, professor, voto));

        long aprovacoes = avaliacaoQuestaoRepository.countByQuestaoIdAndVoto(questaoId, VotoAvaliacao.APROVAR);
        long rejeicoes = avaliacaoQuestaoRepository.countByQuestaoIdAndVoto(questaoId, VotoAvaliacao.REJEITAR);

        if (aprovacoes >= VOTOS_PARA_DECIDIR) {
            questao.setStatus(StatusQuestao.APROVADA);
        } else if (rejeicoes >= VOTOS_PARA_DECIDIR) {
            questao.setStatus(StatusQuestao.REJEITADA);
        }

        return new AvaliacaoQuestaoResponse(
                questaoId, questao.getStatus(), aprovacoes, rejeicoes, VOTOS_PARA_DECIDIR, MAXIMO_VOTOS);
    }
}
