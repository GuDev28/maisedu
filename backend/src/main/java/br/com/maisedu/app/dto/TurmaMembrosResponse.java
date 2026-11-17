package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.StatusEmailAcesso;
import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/** Tela da turma: quem leciona e quem estuda nela (RN01). */
public record TurmaMembrosResponse(
        Long id,
        String nome,
        String anoEscolar,
        boolean ativo,
        List<Membro> professores,
        List<Membro> alunos) {

    /**
     * {@code emailResponsavel} só para alunos (quem vê a turma é o admin ou o professor dela).
     * {@code statusEmailAcesso}: situação do último e-mail com login e senha provisória (ENVIADO,
     * FALHOU ou NAO_CONFIGURADO), para a tela avisar quando o acesso não chegou.
     */
    public record Membro(Long id, String nome, String identificadorDeAcesso, String emailResponsavel, boolean ativo,
                         boolean senhaTemporaria, StatusEmailAcesso statusEmailAcesso, LocalDateTime emailAcessoEm) {

        public static Membro from(Usuario usuario) {
            return new Membro(usuario.getId(), usuario.getNome(), usuario.identificadorDeAcesso(),
                    usuario.getEmailResponsavel(), usuario.isAtivo(),
                    usuario.isSenhaTemporaria(), usuario.getEmailAcessoStatus(), usuario.getEmailAcessoEm());
        }
    }

    public static TurmaMembrosResponse from(Turma turma, List<Usuario> professores, List<Usuario> alunos) {
        List<Membro> listaProfessores = new ArrayList<>();
        for (Usuario professor : professores) {
            listaProfessores.add(Membro.from(professor));
        }
        List<Membro> listaAlunos = new ArrayList<>();
        for (Usuario aluno : alunos) {
            listaAlunos.add(Membro.from(aluno));
        }
        return new TurmaMembrosResponse(turma.getId(), turma.getNome(), turma.getAnoEscolar().name(), turma.isAtivo(),
                listaProfessores, listaAlunos);
    }
}
