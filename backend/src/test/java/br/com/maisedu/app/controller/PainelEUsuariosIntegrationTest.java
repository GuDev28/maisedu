package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AlunoTurmaResponse;
import br.com.maisedu.app.dto.CadastroTurmaRequest;
import br.com.maisedu.app.dto.PaginaResponse;
import br.com.maisedu.app.dto.PainelEscolaResponse;
import br.com.maisedu.app.dto.ProfessorTurmaResponse;
import br.com.maisedu.app.dto.TurmaGestaoResponse;
import br.com.maisedu.app.dto.UsuarioGestaoResponse;
import br.com.maisedu.app.exception.ErroResponse;
import br.com.maisedu.app.model.AnoEscolar;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Lista paginada de usuários (admin e professor) e painel da escola com dados só agregados. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PainelEUsuariosIntegrationTest extends IntegracaoTestBase {

    @Test
    void adminListaOsUsuariosDaEscolaEmPaginasEComBusca() {
        Conta admin = criarAdmin();
        Conta professor1 = cadastrarProfessor(admin);
        cadastrarProfessor(admin);

        PaginaResponse<UsuarioGestaoResponse> primeira = listar(admin, "/usuarios?papel=PROFESSOR&tamanho=1&pagina=0");
        assertThat(primeira.conteudo()).hasSize(1);
        // o admin também tem o papel de professor: são pelo menos 3
        assertThat(primeira.total()).isGreaterThanOrEqualTo(3L);
        assertThat(primeira.totalPaginas()).isGreaterThanOrEqualTo(3);

        PaginaResponse<UsuarioGestaoResponse> busca = listar(admin, "/usuarios?busca=" + professor1.nome().split(" ")[1]);
        assertThat(idsDe(busca)).containsExactly(professor1.id());
    }

    @Test
    void professorListaSoOsAlunosDasSuasTurmas() {
        Conta admin = criarAdmin();
        Conta professor = cadastrarProfessor(admin);
        Conta outroProfessor = cadastrarProfessor(admin);
        Long turmaId = criarTurma(admin, AnoEscolar.SEXTO_ANO);
        vincularProfessor(admin, turmaId, professor);
        Conta alunoDaTurma = cadastrarAluno(professor);
        vincularAluno(professor, turmaId, alunoDaTurma);
        // aluno cadastrado pelo outro professor e fora das turmas do primeiro
        Conta alunoDeFora = cadastrarAluno(outroProfessor);

        PaginaResponse<UsuarioGestaoResponse> alunos = listar(professor, "/usuarios");

        assertThat(idsDe(alunos)).contains(alunoDaTurma.id());
        assertThat(idsDe(alunos)).doesNotContain(alunoDeFora.id(), outroProfessor.id());
    }

    @Test
    void painelDaEscolaSoParaOAdminESemPercentualEmGrupoPequeno() {
        Conta admin = criarAdmin();
        Conta professor = cadastrarProfessor(admin);
        Long turmaId = criarTurma(admin, AnoEscolar.SETIMO_ANO);
        vincularProfessor(admin, turmaId, professor);
        Conta aluno = cadastrarAluno(professor);
        vincularAluno(professor, turmaId, aluno);

        ResponseEntity<PainelEscolaResponse> resposta = client.get().uri("/painel/escola")
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(PainelEscolaResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        PainelEscolaResponse painel = resposta.getBody();
        assertThat(painel.minimoAlunosPorGrupo()).isEqualTo(3);
        assertThat(painel.engajamento()).hasSize(8);
        PainelEscolaResponse.TurmaComparada turma = null;
        for (PainelEscolaResponse.TurmaComparada linha : painel.turmas()) {
            if (linha.turmaId().equals(turmaId)) {
                turma = linha;
            }
        }
        assertThat(turma).isNotNull();
        assertThat(turma.alunos()).isEqualTo(1);
        // turma com menos de 3 alunos com respostas: sem percentual (minimização)
        assertThat(turma.percentual()).isNull();

        ResponseEntity<ErroResponse> negado = client.get().uri("/painel/escola")
                .header(HttpHeaders.COOKIE, cookie(professor)).retrieve().toEntity(ErroResponse.class);
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------- helpers ----------

    private PaginaResponse<UsuarioGestaoResponse> listar(Conta conta, String uri) {
        ResponseEntity<PaginaResponse<UsuarioGestaoResponse>> resposta = client.get().uri(uri)
                .header(HttpHeaders.COOKIE, cookie(conta)).retrieve()
                .toEntity(new ParameterizedTypeReference<PaginaResponse<UsuarioGestaoResponse>>() {});
        assertThat(resposta.getStatusCode()).as("GET " + uri).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    private static List<Long> idsDe(PaginaResponse<UsuarioGestaoResponse> pagina) {
        List<Long> ids = new ArrayList<>();
        for (UsuarioGestaoResponse usuario : pagina.conteudo()) {
            ids.add(usuario.id());
        }
        return ids;
    }

    private Long criarTurma(Conta admin, AnoEscolar ano) {
        ResponseEntity<TurmaGestaoResponse> criada = client.post().uri("/turmas")
                .header(HttpHeaders.COOKIE, cookie(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CadastroTurmaRequest("Turma " + System.nanoTime(), ano))
                .retrieve()
                .toEntity(TurmaGestaoResponse.class);
        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return criada.getBody().id();
    }

    private void vincularProfessor(Conta admin, Long turmaId, Conta professor) {
        assertThat(client.post().uri("/turmas/" + turmaId + "/professores/" + professor.id())
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(ProfessorTurmaResponse.class)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void vincularAluno(Conta professor, Long turmaId, Conta aluno) {
        assertThat(client.post().uri("/turmas/" + turmaId + "/alunos/" + aluno.id())
                .header(HttpHeaders.COOKIE, cookie(professor)).retrieve().toEntity(AlunoTurmaResponse.class)
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
