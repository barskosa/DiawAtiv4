import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  filterPokemon,
  generationOf,
  toFormValues,
  toPokemonPayload,
  toPokemonView,
  validatePokemonForm,
} from '../src/data/pokedex.js'

const types = [
  { id: 0, name: 'Typeless' },
  { id: 2, name: 'Fire' },
  { id: 3, name: 'Water' },
  { id: 4, name: 'Grass' },
  { id: 5, name: 'Electric' },
  { id: 8, name: 'Poison' },
  { id: 10, name: 'Flying' },
]

// Documentos no formato devolvido por GET /api/pokemons.
const documents = [
  { id: 1, name: 'Bulbasaur', hp: 45, attack: 49, defense: 49, specialAttack: 65, specialDefense: 65, speed: 45, types: ['Grass', 'Poison'] },
  { id: 6, name: 'Charizard', hp: 78, attack: 84, defense: 78, specialAttack: 109, specialDefense: 85, speed: 100, types: ['Fire', 'Flying'] },
  { id: 25, name: 'Pikachu', hp: 35, attack: 55, defense: 30, specialAttack: 50, specialDefense: 40, speed: 90, types: ['Electric'] },
  { id: 83, name: "Farfetch'd", hp: 52, attack: 65, defense: 55, specialAttack: 58, specialDefense: 62, speed: 60, types: ['Flying'] },
  { id: 151, name: 'Mew', hp: 100, attack: 100, defense: 100, specialAttack: 100, specialDefense: 100, speed: 100, types: [] },
]
const pokemon = documents.map((document) => toPokemonView(document, types))
const names = (list) => list.map((entry) => entry.name)

test('converte o documento da API para o formato da interface', () => {
  assert.deepEqual(pokemon[2], {
    id: 25,
    name: 'Pikachu',
    stats: { hp: 35, attack: 55, defense: 30, specialAttack: 50, specialDefense: 40, speed: 90 },
    types: [{ id: 5, name: 'Electric' }],
  })
  assert.deepEqual(pokemon[4].types, [])
})

test('converte o formulário para o corpo da API (sem id no cadastro automático)', () => {
  const values = { id: '', name: '  Voltix   Turbo ', stats: { hp: '70', attack: '80', defense: '50', specialAttack: '50', specialDefense: '50', speed: '130' }, types: ['Electric', 'Steel'] }
  assert.deepEqual(toPokemonPayload(values), {
    name: 'Voltix Turbo', hp: 70, attack: 80, defense: 50, specialAttack: 50, specialDefense: 50, speed: 130, types: ['Electric', 'Steel'],
  })
  assert.equal(toPokemonPayload({ ...values, id: '152' }).id, 152)
})

test('ida e volta: interface → formulário → API preserva o documento', () => {
  const { id, ...rest } = documents[1]
  assert.deepEqual(toPokemonPayload(toFormValues(pokemon[1])), { id, ...rest })
})

test('pesquisa por nome, ignorando maiúsculas e acentos', () => {
  assert.deepEqual(names(filterPokemon(pokemon, { query: 'CHAR' })), ['Charizard'])
  assert.deepEqual(names(filterPokemon(pokemon, { query: "farfetch'" })), ["Farfetch'd"])
})

test('pesquisa por número: 25, 025 e #025', () => {
  for (const query of ['25', '025', '#025']) {
    assert.deepEqual(names(filterPokemon(pokemon, { query })), ['Pikachu'])
  }
})

test('filtro por tipos exige todos os selecionados', () => {
  assert.deepEqual(names(filterPokemon(pokemon, { selectedTypes: ['Grass', 'Poison'] })), ['Bulbasaur'])
  assert.deepEqual(names(filterPokemon(pokemon, { selectedTypes: ['Flying'] })), ['Charizard', "Farfetch'd"])
  assert.deepEqual(names(filterPokemon(pokemon, { selectedTypes: ['Fire', 'Water'] })), [])
  assert.deepEqual(names(filterPokemon(pokemon, { selectedTypes: ['Typeless'] })), ['Mew'])
})

test('ordena por nome e por atributo (maior primeiro)', () => {
  assert.deepEqual(names(filterPokemon(pokemon, { sort: 'name' })), ['Bulbasaur', 'Charizard', "Farfetch'd", 'Mew', 'Pikachu'])
  assert.equal(filterPokemon(pokemon, { sort: 'speed' })[0].name, 'Charizard')
  assert.equal(filterPokemon(pokemon, { sort: 'hp' })[0].name, 'Mew')
})

test('favoritos e imutabilidade da lista original', () => {
  const before = JSON.stringify(pokemon)
  assert.deepEqual(names(filterPokemon(pokemon, { favoritesOnly: true, favorites: [151, 1] })), ['Bulbasaur', 'Mew'])
  filterPokemon(pokemon, { sort: 'name' })
  assert.equal(JSON.stringify(pokemon), before)
})

test('validação aceita um cadastro correto', () => {
  const values = { ...toFormValues(null), name: 'Voltix', types: ['Electric'] }
  assert.deepEqual(validatePokemonForm(values, { existing: pokemon }), {})
})

test('validação aponta campos obrigatórios e limites de 1 a 255', () => {
  const values = toFormValues(null)
  values.stats.hp = '0'
  values.stats.attack = '256'
  values.stats.speed = ''
  values.stats.defense = '4.5'
  const errors = validatePokemonForm(values, { existing: pokemon })
  assert.deepEqual(Object.keys(errors).sort(), ['attack', 'defense', 'hp', 'name', 'speed'])
})

test('validação impede nome e número repetidos', () => {
  const values = { ...toFormValues(null), id: '25', name: 'PIKACHU' }
  const errors = validatePokemonForm(values, { existing: pokemon })
  assert.match(errors.id, /número 25/)
  assert.match(errors.name, /#025/)
})

test('na edição o próprio nome não conta como repetido', () => {
  const values = toFormValues(pokemon[2])
  assert.deepEqual(validatePokemonForm(values, { isEditing: true, existing: pokemon }), {})
})

test('no máximo dois tipos', () => {
  const values = { ...toFormValues(null), name: 'Teste', types: ['Fire', 'Water', 'Grass'] }
  assert.ok(validatePokemonForm(values, { existing: pokemon }).types)
})

test('geração pelo número da Pokédex nacional', () => {
  assert.equal(generationOf(1), 1)
  assert.equal(generationOf(151), 1)
  assert.equal(generationOf(152), 2)
  assert.equal(generationOf(700), 6)
  assert.equal(generationOf(1025), 9)
  assert.equal(generationOf(1026), null)
})

test('filtro por geração', () => {
  const chikorita = toPokemonView({ id: 152, name: 'Chikorita', hp: 45, attack: 49, defense: 65, specialAttack: 49, specialDefense: 65, speed: 45, types: ['Grass'] }, types)
  const list = [...pokemon, chikorita]
  assert.deepEqual(names(filterPokemon(list, { generation: 2 })), ['Chikorita'])
  assert.equal(filterPokemon(list, { generation: 1 }).length, pokemon.length)
  assert.equal(filterPokemon(list, { generation: null }).length, list.length)
  assert.deepEqual(names(filterPokemon(list, { generation: 1, selectedTypes: ['Grass'] })), ['Bulbasaur'])
})
