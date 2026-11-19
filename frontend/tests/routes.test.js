import { test } from 'node:test'
import assert from 'node:assert/strict'
import { documentTitle, hashForPage, pageFromHash } from '../src/routes.js'

test('cada página tem um endereço e o endereço volta para a mesma página', () => {
  for (const page of ['pokedex', 'favorites', 'trainers', 'battles', 'minigames']) {
    assert.equal(pageFromHash(hashForPage(page)), page)
  }
  assert.equal(hashForPage('battles'), '#/batalhas')
  assert.equal(hashForPage('pokedex'), '#/')
})

test('sem hash ou rota desconhecida abre a Pokédex', () => {
  assert.equal(pageFromHash(''), 'pokedex')
  assert.equal(pageFromHash('#'), 'pokedex')
  assert.equal(pageFromHash('#/nao-existe'), 'pokedex')
  assert.equal(pageFromHash('#/Batalhas/'), 'battles', 'ignora maiúsculas e barra final')
})

test('âncoras internas não trocam de página', () => {
  assert.equal(pageFromHash('#conteudo'), null)
})

test('título da aba por página', () => {
  assert.equal(documentTitle('pokedex'), 'Pokédex — Explore Kanto')
  assert.equal(documentTitle('minigames'), 'Minigames · Pokédex')
})
