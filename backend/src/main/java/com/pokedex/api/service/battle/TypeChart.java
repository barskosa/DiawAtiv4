package com.pokedex.api.service.battle;

import java.util.List;
import java.util.Map;

/**
 * Tabela de efetividade entre os tipos do catálogo do Script_Pokedex.sql (regras atuais dos jogos,
 * sem o tipo Fada, que não existe no script). Combinações ausentes valem 1x.
 */
public final class TypeChart {

    private TypeChart() {
    }

    private static final Map<String, Map<String, Double>> CHART = Map.ofEntries(
            Map.entry("Normal", Map.of("Rock", 0.5, "Steel", 0.5, "Ghost", 0.0)),
            Map.entry("Fire", Map.of("Grass", 2.0, "Ice", 2.0, "Bug", 2.0, "Steel", 2.0,
                    "Fire", 0.5, "Water", 0.5, "Rock", 0.5, "Dragon", 0.5)),
            Map.entry("Water", Map.of("Fire", 2.0, "Ground", 2.0, "Rock", 2.0,
                    "Water", 0.5, "Grass", 0.5, "Dragon", 0.5)),
            Map.entry("Electric", Map.of("Water", 2.0, "Flying", 2.0,
                    "Electric", 0.5, "Grass", 0.5, "Dragon", 0.5, "Ground", 0.0)),
            Map.entry("Grass", Map.of("Water", 2.0, "Ground", 2.0, "Rock", 2.0,
                    "Fire", 0.5, "Grass", 0.5, "Poison", 0.5, "Flying", 0.5, "Bug", 0.5, "Dragon", 0.5, "Steel", 0.5)),
            Map.entry("Ice", Map.of("Grass", 2.0, "Ground", 2.0, "Flying", 2.0, "Dragon", 2.0,
                    "Fire", 0.5, "Water", 0.5, "Ice", 0.5, "Steel", 0.5)),
            Map.entry("Fighting", Map.of("Normal", 2.0, "Ice", 2.0, "Rock", 2.0, "Dark", 2.0, "Steel", 2.0,
                    "Poison", 0.5, "Flying", 0.5, "Psychic", 0.5, "Bug", 0.5, "Ghost", 0.0)),
            Map.entry("Poison", Map.of("Grass", 2.0,
                    "Poison", 0.5, "Ground", 0.5, "Rock", 0.5, "Ghost", 0.5, "Steel", 0.0)),
            Map.entry("Ground", Map.of("Fire", 2.0, "Electric", 2.0, "Poison", 2.0, "Rock", 2.0, "Steel", 2.0,
                    "Grass", 0.5, "Bug", 0.5, "Flying", 0.0)),
            Map.entry("Flying", Map.of("Grass", 2.0, "Fighting", 2.0, "Bug", 2.0,
                    "Electric", 0.5, "Rock", 0.5, "Steel", 0.5)),
            Map.entry("Psychic", Map.of("Fighting", 2.0, "Poison", 2.0,
                    "Psychic", 0.5, "Steel", 0.5, "Dark", 0.0)),
            Map.entry("Bug", Map.of("Grass", 2.0, "Psychic", 2.0, "Dark", 2.0,
                    "Fire", 0.5, "Fighting", 0.5, "Poison", 0.5, "Flying", 0.5, "Ghost", 0.5, "Steel", 0.5)),
            Map.entry("Rock", Map.of("Fire", 2.0, "Ice", 2.0, "Flying", 2.0, "Bug", 2.0,
                    "Fighting", 0.5, "Ground", 0.5, "Steel", 0.5)),
            Map.entry("Ghost", Map.of("Psychic", 2.0, "Ghost", 2.0, "Dark", 0.5, "Normal", 0.0)),
            Map.entry("Dragon", Map.of("Dragon", 2.0, "Steel", 0.5)),
            Map.entry("Dark", Map.of("Psychic", 2.0, "Ghost", 2.0, "Fighting", 0.5, "Dark", 0.5)),
            Map.entry("Steel", Map.of("Ice", 2.0, "Rock", 2.0,
                    "Fire", 0.5, "Water", 0.5, "Electric", 0.5, "Steel", 0.5)));

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("Normal", "Normal"), Map.entry("Fire", "Fogo"), Map.entry("Water", "Água"),
            Map.entry("Grass", "Planta"), Map.entry("Electric", "Elétrico"), Map.entry("Ice", "Gelo"),
            Map.entry("Fighting", "Lutador"), Map.entry("Poison", "Veneno"), Map.entry("Ground", "Terra"),
            Map.entry("Flying", "Voador"), Map.entry("Psychic", "Psíquico"), Map.entry("Bug", "Inseto"),
            Map.entry("Rock", "Pedra"), Map.entry("Ghost", "Fantasma"), Map.entry("Dark", "Sombrio"),
            Map.entry("Dragon", "Dragão"), Map.entry("Steel", "Aço"));

    /**
     * Multiplicador de um golpe do tipo {@code attackType} contra um Pokémon com {@code defenderTypes}
     * (os multiplicadores dos dois tipos se combinam: 2 × 2 = 4, 2 × 0,5 = 1). Golpe sem tipo: 1x.
     */
    public static double effectiveness(String attackType, List<String> defenderTypes) {
        if (attackType == null || defenderTypes == null) {
            return 1.0;
        }
        Map<String, Double> row = CHART.getOrDefault(attackType, Map.of());
        double multiplier = 1.0;
        for (String defenderType : defenderTypes) {
            multiplier *= row.getOrDefault(defenderType, 1.0);
        }
        return multiplier;
    }

    /** Nome do tipo em português, para as mensagens da batalha. */
    public static String label(String type) {
        return LABELS.getOrDefault(type, type);
    }
}
