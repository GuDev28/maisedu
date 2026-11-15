package br.com.maisedu.app.config;

import br.com.maisedu.app.service.MotorAdaptativo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Um único {@link MotorAdaptativo}, com os parâmetros de {@code maisedu.adaptativo.*} lidos num só
 * lugar. A prática adaptativa (RN06) e o painel de desempenho ("dominado", RN05) usam a mesma regra.
 */
@Configuration
public class AdaptativoConfig {

    @Bean
    public MotorAdaptativo motorAdaptativo(
            @Value("${maisedu.adaptativo.maximo-topicos:3}") int maximoTopicos,
            @Value("${maisedu.adaptativo.questoes-por-pratica:10}") int questoesPorPratica,
            @Value("${maisedu.adaptativo.minimo-respostas-dominio:5}") int minimoRespostasDominio,
            @Value("${maisedu.adaptativo.percentual-dominio:70}") BigDecimal percentualDominio) {
        return new MotorAdaptativo(new MotorAdaptativo.Parametros(
                maximoTopicos, questoesPorPratica, minimoRespostasDominio, percentualDominio));
    }
}
