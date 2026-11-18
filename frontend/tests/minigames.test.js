import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  drawPokemon, highestStats, maxDexNumber, proximityHint, scoreNumberGuess, scoreStatGuess, scoreTypeGuess,
} from '../src/components/minigames/minigameLogic.js'

const stats = (hp, attack, defense, specialAttack, specialDefense, speed) => ({ hp, attack, defense, specialAttack, specialDefense, speed })
const BULBASAUR = { id: 1, name: 'Bulbasaur', types: [{ id: 4, name: 'Grass' }, { id: 8, name: 'Poison' }], stats: stats(45, 49, 49, 65, 65, 45) }
const PIKACHU = { id: 25, name: 'Pikachu', types: [{ id: 5, name: 'Electric' }], stats: stats(35, 55, 30, 50, 40, 90) }
const MEW = { id: 151, name: 'Mew', types: [], stats: stats(100, 100, 100, 100, 100, 100) }
const POOL = [BULBASAUR, PIKACHU, MEW]

test('sorteio não repete Pokémon na partida', () => {
  assert.equal(drawPokemon(POOL, [1, 25], () => 0.99), MEW)
  assert.equal(drawPokemon(POOL, [1], () => 0), PIKACHU)
  assert.equal(drawPokemon(POOL, [1, 25, 151], () => 0), BULBASAUR, 'todos usados: volta a sortear de todos')
  assert.equal(drawPokemon([], [], () => 0), null)
})

test('jogo do tipo: acerto exato, parcial e erros', () => {
  assert.equal(scoreTypeGuess(['Grass', 'Poison'], BULBASAUR).points, 100)
  assert.equal(scoreTypeGuess(['Poison', 'Grass'], BULBASAUR).correct, true, 'ordem não importa')
  assert.equal(scoreTypeGuess(['Grass'], BULBASAUR).points, 50)
  assert.equal(scoreTypeGuess(['Grass', 'Fire'], BULBASAUR).points, 25)
  assert.equal(scoreTypeGuess(['Fire', 'Water'], BULBASAUR).points, 0)
  assert.equal(scoreTypeGuess(['Electric', 'Steel'], PIKACHU).points, 25)
  assert.deepEqual(scoreTypeGuess(['Water'], PIKACHU).missed, ['Electric'])
})

test('jogo do tipo: Mew só pontua com Sem tipo', () => {
  assert.equal(scoreTypeGuess(['Typeless'], MEW).points, 100)
  assert.equal(scoreTypeGuess(['Psychic'], MEW).points, 0)
})

test('jogo do número: pontos pela proximidade', () => {
  assert.deepEqual(scoreNumberGuess(25, PIKACHU), { points: 100, correct: true, difference: 0, hint: 'Na mosca!' })
  assert.equal(scoreNumberGuess(28, PIKACHU).points, 94)
  assert.equal(scoreNumberGuess('20', PIKACHU).points, 90)
  assert.equal(scoreNumberGuess(75, PIKACHU).points, 0)
  assert.equal(scoreNumberGuess(151, PIKACHU).points, 0)
  assert.equal(proximityHint(4), 'Muito perto!')
  assert.equal(proximityHint(12), 'Perto!')
  assert.equal(proximityHint(30), 'Longe…')
  assert.equal(proximityHint(90), 'Muito longe!')
})

test('jogo do maior atributo, com empates', () => {
  assert.deepEqual(highestStats(PIKACHU), ['speed'])
  assert.deepEqual(highestStats(BULBASAUR), ['specialAttack', 'specialDefense'])
  assert.equal(scoreStatGuess('speed', PIKACHU).points, 100)
  assert.equal(scoreStatGuess('attack', PIKACHU).points, 0)
  assert.equal(scoreStatGuess('specialDefense', BULBASAUR).correct, true)
  assert.equal(highestStats(MEW).length, 6)
})

test('maior número da Pokédex considera cadastros novos', () => {
  assert.equal(maxDexNumber(POOL), 151)
  assert.equal(maxDexNumber([...POOL, { id: 160 }]), 160)
})
