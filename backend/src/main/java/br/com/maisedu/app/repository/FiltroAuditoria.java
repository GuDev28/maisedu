package br.com.maisedu.app.repository;

import br.com.maisedu.app.model.AcaoAuditoria;
import br.com.maisedu.app.model.LogAuditoria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros da consulta de auditoria (RF13). A instituição é sempre obrigatória; os outros só entram
 * na consulta quando foram informados, então nenhum parâmetro nulo chega ao banco.
 */
public final class FiltroAuditoria {

    private FiltroAuditoria() {
    }

    public static Specification<LogAuditoria> de(Long instituicaoId, Long usuarioId, AcaoAuditoria acao,
                                                 LocalDateTime de, LocalDateTime ate) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(raiz.get("instituicaoId"), instituicaoId));
            if (usuarioId != null) {
                condicoes.add(cb.equal(raiz.get("usuarioId"), usuarioId));
            }
            if (acao != null) {
                condicoes.add(cb.equal(raiz.get("acao"), acao));
            }
            if (de != null) {
                condicoes.add(cb.greaterThanOrEqualTo(raiz.<LocalDateTime>get("criadoEm"), de));
            }
            if (ate != null) {
                condicoes.add(cb.lessThanOrEqualTo(raiz.<LocalDateTime>get("criadoEm"), ate));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
    }
}
