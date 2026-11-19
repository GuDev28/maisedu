package br.com.maisedu.app.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * Gera o login do aluno: a letra "a" seguida de 6 dígitos sorteados (ex.: a482913). O login não
 * carrega nome, escola nem turma (minimização da LGPD) e é fácil de digitar por um aluno do 6º ano.
 * A unicidade no sistema inteiro é garantida por quem chama, que informa se o login já existe.
 */
@Component
public class GeradorLogin {

    public static final String PREFIXO = "a";
    private static final int DIGITOS = 6;
    private static final int TENTATIVAS = 20;

    private final SecureRandom random = new SecureRandom();

    /**
     * Sorteia logins até encontrar um livre. Depois de várias colisões seguidas (banco muito cheio),
     * passa a usar 8 dígitos, o que torna uma nova colisão praticamente impossível.
     */
    public String gerar(Predicate<String> jaExiste) {
        for (int tentativa = 0; tentativa < TENTATIVAS; tentativa++) {
            String candidato = sortear(tentativa < TENTATIVAS / 2 ? DIGITOS : DIGITOS + 2);
            if (!jaExiste.test(candidato)) {
                return candidato;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um login livre para o aluno. Tente novamente.");
    }

    private String sortear(int digitos) {
        StringBuilder login = new StringBuilder(PREFIXO);
        // primeiro dígito de 1 a 9: o login nunca começa com "a0" (evita confusão com a letra O)
        login.append(1 + random.nextInt(9));
        for (int i = 1; i < digitos; i++) {
            login.append(random.nextInt(10));
        }
        return login.toString();
    }
}
