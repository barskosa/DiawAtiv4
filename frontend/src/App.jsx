import { useCallback, useEffect, useState } from 'react'
import { filterPokemon, formatId, STAT_FIELDS } from './data/pokedex.js'
import { usePokedex } from './hooks/usePokedex.js'
import Header from './components/Header.jsx'
import SearchBar from './components/SearchBar.jsx'
import PokemonList from './components/PokemonList.jsx'
import PokemonDetails from './components/PokemonDetails.jsx'
import Trainers from './components/Trainers.jsx'
import PokemonImage from './components/PokemonImage.jsx'
import Icon from './components/Icon.jsx'
import StatusPanel from './components/StatusPanel.jsx'
import PokemonForm from './components/PokemonForm.jsx'
import Toast from './components/Toast.jsx'
import ConfirmDelete from './components/ConfirmDelete.jsx'
import Battles from './components/Battles.jsx'
import Minigames from './components/Minigames.jsx'

const FAVORITES_KEY = 'pokedex-kanto-favorites'

// Ilustrações fixas do destaque: não dependem da lista carregada da API.
const HERO_POKEMON = { bulbasaur: { id: 1, name: 'Bulbasaur' }, squirtle: { id: 7, name: 'Squirtle' }, charmander: { id: 4, name: 'Charmander' } }

function readFavorites() {
  try {
    const saved = JSON.parse(localStorage.getItem(FAVORITES_KEY) || '[]')
    return Array.isArray(saved) ? [...new Set(saved.filter(Number.isInteger))] : []
  } catch {
    return []
  }
}

export default function App() {
  const { status, error, pokemon, types, trainers, reload, createPokemon, updatePokemon, deletePokemon } = usePokedex()
  const [activePage, setActivePage] = useState('pokedex')
  const [query, setQuery] = useState('')
  const [selectedTypes, setSelectedTypes] = useState([])
  const [sort, setSort] = useState('id')
  const [selectedId, setSelectedId] = useState(null)
  const [savedFavorites, setFavorites] = useState(readFavorites)
  const [form, setForm] = useState(null) // { pokemon: null } para cadastrar, { pokemon } para editar
  const [toast, setToast] = useState('')
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [challengeTrainerId, setChallengeTrainerId] = useState(null)
  const dismissToast = useCallback(() => setToast(''), [])
  const isReady = status === 'ready'
  // Favoritos de Pokémon que não existem mais na API não são exibidos nem contados.
  const favorites = isReady ? savedFavorites.filter((id) => pokemon.some((item) => item.id === id)) : savedFavorites
  const selectedPokemon = pokemon.find((item) => item.id === selectedId) ?? null
  const isFavoritesPage = activePage === 'favorites'
  const hasFilters = query.trim() !== '' || selectedTypes.length > 0
  const filteredPokemon = filterPokemon(pokemon, { query, selectedTypes, sort, favorites, favoritesOnly: isFavoritesPage })

  useEffect(() => {
    try {
      localStorage.setItem(FAVORITES_KEY, JSON.stringify(savedFavorites))
    } catch {
      // A coleção continua disponível nesta sessão quando o navegador bloqueia armazenamento.
    }
  }, [savedFavorites])

  function toggleFavorite(id) {
    setFavorites((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])
  }

  function resetFilters() {
    setQuery('')
    setSelectedTypes([])
  }

  function toggleType(name) {
    setSelectedTypes((current) => {
      if (current.includes(name)) return current.filter((item) => item !== name)
      return current.length < 2 ? [...current, name] : current
    })
  }

  function navigate(page) {
    setActivePage(page)
    resetFilters()
    window.scrollTo({ top: 0, behavior: 'instant' })
  }

  function selectPokemon(entry) {
    setSelectedId(entry ? entry.id : null)
  }

  function openCreateForm() {
    setForm({ pokemon: null })
  }

  function openEditForm(entry) {
    setSelectedId(null)
    setForm({ pokemon: entry })
  }

  async function savePokemon(values) {
    const editing = form.pokemon
    const saved = editing ? await updatePokemon(editing.id, values) : await createPokemon(values)
    setForm(null)
    setSelectedId(saved.id)
    setToast(editing ? `${saved.name} foi atualizado.` : `${saved.name} foi cadastrado como ${formatId(saved.id)}.`)
  }

  async function confirmDelete() {
    const target = deleteTarget
    await deletePokemon(target.id)
    setDeleteTarget(null)
    setSelectedId(null)
    setFavorites((current) => current.filter((id) => id !== target.id))
    setToast(`${target.name} foi excluído.`)
  }

  function challengeTrainer(trainerId) {
    setChallengeTrainerId(trainerId)
    navigate('battles')
  }

  function chooseRandomPokemon() {
    const collection = filteredPokemon.length ? filteredPokemon : pokemon
    if (collection.length) selectPokemon(collection[Math.floor(Math.random() * collection.length)])
  }

  return (
    <div className="app-layout">
      <a className="skip-link" href="#conteudo">Pular para o conteúdo</a>
      <Header activePage={activePage} onNavigate={navigate} favoriteCount={favorites.length} pokemonCount={isReady ? pokemon.length : null} />
      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb"><Icon name="globe" size={17} /><span>Pokémon</span><Icon name="chevron-right" size={13} /><strong>{activePage === 'trainers' ? 'Treinadores' : activePage === 'battles' ? 'Batalhas' : activePage === 'minigames' ? 'Minigames' : isFavoritesPage ? 'Favoritos' : 'Pokédex'}</strong></div>
          <span className="generation-label"><span className="status-dot" /> 1ª geração <span className="topbar-divider" /> Kanto</span>
        </header>
        <main className="main-content" id="conteudo" tabIndex={-1}>
          {!isReady ? <StatusPanel status={status} error={error} onRetry={reload} /> : activePage === 'trainers' ? <Trainers trainers={trainers} pokemon={pokemon} onSelectPokemon={selectPokemon} onChallenge={challengeTrainer} /> : activePage === 'battles' ? <Battles trainers={trainers} pokemon={pokemon} initialTrainerId={challengeTrainerId} /> : activePage === 'minigames' ? <Minigames pokemon={pokemon} types={types} /> : <>
            <section className="hero">
              <div className="hero-copy">
                <h1>{isFavoritesPage ? 'Favoritos' : <>Kanto.<br />1ª <em>Geração.</em></>}</h1>
                <div className="hero-meta"><span><Icon name="pokeball" size={16} /><strong>{isFavoritesPage ? favorites.length : pokemon.length}</strong> Pokémon</span></div>
              </div>
              <div className="hero-art" aria-hidden="true">
                <span className="hero-art-ring ring-one" /><span className="hero-art-ring ring-two" />
                <PokemonImage pokemon={HERO_POKEMON.bulbasaur} className="hero-pokemon hero-bulbasaur" loading="eager" alt="" />
                <PokemonImage pokemon={HERO_POKEMON.squirtle} className="hero-pokemon hero-squirtle" loading="eager" alt="" />
                <PokemonImage pokemon={HERO_POKEMON.charmander} className="hero-pokemon hero-charmander" loading="eager" alt="" />
              </div>
            </section>
            <SearchBar types={types} query={query} onQueryChange={setQuery} selectedTypes={selectedTypes} onToggleType={toggleType} onClearTypes={() => setSelectedTypes([])} hasFilters={hasFilters} onReset={resetFilters} />
            <div className="results-toolbar">
              <div className="results-title"><h2>{isFavoritesPage ? 'Favoritos' : 'Pokémon'}</h2><span className="results-count" role="status" aria-live="polite" aria-atomic="true">{filteredPokemon.length} {filteredPokemon.length === 1 ? 'encontrado' : 'encontrados'}</span></div>
              <div className="results-actions">
                <button type="button" className="button button-primary new-pokemon-button" onClick={openCreateForm}><Icon name="plus" size={16} /><span>Novo Pokémon</span></button>
                <span className="toolbar-divider" />
                <button type="button" className="random-button" onClick={chooseRandomPokemon} title="Selecionar Pokémon aleatório"><Icon name="shuffle" size={16} /><span>Aleatório</span></button>
                <span className="toolbar-divider" />
                <label className="sort-label" htmlFor="pokemon-sort">Ordenar por:</label>
                <select id="pokemon-sort" className="sort-select" value={sort} onChange={(event) => setSort(event.target.value)}>
                  <option value="id">Nº da Pokédex</option><option value="name">Nome (A–Z)</option>
                  {STAT_FIELDS.map(({ key, label }) => <option key={key} value={key}>{label} (maior primeiro)</option>)}
                </select>
              </div>
            </div>
            <PokemonList pokemon={filteredPokemon} favorites={favorites} onSelect={selectPokemon} onToggleFavorite={toggleFavorite} isFavoritesPage={isFavoritesPage} hasFilters={hasFilters} onReset={resetFilters} onExplore={() => navigate('pokedex')} />
          </>}
        </main>
      </div>
      {selectedPokemon && <PokemonDetails pokemon={selectedPokemon} allPokemon={pokemon} trainers={trainers} onClose={() => selectPokemon(null)} onSelect={selectPokemon} favorites={favorites} onToggleFavorite={toggleFavorite} onEdit={openEditForm} onDelete={setDeleteTarget} />}
      {form && <PokemonForm pokemon={form.pokemon} types={types} allPokemon={pokemon} onSubmit={savePokemon} onClose={() => setForm(null)} />}
      {deleteTarget && <ConfirmDelete pokemon={deleteTarget} trainers={trainers} onConfirm={confirmDelete} onCancel={() => setDeleteTarget(null)} />}
      <Toast message={toast} onDismiss={dismissToast} />
    </div>
  )
}
