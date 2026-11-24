package com.pokedex.api.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio dos desafios diários: o dia vira à meia-noite no horário de Brasília. */
@Configuration(proxyBeanMethods = false)
public class DailyConfig {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock dailyClock() {
        return Clock.system(ZONE);
    }
}
