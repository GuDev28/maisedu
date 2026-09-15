package br.com.maisedu.app.service;

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

    private final QuestaoRepository questaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;

    @Transactional
    public void avaliar(Long questaoId, Long professorId, VotoAvaliacao voto) {
        Questao questao = questaoRepository.findById(questaoId)
                .orElseThrow(() -> new NegocioException("Questão não encontrada"));

        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new NegocioException("Professor não encontrado"));

        if (!professor.possuiRole(RoleNome.PROFESSOR)) {
            throw new NegocioException("Usuário informado não possui papel de professor");
        }

        if (questao.getAutor().getId().equals(professorId)) {
            throw new NegocioException("O autor da questão não pode avaliar a própria questão");
        }

        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new NegocioException("Questão já foi avaliada anteriormente");
        }

        if (avaliacaoQuestaoRepository.existsByQuestaoIdAndProfessorId(questaoId, professorId)) {
            throw new NegocioException("Professor já avaliou esta questão");
        }

        avaliacaoQuestaoRepository.save(new AvaliacaoQuestao(questao, professor, voto));

        questao.setStatus(voto == VotoAvaliacao.APROVAR
                ? StatusQuestao.APROVADA
                : StatusQuestao.REJEITADA);
        // sem save explícito: "questao" é uma entidade gerenciada dentro da transação,
        // o Hibernate persiste a alteração de status sozinho (dirty checking)
    }
}
