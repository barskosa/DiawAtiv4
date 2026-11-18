package com.pokedex.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Documento da coleção {@code pokemons}.
 *
 * <p>O {@code id} é o número da Pokédex e vira o {@code _id} do MongoDB. Os nomes dos tipos ficam
 * embutidos no documento, porque são poucos, mudam raramente e sempre são exibidos com o Pokémon.</p>
 */
@Document(collection = "pokemons")
@JsonPropertyOrder({"id", "name", "hp", "attack", "defense", "specialAttack", "specialDefense", "speed", "types"})
public class Pokemon {

    @Id
    private Integer id;
    private String name;
    private int hp;
    private int attack;
    private int defense;
    private int specialAttack;
    private int specialDefense;
    private int speed;
    private List<String> types = new ArrayList<>();

    public Pokemon() {
    }

    public Pokemon(Integer id, String name, int hp, int attack, int defense, int specialAttack,
            int specialDefense, int speed, List<String> types) {
        this.id = id;
        this.name = name;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.specialAttack = specialAttack;
        this.specialDefense = specialDefense;
        this.speed = speed;
        setTypes(types);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types == null ? new ArrayList<>() : new ArrayList<>(types);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pokemon pokemon)) {
            return false;
        }
        return hp == pokemon.hp && attack == pokemon.attack && defense == pokemon.defense
                && specialAttack == pokemon.specialAttack && specialDefense == pokemon.specialDefense
                && speed == pokemon.speed && Objects.equals(id, pokemon.id) && Objects.equals(name, pokemon.name)
                && Objects.equals(types, pokemon.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, hp, attack, defense, specialAttack, specialDefense, speed, types);
    }

    @Override
    public String toString() {
        return "Pokemon{id=" + id + ", name='" + name + "', types=" + types + "}";
    }
}
