package br.com.maisedu.app.service;

import br.com.maisedu.app.model.Resposta;

import java.util.List;

/** Respondidas e acertos de uma folha (atividade regular ou prática). Uma única regra de contagem. */
public record Placar(int respondidas, int acertos) {

    public static final Placar VAZIO = new Placar(0, 0);

    public static Placar de(List<Resposta> respostas) {
        int acertos = 0;
        for (Resposta resposta : respostas) {
            if (resposta.acertou()) {
                acertos++;
            }
        }
        return new Placar(respostas.size(), acertos);
    }
}
