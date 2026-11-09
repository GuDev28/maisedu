package br.com.maisedu.app.dto;

import br.com.maisedu.app.model.Turma;
import br.com.maisedu.app.model.Usuario;

import java.util.ArrayList;
import java.util.List;

/** Tela da turma: quem leciona e quem estuda nela (RN01). */
public record TurmaMembrosResponse(
        Long id,
        String nome,
        String anoEscolar,
        boolean ativo,
        List<Membro> professores,
        List<Membro> alunos) {

    /** {@code emailResponsavel} só para alunos (quem vê a turma é o admin ou o professor dela). */
    public record Membro(Long id, String nome, String identificadorDeAcesso, String emailResponsavel, boolean ativo,
                         boolean senhaTemporaria) {

        public static Membro from(Usuario usuario) {
            return new Membro(usuario.getId(), usuario.getNome(), usuario.identificadorDeAcesso(),
                    usuario.getEmailResponsavel(), usuario.isAtivo(),
                    usuario.isSenhaTemporaria());
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
