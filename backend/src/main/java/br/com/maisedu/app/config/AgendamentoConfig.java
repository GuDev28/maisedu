package br.com.maisedu.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita rotinas agendadas (ex.: limpeza dos registros de auditoria vencidos). */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
