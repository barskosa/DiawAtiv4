import { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import { filterPokemon, formatId, STAT_FIELDS } from './data/pokedex.js'
import { usePokedex } from './hooks/usePokedex.js'
import { documentTitle, hashForPage, pageFromHash } from './routes.js'
import Header from './components/Header.jsx'
import SearchBar from './components/SearchBar.jsx'
import PokemonList from './components/PokemonList.jsx'
import PokemonDetails from './components/PokemonDetails.jsx'
import Trainers from './components/Trainers.jsx'
import Icon from './components/Icon.jsx'
import StatusPanel from './components/StatusPanel.jsx'
import PokemonForm from './components/PokemonForm.jsx'
import Toast from './components/Toast.jsx'
import ConfirmDelete from './components/ConfirmDelete.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'

// Páginas extras carregadas só quando abertas: o carregamento inicial da Pokédex fica menor.
const Battles = lazy(() => import('./components/Battles.jsx'))
const Minigames = lazy(() => import('./components/Minigames.jsx'))

const FAVORITES_KEY = 'pokedex-kanto-favorites'

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
  const [activePage, setActivePage] = useState(() => pageFromHash(window.location.hash) ?? 'pokedex')
  const [query, setQuery] = useState('')
  const [selectedTypes, setSelectedTypes] = useState([])
  const [generation, setGeneration] = useState(null)
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
  const hasFilters = query.trim() !== '' || selectedTypes.length > 0 || generation !== null
  const filteredPokemon = filterPokemon(pokemon, { query, selectedTypes, sort, favorites, favoritesOnly: isFavoritesPage, generation })

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
    setGeneration(null)
  }

  function toggleType(name) {
    setSelectedTypes((current) => {
      if (current.includes(name)) return current.filter((item) => item !== name)
      return current.length < 2 ? [...current, name] : current
    })
  }

  function showPage(page) {
    setActivePage(page)
    setQuery('')
    setSelectedTypes([])
    setGeneration(null)
    window.scrollTo({ top: 0, behavior: 'instant' })
  }

  // A URL é a fonte da página atual: voltar/avançar do navegador e links diretos funcionam.
  useEffect(() => {
    function handleHashChange() {
      const page = pageFromHash(window.location.hash)
      if (page) showPage(page)
    }
    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [])

  useEffect(() => {
    document.title = documentTitle(activePage)
  }, [activePage])

  function navigate(page) {
    const hash = hashForPage(page)
    // Mesma página: só volta ao topo e limpa os filtros. Outra página: muda a URL (o hashchange troca a tela).
    if (window.location.hash === hash) showPage(page)
    else window.location.hash = hash
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
      <Header activePage={activePage} onNavigate={navigate} favoriteCount={favorites.length} />
      <div className="workspace">
        <main className="main-content" id="conteudo" tabIndex={-1}>
          <ErrorBoundary key={activePage}>
            <Suspense fallback={<StatusPanel status="loading" />}>
              {!isReady ? <StatusPanel status={status} error={error} onRetry={reload} /> : activePage === 'trainers' ? <Trainers trainers={trainers} pokemon={pokemon} onSelectPokemon={selectPokemon} onChallenge={challengeTrainer} /> : activePage === 'battles' ? <Battles trainers={trainers} pokemon={pokemon} initialTrainerId={challengeTrainerId} /> : activePage === 'minigames' ? <Minigames pokemon={pokemon} types={types} /> : <>
                <div className="page-heading">
                  <h1>{isFavoritesPage ? 'Favoritos' : 'Pokédex'}</h1>
                  <p>{isFavoritesPage ? 'Pokémon marcados com a estrela.' : `${pokemon.length} Pokémon de 9 gerações, de Kanto a Paldea.`}</p>
                </div>
                <SearchBar types={types} query={query} onQueryChange={setQuery} generation={generation} onGenerationChange={setGeneration} selectedTypes={selectedTypes} onToggleType={toggleType} onClearTypes={() => setSelectedTypes([])} hasFilters={hasFilters} onReset={resetFilters} />
                <div className="results-toolbar">
                  <p className="results-count" role="status" aria-live="polite" aria-atomic="true">{filteredPokemon.length} {filteredPokemon.length === 1 ? 'Pokémon encontrado' : 'Pokémon encontrados'}</p>
                  <div className="results-actions">
                    <button type="button" className="button button-primary new-pokemon-button" onClick={openCreateForm}><Icon name="plus" size={16} /><span>Novo Pokémon</span></button>
                    <button type="button" className="button button-secondary" onClick={chooseRandomPokemon}>Aleatório</button>
                    <label className="sort-label" htmlFor="pokemon-sort">Ordenar por</label>
                    <select id="pokemon-sort" className="sort-select" value={sort} onChange={(event) => setSort(event.target.value)}>
                      <option value="id">Nº da Pokédex</option><option value="name">Nome (A–Z)</option>
                      {STAT_FIELDS.map(({ key, label }) => <option key={key} value={key}>{label} (maior primeiro)</option>)}
                    </select>
                  </div>
                </div>
                <PokemonList key={`${activePage}-${query}-${selectedTypes.join()}-${generation}-${sort}`} pokemon={filteredPokemon} favorites={favorites} onSelect={selectPokemon} onToggleFavorite={toggleFavorite} isFavoritesPage={isFavoritesPage} hasFilters={hasFilters} onReset={resetFilters} onExplore={() => navigate('pokedex')} />
              </>}
            </Suspense>
          </ErrorBoundary>
        </main>
      </div>
      {selectedPokemon && <PokemonDetails pokemon={selectedPokemon} allPokemon={pokemon} trainers={trainers} onClose={() => selectPokemon(null)} onSelect={selectPokemon} favorites={favorites} onToggleFavorite={toggleFavorite} onEdit={openEditForm} onDelete={setDeleteTarget} />}
      {form && <PokemonForm pokemon={form.pokemon} types={types} allPokemon={pokemon} onSubmit={savePokemon} onClose={() => setForm(null)} />}
      {deleteTarget && <ConfirmDelete pokemon={deleteTarget} trainers={trainers} onConfirm={confirmDelete} onCancel={() => setDeleteTarget(null)} />}
      <Toast message={toast} onDismiss={dismissToast} />
    </div>
  )
}
