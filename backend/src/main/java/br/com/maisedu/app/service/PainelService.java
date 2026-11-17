package br.com.maisedu.app.service;

import br.com.maisedu.app.dto.PainelEscolaResponse;
import br.com.maisedu.app.dto.PainelInstituicaoResponse;
import br.com.maisedu.app.exception.NegocioException;
import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.DesempenhoTopico;
import br.com.maisedu.app.model.Instituicao;
import br.com.maisedu.app.model.RoleNome;
import br.com.maisedu.app.model.StatusQuestao;
import br.com.maisedu.app.model.TipoAtividade;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;
import br.com.maisedu.app.repository.AtividadeAlunoRepository;
import br.com.maisedu.app.repository.AtividadeRepository;
import br.com.maisedu.app.repository.AvaliacaoQuestaoRepository;
import br.com.maisedu.app.repository.DesempenhoTopicoRepository;
import br.com.maisedu.app.repository.InstituicaoRepository;
import br.com.maisedu.app.repository.LogAuditoriaRepository;
import br.com.maisedu.app.repository.Projecoes;
import br.com.maisedu.app.repository.QuestaoRepository;
import br.com.maisedu.app.repository.RespostaRepository;
import br.com.maisedu.app.repository.TurmaRepository;
import br.com.maisedu.app.repository.UsuarioRepository;
import br.com.maisedu.app.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Comparator;

/**
 * Números da página Início do administrador e o painel pedagógico da escola, sempre da própria
 * instituição e só com dados agregados (sem identificar alunos).
 */
@Service
@RequiredArgsConstructor
public class PainelService {

    private final InstituicaoRepository instituicaoRepository;
    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;
    private final QuestaoRepository questaoRepository;
    private final DesempenhoTopicoRepository desempenhoTopicoRepository;
    private final AtividadeRepository atividadeRepository;
    private final AtividadeAlunoRepository atividadeAlunoRepository;
    private final RespostaRepository respostaRepository;
    private final AvaliacaoQuestaoRepository avaliacaoQuestaoRepository;

    /** Grupos (ano + tópico, turma) com menos alunos que isto aparecem sem percentual. */
    public static final int MINIMO_ALUNOS_POR_GRUPO = 3;
    public static final int SEMANAS_DE_ENGAJAMENTO = 8;
    public static final int DIAS_ALUNO_ATIVO = 14;

    @Transactional(readOnly = true)
    public PainelInstituicaoResponse resumoDaInstituicao(UsuarioAutenticado admin) {
        if (!admin.possuiRole(RoleNome.ADMIN)) {
            throw new AccessDeniedException("Só o administrador vê o resumo da instituição.");
        }
        Instituicao instituicao = instituicaoRepository.findById(admin.instituicaoId())
                .orElseThrow(() -> new NegocioException("Instituição não encontrada."));

        long turmasAtivas = 0;
        long turmasInativas = 0;
        for (Turma turma : turmaRepository.findByInstituicaoIdOrderByAtivoDescNomeAsc(instituicao.getId())) {
            if (turma.isAtivo()) {
                turmasAtivas++;
            } else {
                turmasInativas++;
            }
        }

        long professores = 0;
        long alunos = 0;
        long bloqueadas = 0;
        long primeiroAcesso = 0;
        long desativadas = 0;
        for (Usuario usuario : usuarioRepository.findDaInstituicao(instituicao.getId(), null)) {
            if (usuario.isAnonimizado()) {
                continue;
            }
            if (!usuario.isAtivo()) {
                desativadas++;
                continue;
            }
            if (usuario.possuiRole(RoleNome.PROFESSOR)) {
                professores++;
            }
            if (usuario.possuiRole(RoleNome.ALUNO)) {
                alunos++;
            }
            if (usuario.estaBloqueado()) {
                bloqueadas++;
            }
            if (usuario.isSenhaTemporaria()) {
                primeiroAcesso++;
            }
        }

        long logins = logAuditoriaRepository.countByInstituicaoIdAndAcaoAndSucessoTrueAndCriadoEmAfter(
                instituicao.getId(), AcaoAuditoria.LOGIN, LocalDateTime.now().minusDays(7));
        long noBanco = questaoRepository.countByStatus(StatusQuestao.APROVADA);

        return new PainelInstituicaoResponse(instituicao.getNome(), turmasAtivas, turmasInativas, professores, alunos,
                bloqueadas, primeiroAcesso, desativadas, logins, noBanco);
    }

    /**
     * Painel da escola (admin): mapa ano escolar × tópico, comparativo entre turmas, engajamento das
     * últimas semanas e participação no banco compartilhado. Tudo agregado no banco ou em memória a
     * partir de contagens; nenhum nome ou resultado individual sai daqui.
     */
    @Transactional(readOnly = true)
    public PainelEscolaResponse desempenhoDaEscola(UsuarioAutenticado admin) {
        if (!admin.possuiRole(RoleNome.ADMIN)) {
            throw new AccessDeniedException("Só o administrador vê o painel da escola.");
        }
        Long instituicaoId = admin.instituicaoId();
        Instituicao instituicao = instituicaoRepository.findById(instituicaoId)
                .orElseThrow(() -> new NegocioException("Instituição não encontrada."));

        List<PainelEscolaResponse.CelulaAnoTopico> mapa = new ArrayList<>();
        for (Projecoes.DesempenhoAnoTopico linha : desempenhoTopicoRepository.somarPorAnoETopico(instituicaoId)) {
            int alunos = inteiro(linha.alunos());
            int respostas = inteiro(linha.respostas());
            mapa.add(new PainelEscolaResponse.CelulaAnoTopico(linha.anoEscolar().name(), linha.topicoId(),
                    linha.topico(), alunos, respostas, percentualSeHouverAlunos(linha.acertos(), linha.respostas(), alunos)));
        }
        // ordem do ano escolar (6º ao 9º), não a alfabética do nome do enum
        mapa.sort(Comparator.comparing((PainelEscolaResponse.CelulaAnoTopico celula) -> AnoEscolar.valueOf(celula.anoEscolar()))
                .thenComparing(PainelEscolaResponse.CelulaAnoTopico::topico));

        LocalDateTime agora = LocalDateTime.now();
        Map<Long, Long> ativosPorTurma = new HashMap<>();
        for (Projecoes.ContagemPorId contagem : respostaRepository.contarAlunosAtivosPorTurma(instituicaoId,
                agora.minusDays(DIAS_ALUNO_ATIVO))) {
            ativosPorTurma.put(contagem.id(), contagem.total());
        }
        Map<Long, Long> atividadesPorTurma = new HashMap<>();
        for (Projecoes.ContagemPorId contagem : atividadeRepository.contarPorTurma(instituicaoId)) {
            atividadesPorTurma.put(contagem.id(), contagem.total());
        }
        List<PainelEscolaResponse.TurmaComparada> turmas = new ArrayList<>();
        for (Projecoes.DesempenhoDaTurma linha : desempenhoTopicoRepository.somarPorTurma(instituicaoId)) {
            int comRespostas = inteiro(linha.alunosComRespostas());
            turmas.add(new PainelEscolaResponse.TurmaComparada(linha.turmaId(), linha.turma(),
                    linha.anoEscolar().name(), inteiro(linha.alunos()), comRespostas,
                    inteiro(ativosPorTurma.get(linha.turmaId())), inteiro(atividadesPorTurma.get(linha.turmaId())),
                    inteiro(linha.respostas()), percentualSeHouverAlunos(linha.acertos(), linha.respostas(), comRespostas)));
        }
        turmas.sort(Comparator.comparing((PainelEscolaResponse.TurmaComparada turma) -> AnoEscolar.valueOf(turma.anoEscolar()))
                .thenComparing(PainelEscolaResponse.TurmaComparada::turma));

        return new PainelEscolaResponse(instituicao.getNome(), MINIMO_ALUNOS_POR_GRUPO, mapa, turmas,
                engajamento(instituicaoId, agora), bancoCompartilhado(instituicaoId));
    }

    /** Últimas semanas (a atual inclusive), com zero nas semanas sem nada. */
    private List<PainelEscolaResponse.SemanaEngajamento> engajamento(Long instituicaoId, LocalDateTime agora) {
        LocalDate semanaAtual = agora.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate primeira = semanaAtual.minusWeeks(SEMANAS_DE_ENGAJAMENTO - 1);
        LocalDateTime desde = primeira.atStartOfDay();

        Map<LocalDate, int[]> porSemana = new TreeMap<>();
        Map<LocalDate, Set<Long>> alunosPorSemana = new HashMap<>();
        for (int i = 0; i < SEMANAS_DE_ENGAJAMENTO; i++) {
            porSemana.put(primeira.plusWeeks(i), new int[3]);
            alunosPorSemana.put(primeira.plusWeeks(i), new HashSet<>());
        }
        for (Projecoes.RespostaDoAluno resposta : respostaRepository.findDaInstituicaoDesde(instituicaoId, desde)) {
            LocalDate semana = semanaDe(resposta.respondidaEm());
            int[] numeros = porSemana.get(semana);
            if (numeros != null) {
                numeros[0]++;
                alunosPorSemana.get(semana).add(resposta.alunoId());
            }
        }
        for (LocalDateTime criada : atividadeRepository.findDatasDeCriacao(instituicaoId, desde)) {
            int[] numeros = porSemana.get(semanaDe(criada));
            if (numeros != null) {
                numeros[1]++;
            }
        }
        for (LocalDateTime iniciada : atividadeAlunoRepository.findDatasDeInicio(instituicaoId,
                TipoAtividade.ADAPTATIVA, desde)) {
            int[] numeros = porSemana.get(semanaDe(iniciada));
            if (numeros != null) {
                numeros[2]++;
            }
        }
        List<PainelEscolaResponse.SemanaEngajamento> semanas = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> entrada : porSemana.entrySet()) {
            int[] numeros = entrada.getValue();
            semanas.add(new PainelEscolaResponse.SemanaEngajamento(entrada.getKey(),
                    alunosPorSemana.get(entrada.getKey()).size(), numeros[0], numeros[1], numeros[2]));
        }
        return semanas;
    }

    private PainelEscolaResponse.BancoCompartilhado bancoCompartilhado(Long instituicaoId) {
        long rascunhos = 0;
        long pendentes = 0;
        long aprovadas = 0;
        long rejeitadas = 0;
        for (Projecoes.ContagemStatusQuestao contagem : questaoRepository.contarDaInstituicaoPorStatus(instituicaoId)) {
            switch (contagem.status()) {
                case RASCUNHO -> rascunhos = contagem.total();
                case PENDENTE -> pendentes = contagem.total();
                case APROVADA -> aprovadas = contagem.total();
                case REJEITADA -> rejeitadas = contagem.total();
            }
        }
        return new PainelEscolaResponse.BancoCompartilhado(rascunhos, pendentes, aprovadas, rejeitadas,
                avaliacaoQuestaoRepository.contarVotosDaInstituicao(instituicaoId),
                atividadeRepository.contarUsosPorOutrasEscolas(instituicaoId),
                atividadeRepository.contarUsosNaPropriaEscola(instituicaoId),
                atividadeRepository.contarQuestoesDeOutrasEscolasUsadas(instituicaoId));
    }

    private static LocalDate semanaDe(LocalDateTime data) {
        return data.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Percentual de acerto, ou nulo quando o grupo tem poucos alunos (minimização). */
    private static BigDecimal percentualSeHouverAlunos(Long acertos, Long respostas, int alunos) {
        if (alunos < MINIMO_ALUNOS_POR_GRUPO || respostas == null || respostas == 0) {
            return null;
        }
        return DesempenhoTopico.percentual(acertos == null ? 0 : acertos, respostas);
    }

    private static int inteiro(Long valor) {
        return valor == null ? 0 : valor.intValue();
    }
}
