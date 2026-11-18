package com.pokedex.api.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Participante de uma batalha. Guarda uma cópia (snapshot) do Pokémon no momento em que a batalha
 * começou, para que editar ou excluir o Pokémon depois não altere o histórico.
 */
@JsonPropertyOrder({"playerName", "pokemonId", "pokemonName", "types", "hp", "attack", "defense",
        "specialAttack", "specialDefense", "speed", "maxHp", "currentHp", "defending", "lastAction"})
public class BattleFighter {

    private String playerName;
    private Integer pokemonId;
    private String pokemonName;
    private List<String> types = new ArrayList<>();
    private int hp;
    private int attack;
    private int defense;
    private int specialAttack;
    private int specialDefense;
    private int speed;
    private int maxHp;
    private int currentHp;
    private boolean defending;
    private BattleAction lastAction;

    public BattleFighter() {
    }

    public static BattleFighter of(String playerName, Pokemon pokemon) {
        BattleFighter fighter = new BattleFighter();
        fighter.playerName = playerName;
        fighter.pokemonId = pokemon.getId();
        fighter.pokemonName = pokemon.getName();
        fighter.types = new ArrayList<>(pokemon.getTypes());
        fighter.hp = pokemon.getHp();
        fighter.attack = pokemon.getAttack();
        fighter.defense = pokemon.getDefense();
        fighter.specialAttack = pokemon.getSpecialAttack();
        fighter.specialDefense = pokemon.getSpecialDefense();
        fighter.speed = pokemon.getSpeed();
        return fighter;
    }

    public boolean isFainted() {
        return currentHp <= 0;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Integer getPokemonId() {
        return pokemonId;
    }

    public void setPokemonId(Integer pokemonId) {
        this.pokemonId = pokemonId;
    }

    public String getPokemonName() {
        return pokemonName;
    }

    public void setPokemonName(String pokemonName) {
        this.pokemonName = pokemonName;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types == null ? new ArrayList<>() : new ArrayList<>(types);
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public int getSpecialAttack() {
        return specialAttack;
    }

    public void setSpecialAttack(int specialAttack) {
        this.specialAttack = specialAttack;
    }

    public int getSpecialDefense() {
        return specialDefense;
    }

    public void setSpecialDefense(int specialDefense) {
        this.specialDefense = specialDefense;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = currentHp;
    }

    public boolean isDefending() {
        return defending;
    }

    public void setDefending(boolean defending) {
        this.defending = defending;
    }

    public BattleAction getLastAction() {
        return lastAction;
    }

    public void setLastAction(BattleAction lastAction) {
        this.lastAction = lastAction;
    }
}
