package com.pokedex.api.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.pokedex.api.exception.InvalidRequestException;
import com.pokedex.api.model.Pokemon;
import com.pokedex.api.model.Trainer;
import com.pokedex.api.repository.PokemonRepository;
import com.pokedex.api.repository.TrainerRepository;

/**
 * Sorteios dos desafios diários. A semente é a data (horário de Brasília) e o nome do desafio, então
 * todos os jogadores recebem o mesmo conteúdo no mesmo dia, e ele muda à meia-noite.
 *
 * <p>Os sorteios partem das listas ordenadas por número, para que a mesma data produza o mesmo
 * resultado em qualquer servidor (enquanto a Pokédex não muda).</p>
 */
@Service
public class DailyChallengeService {

    private final PokemonRepository pokemonRepository;
    private final TrainerRepository trainerRepository;
    private final Clock clock;

    public DailyChallengeService(PokemonRepository pokemonRepository, TrainerRepository trainerRepository, Clock clock) {
        this.pokemonRepository = pokemonRepository;
        this.trainerRepository = trainerRepository;
        this.clock = clock;
    }

    /** Data do desafio de hoje. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** {@code count} Pokémon distintos do dia para o desafio {@code challenge}. */
    public List<Pokemon> pokemonsOfTheDay(LocalDate date, String challenge, int count) {
        List<Pokemon> pool = new ArrayList<>(pokemonRepository.findAll());
        if (pool.size() < count) {
            throw new InvalidRequestException("pokemon", "A Pokédex precisa de pelo menos " + count
                    + " Pokémon para o desafio do dia.");
        }
        pool.sort(Comparator.comparing(Pokemon::getId));
        shuffle(pool, random(date, challenge));
        return List.copyOf(pool.subList(0, count));
    }

    /** Treinador do dia, entre os que têm Pokémon na equipe. */
    public Trainer trainerOfTheDay(LocalDate date) {
        List<Trainer> trainers = trainerRepository.findAll().stream()
                .filter(trainer -> !trainer.pokemonIds().isEmpty())
                .sorted(Comparator.comparing(Trainer::id))
                .toList();
        if (trainers.isEmpty()) {
            throw new InvalidRequestException("trainerId", "Nenhum treinador tem Pokémon para o desafio do dia.");
        }
        return trainers.get(random(date, "BATTLE_TRAINER").nextInt(trainers.size()));
    }

    /** Gerador com semente fixa para a data e o desafio (java.util.Random é igual em qualquer JVM). */
    static Random random(LocalDate date, String challenge) {
        return new Random(date.toEpochDay() * 1_000_003L + challenge.hashCode());
    }

    /** Embaralhamento de Fisher–Yates. */
    static <T> void shuffle(List<T> list, Random random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T swap = list.get(i);
            list.set(i, list.get(j));
            list.set(j, swap);
        }
    }
}
