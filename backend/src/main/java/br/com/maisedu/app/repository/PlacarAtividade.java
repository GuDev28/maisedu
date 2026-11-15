package br.com.maisedu.app.repository;

/** Contagem agregada de respostas e acertos por folha (AtividadeAluno), feita no banco. */
public record PlacarAtividade(Long atividadeAlunoId, Long respondidas, Long acertos) {
}
