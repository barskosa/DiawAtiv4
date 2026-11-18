import { afterEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { api, ApiError } from '../src/services/api.js'

const originalFetch = globalThis.fetch
const calls = []

function mockFetch(status, body, contentType = 'application/json') {
  globalThis.fetch = async (url, options) => {
    calls.push({ url, ...options })
    const text = body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body)
    return new Response(status === 204 ? null : text, { status, headers: { 'Content-Type': contentType } })
  }
}

afterEach(() => {
  globalThis.fetch = originalFetch
  calls.length = 0
})

test('GET /api/pokemons devolve a lista', async () => {
  mockFetch(200, [{ id: 25, name: 'Pikachu' }])
  assert.deepEqual(await api.listPokemons(), [{ id: 25, name: 'Pikachu' }])
  assert.equal(calls[0].url, '/api/pokemons')
  assert.equal(calls[0].method, 'GET')
})

test('POST envia JSON com Content-Type', async () => {
  mockFetch(201, { id: 152, name: 'Voltix' })
  await api.createPokemon({ name: 'Voltix' })
  assert.equal(calls[0].method, 'POST')
  assert.equal(calls[0].headers['Content-Type'], 'application/json')
  assert.equal(calls[0].body, '{"name":"Voltix"}')
})

test('PUT e DELETE usam o id na URL; 204 devolve null', async () => {
  mockFetch(200, { id: 25 })
  await api.updatePokemon(25, { name: 'Pikachu' })
  assert.equal(calls[0].url, '/api/pokemons/25')
  assert.equal(calls[0].method, 'PUT')

  mockFetch(204)
  assert.equal(await api.deletePokemon(25), null)
  assert.equal(calls[1].method, 'DELETE')
})

test('erro da API vira ApiError com detail e erros por campo', async () => {
  mockFetch(409, { status: 409, title: 'Conflito', detail: 'O nome Pikachu já pertence ao Pokémon 25.', errors: { name: 'O nome Pikachu já pertence ao Pokémon 25.' } }, 'application/problem+json')
  await assert.rejects(api.createPokemon({}), (error) => {
    assert.ok(error instanceof ApiError)
    assert.equal(error.status, 409)
    assert.equal(error.message, 'O nome Pikachu já pertence ao Pokémon 25.')
    assert.deepEqual(error.fieldErrors, { name: 'O nome Pikachu já pertence ao Pokémon 25.' })
    return true
  })
})

test('backend desligado: mensagem de conexão (falha de rede ou 5xx do proxy)', async () => {
  globalThis.fetch = async () => { throw new TypeError('fetch failed') }
  await assert.rejects(api.listPokemons(), /Não foi possível conectar à API/)

  mockFetch(502, '')
  await assert.rejects(api.listPokemons(), /Não foi possível conectar à API/)
})

test('resposta que não é JSON (endereço errado) gera erro claro', async () => {
  mockFetch(200, '<!doctype html><html></html>', 'text/html')
  await assert.rejects(api.listPokemons(), /formato inesperado/)
})
