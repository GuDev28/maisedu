package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.CadastroAlunoRequest;
import br.com.maisedu.app.dto.CadastroProfessorRequest;
import br.com.maisedu.app.dto.CadastroTurmaRequest;
import br.com.maisedu.app.dto.ContaResponse;
import br.com.maisedu.app.dto.DadosPessoaisResponse;
import br.com.maisedu.app.dto.EmailResponsavelRequest;
import br.com.maisedu.app.dto.PainelInstituicaoResponse;
import br.com.maisedu.app.dto.PraticaAdaptativaResponse;
import br.com.maisedu.app.dto.TrocarSenhaRequest;
import br.com.maisedu.app.dto.TurmaGestaoResponse;
import br.com.maisedu.app.dto.TurmaMembrosResponse;
import br.com.maisedu.app.dto.UsuarioCadastradoResponse;
import br.com.maisedu.app.dto.UsuarioResumoResponse;
import br.com.maisedu.app.exception.ErroResponse;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.VotoAvaliacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Telas novas da apresentação, ponta a ponta: tela da Turma (membros, cadastrar já vinculando e
 * desvincular), Minha conta (dados, baixar meus dados, troca voluntária de senha), Início do admin
 * e prática diagnóstica de quem ainda não respondeu nada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TurmaContaPainelIntegrationTest extends IntegracaoTestBase {

    private Conta admin;
    private Long turmaId;

    @BeforeEach
    void criarTurma() {
        admin = criarAdmin();
        ResponseEntity<TurmaGestaoResponse> turma = client.post().uri("/turmas")
                .header(HttpHeaders.COOKIE, cookie(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CadastroTurmaRequest("6º ano B " + System.nanoTime(), AnoEscolar.SEXTO_ANO))
                .retrieve()
                .toEntity(TurmaGestaoResponse.class);
        assertThat(turma.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        turmaId = turma.getBody().id();
    }

    @Test
    void cadastroComTurmaJaVinculaEOProfessorDesvinculaOAluno() {
        // admin cadastra o professor já na turma (1 passo em vez de 2)
        long ts = System.nanoTime();
        ResponseEntity<UsuarioCadastradoResponse> professorCriado = client.post().uri("/professores")
                .header(HttpHeaders.COOKIE, cookie(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CadastroProfessorRequest("Professora " + ts, "prof.turma." + ts + "@maisedu.test", turmaId))
                .retrieve()
                .toEntity(UsuarioCadastradoResponse.class);
        assertThat(professorCriado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Conta professor = ativarConta(new NovoCadastro(professorCriado.getBody().id(), "Professora " + ts,
                "prof.turma." + ts + "@maisedu.test", senhaProvisoriaEnviadaPara(professorCriado.getBody().id())));

        // o professor cadastra o aluno já na turma
        ResponseEntity<UsuarioCadastradoResponse> alunoCriado = client.post().uri("/alunos")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CadastroAlunoRequest("Aluno " + ts, "aluno.turma." + ts, "mae." + ts + "@familia.test",
                        turmaId))
                .retrieve()
                .toEntity(UsuarioCadastradoResponse.class);
        assertThat(alunoCriado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(alunoCriado.getBody().senhaProvisoria()).as("a senha vai para o responsável").isNull();
        Long alunoId = alunoCriado.getBody().id();
        assertThat(senhaEnviadaAoResponsavel(alunoId, "mae." + ts + "@familia.test")).isNotBlank();

        TurmaMembrosResponse membros = membros(professor);
        assertThat(idsDosMembros(membros.professores())).containsExactly(professor.id());
        assertThat(idsDosMembros(membros.alunos())).containsExactly(alunoId);
        assertThat(membros.alunos().get(0).senhaTemporaria()).isTrue();
        assertThat(membros.alunos().get(0).emailResponsavel()).isEqualTo("mae." + ts + "@familia.test");

        // o professor corrige o e-mail do responsável digitado errado
        ResponseEntity<Void> correcao = client.put().uri("/usuarios/" + alunoId + "/email-responsavel")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmailResponsavelRequest("pai." + ts + "@familia.test"))
                .retrieve()
                .toBodilessEntity();
        assertThat(correcao.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(membros(professor).alunos().get(0).emailResponsavel()).isEqualTo("pai." + ts + "@familia.test");

        // professor não tira professor da turma (só o admin)
        ResponseEntity<ErroResponse> negado = client.delete().uri("/turmas/" + turmaId + "/professores/" + professor.id())
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toEntity(ErroResponse.class);
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // professor tira o aluno da turma
        ResponseEntity<Void> removido = client.delete().uri("/turmas/" + turmaId + "/alunos/" + alunoId)
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toBodilessEntity();
        assertThat(removido.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(membros(professor).alunos()).isEmpty();

        // tirar de novo responde 404, não 400
        ResponseEntity<ErroResponse> deNovo = client.delete().uri("/turmas/" + turmaId + "/alunos/" + alunoId)
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toEntity(ErroResponse.class);
        assertThat(deNovo.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void minhaContaMostraOsDadosPermiteBaixarETrocarASenhaSemNovoAceite() {
        Conta professor = cadastrarProfessor(admin);

        ContaResponse conta = client.get().uri("/auth/conta")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .body(ContaResponse.class);
        assertThat(conta.nome()).isEqualTo(professor.nome());
        assertThat(conta.instituicao()).isNotBlank();
        assertThat(conta.versaoTermosAceita()).isEqualTo(conta.versaoTermosAtual());

        ResponseEntity<DadosPessoaisResponse> meusDados = client.get().uri("/auth/meus-dados")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toEntity(DadosPessoaisResponse.class);
        assertThat(meusDados.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meusDados.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment");
        assertThat(meusDados.getBody().cadastro().id()).isEqualTo(professor.id());

        UsuarioResumoResponse sessao = client.get().uri("/auth/me")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .body(UsuarioResumoResponse.class);
        assertThat(sessao.instituicao()).isEqualTo(conta.instituicao());

        // troca voluntária: sem marcar o aceite de novo
        ResponseEntity<Void> troca = client.post().uri("/auth/trocar-senha")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TrocarSenhaRequest(SENHA_DEFINITIVA, "OutraSenha#2026", false))
                .retrieve()
                .toBodilessEntity();
        assertThat(troca.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(login(professor.identificador(), "OutraSenha#2026")).isNotBlank();
    }

    @Test
    void inicioDoAdminContaTurmasEPessoasDaPropriaInstituicao() {
        Conta professor = cadastrarProfessor(admin);
        cadastrarAlunoSemAtivar(professor);

        PainelInstituicaoResponse painel = client.get().uri("/painel/instituicao")
                .header(HttpHeaders.COOKIE, cookie(admin))
                .retrieve()
                .body(PainelInstituicaoResponse.class);
        assertThat(painel.turmasAtivas()).isGreaterThanOrEqualTo(1);
        assertThat(painel.professores()).isGreaterThanOrEqualTo(1);
        assertThat(painel.alunos()).isGreaterThanOrEqualTo(1);
        assertThat(painel.primeiroAcessoPendente()).isGreaterThanOrEqualTo(1);
        assertThat(painel.loginsUltimos7Dias()).isGreaterThanOrEqualTo(1);

        ResponseEntity<ErroResponse> negado = client.get().uri("/painel/instituicao")
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toEntity(ErroResponse.class);
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void alunoSemNenhumaRespostaGanhaPraticaDiagnostica() {
        // uma questão aprovada do 6º ano (o helper cria questões do 6º ano)
        Conta autor = cadastrarProfessor(admin);
        Long questaoId = criarQuestaoPendente(autor, "Diagnóstica " + System.nanoTime());
        for (int i = 0; i < 3; i++) {
            votar(cadastrarProfessor(admin), questaoId, VotoAvaliacao.APROVAR);
        }

        Conta professor = cadastrarProfessor(admin);
        client.post().uri("/turmas/" + turmaId + "/professores/" + professor.id())
                .header(HttpHeaders.COOKIE, cookie(admin))
                .retrieve()
                .toBodilessEntity();
        NovoCadastro novo = cadastrarAlunoSemAtivar(professor);
        client.post().uri("/turmas/" + turmaId + "/alunos/" + novo.id())
                .header(HttpHeaders.COOKIE, cookie(professor))
                .retrieve()
                .toBodilessEntity();
        Conta aluno = ativarConta(novo);

        ResponseEntity<PraticaAdaptativaResponse> pratica = client.post().uri("/aluno/praticas-adaptativas")
                .header(HttpHeaders.COOKIE, cookie(aluno))
                .retrieve()
                .toEntity(PraticaAdaptativaResponse.class);

        assertThat(pratica.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(pratica.getBody().diagnostica()).isTrue();
        assertThat(pratica.getBody().nova()).isTrue();
        assertThat(pratica.getBody().totalQuestoes()).isGreaterThanOrEqualTo(1);
    }

    // ---------- helpers ----------

    /** O e-mail é um mock: a senha provisória é capturada no envio, como no cadastro da base. */
    private String senhaProvisoriaEnviadaPara(Long usuarioId) {
        ArgumentCaptor<String> senha = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarSenhaProvisoria(eq(usuarioId), any(), any(), senha.capture());
        return senha.getValue();
    }

    private TurmaMembrosResponse membros(Conta conta) {
        ResponseEntity<TurmaMembrosResponse> resposta = client.get().uri("/turmas/" + turmaId + "/membros")
                .header(HttpHeaders.COOKIE, cookie(conta))
                .retrieve()
                .toEntity(TurmaMembrosResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    private static List<Long> idsDosMembros(List<TurmaMembrosResponse.Membro> membros) {
        List<Long> ids = new ArrayList<>();
        for (TurmaMembrosResponse.Membro membro : membros) {
            ids.add(membro.id());
        }
        return ids;
    }
}
