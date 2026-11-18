import { test } from 'node:test'
import assert from 'node:assert/strict'
import { battleHp, hpTone, specialMoveName, switchOptions } from '../src/components/battle/battleRules.js'

test('HP de batalha igual ao calculado pela API (HP base + 60)', () => {
  assert.equal(battleHp(35), 95)   // Pikachu
  assert.equal(battleHp(78), 138)  // Charizard
  assert.equal(battleHp(250), 310) // Chansey
})

test('nome do golpe especial pelo primeiro tipo, igual ao backend', () => {
  assert.equal(specialMoveName(['Electric']), 'Golpe Elétrico')
  assert.equal(specialMoveName(['Fire', 'Flying']), 'Golpe Fogo')
  assert.equal(specialMoveName([]), 'Rajada Mística')
})

test('cor da barra de HP por faixa', () => {
  assert.equal(hpTone(100, 100), 'is-high')
  assert.equal(hpTone(40, 100), 'is-mid')
  assert.equal(hpTone(20, 100), 'is-low')
  assert.equal(hpTone(0, 100), 'is-low')
})

test('troca só oferece Pokémon vivos que não estão em campo', () => {
  const team = [
    { pokemonName: 'Pikachu', currentHp: 50 },
    { pokemonName: 'Squirtle', currentHp: 0 },
    { pokemonName: 'Bulbasaur', currentHp: 20 },
  ]
  assert.deepEqual(switchOptions(team, 0).map(({ index }) => index), [2])
  assert.deepEqual(switchOptions(team, 2).map(({ index }) => index), [0])
})
