package br.com.maisedu.app.controller;

import br.com.maisedu.app.dto.AceiteTermosRequest;
import br.com.maisedu.app.dto.AlunoTurmaResponse;
import br.com.maisedu.app.dto.CadastroTurmaRequest;
import br.com.maisedu.app.dto.ProfessorTurmaResponse;
import br.com.maisedu.app.dto.SenhaRedefinidaResponse;
import br.com.maisedu.app.dto.TurmaGestaoResponse;
import br.com.maisedu.app.dto.UsuarioGestaoResponse;
import br.com.maisedu.app.dto.UsuarioResumoResponse;
import br.com.maisedu.app.exception.ErroResponse;
import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/** Revogação de sessões, isolamento da auditoria por instituição, redefinição de senha e novo aceite dos termos. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SegurancaSessaoIntegrationTest extends IntegracaoTestBase {

    @Test
    void logoutInvalidaOTokenNoServidor() {
        Conta admin = criarAdmin();
        Conta professor = cadastrarProfessor(admin);

        ResponseEntity<Void> logout = client.post().uri("/auth/logout")
                .header(HttpHeaders.COOKIE, cookie(professor)).retrieve().toBodilessEntity();
        assertEquals(HttpStatus.NO_CONTENT, logout.getStatusCode());

        // Um token copiado antes do logout não vale mais
        assertEquals(HttpStatus.UNAUTHORIZED, statusDoMe(professor));
    }

    @Test
    void desativarUsuarioDerrubaASessaoNaHora() {
        Conta admin = criarAdmin();
        Conta professor = cadastrarProfessor(admin);
        assertEquals(HttpStatus.OK, statusDoMe(professor));

        ResponseEntity<UsuarioGestaoResponse> desativado = client.post().uri("/usuarios/" + professor.id() + "/desativar")
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(UsuarioGestaoResponse.class);
        assertEquals(HttpStatus.OK, desativado.getStatusCode());
        assertFalse(desativado.getBody().ativo());

        assertEquals(HttpStatus.UNAUTHORIZED, statusDoMe(professor));
    }

    @Test
    void cadaAdminSoVeAAuditoriaDaPropriaInstituicao() {
        Conta adminA = criarAdmin();
        Conta adminB = criarAdmin();

        ResponseEntity<String> doA = client.get().uri("/auditoria/logs?usuarioId=" + adminA.id())
                .header(HttpHeaders.COOKIE, cookie(adminA)).retrieve().toEntity(String.class);
        ResponseEntity<String> doBSobreA = client.get().uri("/auditoria/logs?usuarioId=" + adminA.id())
                .header(HttpHeaders.COOKIE, cookie(adminB)).retrieve().toEntity(String.class);

        assertEquals(HttpStatus.OK, doA.getStatusCode());
        assertTrue(doA.getBody().contains(adminA.nome()), "o admin vê os próprios registros de login");
        assertEquals(HttpStatus.OK, doBSobreA.getStatusCode());
        assertFalse(doBSobreA.getBody().contains(adminA.nome()), "o admin de outra escola não vê nada de A");
    }

    @Test
    void auditoriaAbreSemFiltrosEFiltraPorAcaoEPeriodo() {
        Conta admin = criarAdmin();
        String hoje = java.time.LocalDate.now().toString();

        // a tela abre assim: sem nenhum filtro (era aqui que o PostgreSQL devolvia erro 500)
        ResponseEntity<String> semFiltros = client.get().uri("/auditoria/logs")
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(String.class);
        ResponseEntity<String> loginsDeHoje = client.get()
                .uri("/auditoria/logs?acao=LOGIN&de=" + hoje + "T00:00:00&ate=" + hoje + "T23:59:59")
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(String.class);
        ResponseEntity<String> logoutsDeHoje = client.get()
                .uri("/auditoria/logs?acao=LOGOUT&de=" + hoje + "T00:00:00")
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(String.class);

        assertEquals(HttpStatus.OK, semFiltros.getStatusCode());
        assertTrue(semFiltros.getBody().contains(admin.nome()));
        assertEquals(HttpStatus.OK, loginsDeHoje.getStatusCode());
        assertTrue(loginsDeHoje.getBody().contains("\"LOGIN\""), "o login de hoje aparece no filtro");
        assertEquals(HttpStatus.OK, logoutsDeHoje.getStatusCode());
        assertFalse(logoutsDeHoje.getBody().contains(admin.nome()), "o admin ainda não fez logout");
    }

    @Test
    void professorRedefineSenhaDeAlunoDaTurmaEAlunoTrocaNoProximoAcesso() {
        Conta admin = criarAdmin();
        Conta professor = cadastrarProfessor(admin);
        Long turmaId = client.post().uri("/turmas")
                .header(HttpHeaders.COOKIE, cookie(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CadastroTurmaRequest("Turma " + System.nanoTime(), AnoEscolar.SEXTO_ANO))
                .retrieve().toEntity(TurmaGestaoResponse.class).getBody().id();
        client.post().uri("/turmas/" + turmaId + "/professores/" + professor.id())
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(ProfessorTurmaResponse.class);
        Conta aluno = cadastrarAluno(professor);
        client.post().uri("/turmas/" + turmaId + "/alunos/" + aluno.id())
                .header(HttpHeaders.COOKIE, cookie(professor)).retrieve().toEntity(AlunoTurmaResponse.class);

        ResponseEntity<SenhaRedefinidaResponse> redefinicao = client.post()
                .uri("/usuarios/" + aluno.id() + "/redefinir-senha")
                .header(HttpHeaders.COOKIE, cookie(professor)).retrieve().toEntity(SenhaRedefinidaResponse.class);

        assertEquals(HttpStatus.OK, redefinicao.getStatusCode());
        // a senha nova vai para o e-mail do responsável, não para o professor
        assertNull(redefinicao.getBody().senhaProvisoria());
        assertTrue(redefinicao.getBody().enviadaPorEmail());
        ArgumentCaptor<String> capturada = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarNovaSenhaAoResponsavel(eq(aluno.id()), anyString(), anyString(), anyString(),
                anyString(), capturada.capture());
        String novaSenha = capturada.getValue();
        assertNotNull(novaSenha);
        // A sessão antiga do aluno caiu e a senha nova é provisória
        assertEquals(HttpStatus.UNAUTHORIZED, statusDoMe(aluno));
        String token = login(aluno.identificador(), novaSenha);
        ResponseEntity<UsuarioResumoResponse> sessao = client.get().uri("/auth/me")
                .header(HttpHeaders.COOKIE, cookie(token)).retrieve().toEntity(UsuarioResumoResponse.class);
        assertTrue(sessao.getBody().senhaTemporaria());
    }

    @Test
    void novaVersaoDosTermosExigeNovoAceiteAntesDeContinuar() {
        Conta admin = criarAdmin();
        // Simula um usuário que aceitou uma versão anterior dos termos
        Usuario usuario = usuarioRepository.findById(admin.id()).orElseThrow();
        ReflectionTestUtils.setField(usuario, "termosVersao", 0);
        usuarioRepository.save(usuario);

        ResponseEntity<ErroResponse> bloqueado = client.get().uri("/turmas")
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(ErroResponse.class);
        assertEquals(HttpStatus.FORBIDDEN, bloqueado.getStatusCode());
        assertTrue(bloqueado.getBody().mensagem().contains("Termos"));
        assertTrue(sessaoAtual(admin).termosPendentes());

        ResponseEntity<Void> aceite = client.post().uri("/auth/aceitar-termos")
                .header(HttpHeaders.COOKIE, cookie(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AceiteTermosRequest(true))
                .retrieve().toBodilessEntity();
        assertEquals(HttpStatus.NO_CONTENT, aceite.getStatusCode());

        ResponseEntity<String> liberado = client.get().uri("/turmas")
                .header(HttpHeaders.COOKIE, cookie(admin)).retrieve().toEntity(String.class);
        assertEquals(HttpStatus.OK, liberado.getStatusCode());
    }

    /** Só o status de GET /auth/me (o corpo de erro não é desserializado como sessão). */
    private org.springframework.http.HttpStatusCode statusDoMe(Conta conta) {
        return client.get().uri("/auth/me").header(HttpHeaders.COOKIE, cookie(conta)).retrieve()
                .toBodilessEntity().getStatusCode();
    }

    private UsuarioResumoResponse sessaoAtual(Conta conta) {
        ResponseEntity<UsuarioResumoResponse> resposta = client.get().uri("/auth/me")
                .header(HttpHeaders.COOKIE, cookie(conta)).retrieve().toEntity(UsuarioResumoResponse.class);
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        return resposta.getBody();
    }
}
