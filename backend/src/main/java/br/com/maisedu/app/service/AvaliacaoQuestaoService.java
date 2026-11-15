package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.AvaliacaoQuestaoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.*;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RN02: validação colaborativa. Cada professor vota uma vez; 3 votos iguais decidem, no máximo 5 votos. */
@Service
@RequiredArgsConstructor
public class AvaliacaoQuestaoService {

    public static final int VOTOS_PARA_DECIDIR = Questao.VOTOS_PARA_DECIDIR;
    public static final int MAXIMO_VOTOS = Questao.MAXIMO_VOTOS;

    private final QuestaoRepository questaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public AvaliacaoQuestaoResponse avaliar(UsuarioAutenticado ator, Long questaoId, VotoAvaliacao voto,
                                            String comentarioInformado) {
        if (voto == null) {
            throw new NegocioException("Informe o voto (APROVAR ou REJEITAR)");
        }
        String comentario = comentarioInformado == null || comentarioInformado.isBlank()
                ? null
                : comentarioInformado.strip();
        if (voto == VotoAvaliacao.REJEITAR && comentario == null) {
            throw new NegocioException("Explique o motivo da rejeição: é o que o autor lê para corrigir a questão");
        }

        // trava a questão até o fim da transação: votos simultâneos são apurados um de cada vez
        Questao questao = questaoRepository.findByIdParaVotacao(questaoId)
                .orElseThrow(() -> new NegocioException("Questão não encontrada"));

        Usuario professor = usuarioRepository.findById(ator.id())
                .orElseThrow(() -> new NegocioException("Professor não encontrado"));

        if (!professor.possuiRole(RoleNome.PROFESSOR)) {
            throw new NegocioException("Usuário informado não possui papel de professor");
        }

        if (!professor.isAtivo()) {
            throw new NegocioException("Professor inativo não pode avaliar questões");
        }

        if (questao.isAutor(ator.id())) {
            throw new NegocioException("O autor da questão não pode avaliar a própria questão");
        }

        if (questao.getStatus() == StatusQuestao.RASCUNHO) {
            throw new NegocioException("Questão ainda é um rascunho: o autor precisa enviá-la para validação antes");
        }
        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new NegocioException("A validação desta questão já foi encerrada");
        }

        if (avaliacaoQuestaoRepository.existsByQuestaoIdAndProfessorId(questaoId, ator.id())) {
            throw new NegocioException("Professor já avaliou esta questão");
        }

        if (avaliacaoQuestaoRepository.countByQuestaoId(questaoId) >= MAXIMO_VOTOS) {
            // não deve acontecer (o quórum fecha antes), mas protege contra dados inconsistentes
            throw new NegocioException("A questão já recebeu o máximo de " + MAXIMO_VOTOS + " votos");
        }

        avaliacaoQuestaoRepository.save(new AvaliacaoQuestao(questao, professor, voto, comentario));

        long aprovacoes = avaliacaoQuestaoRepository.countByQuestaoIdAndVoto(questaoId, VotoAvaliacao.APROVAR);
        long rejeicoes = avaliacaoQuestaoRepository.countByQuestaoIdAndVoto(questaoId, VotoAvaliacao.REJEITAR);
        questao.apurarVotos(aprovacoes, rejeicoes);
        // sem save explícito: "questao" é gerenciada pela transação (dirty checking)

        auditoriaService.registrar(ator, AcaoAuditoria.AVALIACAO_QUESTAO, "Questao", questaoId, true,
                voto + " -> " + questao.getStatus());

        return new AvaliacaoQuestaoResponse(
                questaoId, questao.getStatus(), aprovacoes, rejeicoes, VOTOS_PARA_DECIDIR, MAXIMO_VOTOS);
    }
}
