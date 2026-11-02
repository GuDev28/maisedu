package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.RespostaResultadoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.exception.RecursoNaoEncontradoException;
import br.com.maisedu.app.model.Alternativa;
import br.com.maisedu.app.model.AtividadeAluno;
import br.com.maisedu.app.model.AtividadeAlunoQuestao;
import br.com.maisedu.app.model.Resposta;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AlternativaRepository;
import br.com.maisedu.app.repository.AtividadeAlunoQuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** RF16 (responder com correção automática), RN04 (integridade e unicidade) e RF17 (atualização do desempenho). */
@Service
@RequiredArgsConstructor
public class RespostaService {

    private final UsuarioRepository usuarioRepository;
    private final AtividadeAlunoQuestaoRepository atividadeAlunoQuestaoRepository;
    private final AlternativaRepository alternativaRepository;
    private final RespostaRepository respostaRepository;
    private final DesempenhoService desempenhoService;

    @Transactional
    public RespostaResultadoResponse responder(Long alunoId, Long atividadeAlunoId, Long atividadeAlunoQuestaoId,
                                               Long alternativaId) {
        // Trava a linha do aluno: respostas simultâneas dele (duplo clique, duas abas) são processadas
        // uma de cada vez, o que mantém a unicidade da resposta e os contadores de desempenho corretos.
        Usuario aluno = usuarioRepository.findByIdParaAtualizacao(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));

        AtividadeAlunoQuestao entregue = atividadeAlunoQuestaoRepository.findById(atividadeAlunoQuestaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão não encontrada nesta atividade."));
        AtividadeAluno atividadeAluno = entregue.getAtividadeAluno();

        // RN04: só responde questões entregues a ele, dentro da atividade informada.
        if (!atividadeAluno.getId().equals(atividadeAlunoId) || !atividadeAluno.pertenceAo(alunoId)) {
            throw new RecursoNaoEncontradoException("Questão não encontrada nesta atividade.");
        }
        // RN04: uma única resposta por questão entregue, sem alteração.
        if (respostaRepository.existsByAtividadeAlunoQuestaoId(entregue.getId())) {
            throw new NegocioException("Esta questão já foi respondida e a resposta não pode ser alterada.");
        }

        Alternativa alternativa = alternativaRepository.findById(alternativaId)
                .orElseThrow(() -> new NegocioException("Alternativa não encontrada."));
        // RN04: a alternativa precisa pertencer à questão (validado no construtor e por trigger no banco).
        Resposta resposta = respostaRepository.save(new Resposta(entregue, alternativa));
        boolean acertou = resposta.acertou();

        // RF17/CA03: o desempenho é atualizado na mesma transação da resposta.
        desempenhoService.registrarResposta(aluno, entregue.getQuestao(), acertou);

        List<Resposta> respostasDaAtividade = respostaRepository.findByAtividadeAluno(atividadeAluno.getId());
        int acertos = 0;
        for (Resposta registrada : respostasDaAtividade) {
            if (registrada.acertou()) {
                acertos++;
            }
        }
        int total = atividadeAluno.getQuestoes().size();
        if (respostasDaAtividade.size() >= total) {
            atividadeAluno.concluir();
        }

        Alternativa correta = entregue.getQuestao().alternativaCorreta();
        return new RespostaResultadoResponse(acertou, alternativa.getId(), correta.getId(), correta.letra(),
                atividadeAluno.isConcluida(), respostasDaAtividade.size(), acertos, total);
    }
}
