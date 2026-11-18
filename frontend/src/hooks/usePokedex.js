import { useCallback, useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { sortById, toPokemonPayload, toPokemonView } from '../data/pokedex.js'

const initialState = { status: 'loading', error: null, pokemon: [], types: [], trainers: [] }

/**
 * Carrega Pokémon, tipos e treinadores da API e expõe as operações de cadastro, edição e exclusão.
 * O estado local só é alterado depois que a API confirma a operação no MongoDB.
 */
export function usePokedex() {
  const [state, setState] = useState(initialState)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setState((current) => ({ ...current, status: 'loading', error: null }))

    Promise.all([
      api.listTypes(controller.signal),
      api.listPokemons(controller.signal),
      api.listTrainers(controller.signal),
    ])
      .then(([types, pokemon, trainers]) => {
        setState({
          status: 'ready',
          error: null,
          types: sortById(types),
          pokemon: sortById(pokemon.map((document) => toPokemonView(document, types))),
          trainers,
        })
      })
      .catch((error) => {
        if (error.name === 'AbortError') return
        setState((current) => ({ ...current, status: 'error', error }))
      })

    return () => controller.abort()
  }, [reloadKey])

  const reload = useCallback(() => setReloadKey((key) => key + 1), [])

  async function createPokemon(entry) {
    const created = toPokemonView(await api.createPokemon(toPokemonPayload(entry)), state.types)
    setState((current) => ({ ...current, pokemon: sortById([...current.pokemon, created]) }))
    return created
  }

  async function updatePokemon(id, entry) {
    const updated = toPokemonView(await api.updatePokemon(id, toPokemonPayload({ ...entry, id })), state.types)
    setState((current) => ({
      ...current,
      pokemon: current.pokemon.map((item) => (item.id === id ? updated : item)),
    }))
    return updated
  }

  return { ...state, reload, createPokemon, updatePokemon }
}
