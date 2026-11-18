package com.pokedex.api.config;

import java.util.Random;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Gerador de números aleatórios da batalha (crítico, variação de dano e empates de velocidade). */
@Configuration(proxyBeanMethods = false)
public class BattleConfig {

    /**
     * java.util.Random é thread-safe e existe em qualquer JRE. RandomGenerator.getDefault() não serve
     * aqui: depende do módulo jdk.random, ausente na imagem eclipse-temurin:21-jre do Docker.
     */
    @Bean
    RandomGenerator battleRandom() {
        return new Random();
    }
}
