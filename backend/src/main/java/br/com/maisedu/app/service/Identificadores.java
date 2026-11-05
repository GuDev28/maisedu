package br.com.maisedu.app.service;

import java.util.Locale;

/**
 * Normalização dos identificadores de acesso (e-mail e login): sem espaços nas pontas e em
 * minúsculas. Aplicada no cadastro e no login, para que "Prof@Escola.com" e "prof@escola.com"
 * sejam o mesmo usuário.
 */
public final class Identificadores {

    private Identificadores() {
    }

    public static String normalizar(String identificador) {
        if (identificador == null) {
            return null;
        }
        return identificador.strip().toLowerCase(Locale.ROOT);
    }
}
