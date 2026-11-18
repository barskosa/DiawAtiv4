// Cliente da API REST da Pokédex (Spring Boot + MongoDB).
// Em desenvolvimento, o Vite encaminha /api para o backend (veja vite.config.js).
// Para apontar para outro endereço, defina VITE_API_URL (ex.: http://localhost:8080/api).
const API_URL = (import.meta.env?.VITE_API_URL || '/api').replace(/\/+$/, '')

const CONNECTION_MESSAGE = 'Não foi possível conectar à API. Verifique se o backend está em execução.'

export class ApiError extends Error {
  constructor(message, status = 0, fieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function request(path, { method = 'GET', body, signal } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError(CONNECTION_MESSAGE)
  }

  if (response.status === 204) return null

  const text = await response.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = null
  }

  if (!response.ok) {
    // O proxy do Vite responde 5xx sem corpo JSON quando o backend está desligado.
    if (!data && response.status >= 500) throw new ApiError(CONNECTION_MESSAGE, response.status)
    throw new ApiError(data?.detail || data?.title || `Erro ${response.status} na API.`, response.status, data?.errors || {})
  }
  if (data === null && text) {
    throw new ApiError('A API respondeu em um formato inesperado. Confira o endereço configurado em VITE_API_URL.', response.status)
  }
  return data
}

export const api = {
  listPokemons: (signal) => request('/pokemons', { signal }),
  getPokemon: (id, signal) => request(`/pokemons/${id}`, { signal }),
  createPokemon: (pokemon) => request('/pokemons', { method: 'POST', body: pokemon }),
  updatePokemon: (id, pokemon) => request(`/pokemons/${id}`, { method: 'PUT', body: pokemon }),
  deletePokemon: (id) => request(`/pokemons/${id}`, { method: 'DELETE' }),
  listTypes: (signal) => request('/types', { signal }),
  listTrainers: (signal) => request('/trainers', { signal }),
}
