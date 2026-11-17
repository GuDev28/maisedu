package br.com.maisedu.app.dto;

import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Uma página de resultados com formato fixo (independente de como o Spring Data serializa o Page).
 * {@code pagina} começa em 0.
 */
public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long total, int totalPaginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> conversor) {
        List<T> conteudo = new ArrayList<>();
        for (E item : pagina.getContent()) {
            conteudo.add(conversor.apply(item));
        }
        return new PaginaResponse<>(conteudo, pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
