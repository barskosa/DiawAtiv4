package com.pokedex.api;

import org.springframework.boot.SpringApplication;

public class TestPokedexApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(PokedexApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
