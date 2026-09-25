package br.com.maisedu.app.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SenhaGeradora {

    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TAMANHO = 10;

    private final SecureRandom random = new SecureRandom();

    public String gerar() {
        StringBuilder senha = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            senha.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }
}
