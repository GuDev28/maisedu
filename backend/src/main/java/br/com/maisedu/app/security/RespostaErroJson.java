package br.com.maisedu.app.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Escreve {"mensagem": "..."} direto na resposta, no mesmo formato do ErroResponse dos
 * controllers. Usado onde a requisição é recusada antes de chegar a um controller (filtro JWT e
 * respostas 401/403 do Spring Security).
 */
public final class RespostaErroJson {

    private RespostaErroJson() {
    }

    public static void escrever(HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensagem\":\"" + escapar(mensagem) + "\"}");
    }

    private static String escapar(String texto) {
        StringBuilder saida = new StringBuilder(texto.length() + 8);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> saida.append(c);
            }
        }
        return saida.toString();
    }
}
