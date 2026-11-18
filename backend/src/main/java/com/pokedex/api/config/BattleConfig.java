package com.pokedex.api.config;

import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Gerador de números aleatórios da batalha (crítico, variação de dano e empates de velocidade). */
@Configuration(proxyBeanMethods = false)
public class BattleConfig {

    @Bean
    RandomGenerator battleRandom() {
        return RandomGenerator.getDefault();
    }
}
