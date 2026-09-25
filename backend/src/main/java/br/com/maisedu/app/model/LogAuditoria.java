package br.com.maisedu.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "log_auditoria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;

    private String usuarioNome;

    private String identificadorAcesso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoAuditoria acao;

    private String entidade;

    private Long entidadeId;

    @Column(nullable = false)
    private boolean sucesso;

    @Column(length = 500)
    private String detalhes;

    @Column(length = 45)
    private String ip;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    public LogAuditoria(Long usuarioId, String usuarioNome, String identificadorAcesso, AcaoAuditoria acao,
                         String entidade, Long entidadeId, boolean sucesso, String detalhes,
                         String ip, String userAgent) {
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.identificadorAcesso = identificadorAcesso;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.sucesso = sucesso;
        this.detalhes = detalhes;
        this.ip = ip;
        this.userAgent = userAgent;
        this.criadoEm = LocalDateTime.now();
    }
}
