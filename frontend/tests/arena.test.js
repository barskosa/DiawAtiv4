import { test } from 'node:test'
import assert from 'node:assert/strict'
import { actionLabel, battleHp, hpTone, playerLabel, specialMoveName } from '../src/components/arena/arenaRules.js'

test('HP de batalha igual ao calculado pela API (nível 50)', () => {
  assert.equal(battleHp(35), 95)   // Pikachu
  assert.equal(battleHp(78), 138)  // Charizard
  assert.equal(battleHp(250), 310) // Chansey
})

test('nome do golpe especial pelo primeiro tipo, igual ao backend', () => {
  assert.equal(specialMoveName(['Electric']), 'Golpe Elétrico')
  assert.equal(specialMoveName(['Fire', 'Flying']), 'Golpe Fogo')
  assert.equal(specialMoveName([]), 'Rajada Mística')
  assert.equal(actionLabel('SPECIAL', { types: ['Water'] }), 'Golpe Água')
  assert.equal(actionLabel('DEFEND', { types: [] }), 'Defender')
})

test('cor da barra de HP por faixa', () => {
  assert.equal(hpTone(100, 100), 'is-high')
  assert.equal(hpTone(40, 100), 'is-mid')
  assert.equal(hpTone(20, 100), 'is-low')
  assert.equal(hpTone(0, 100), 'is-low')
})

test('nome padrão do jogador', () => {
  assert.equal(playerLabel('  ', 2), 'Jogador 2')
  assert.equal(playerLabel('Ana', 1), 'Ana')
})
