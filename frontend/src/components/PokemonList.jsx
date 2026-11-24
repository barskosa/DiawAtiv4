import { useEffect, useRef, useState } from 'react'
import PokemonCard from './PokemonCard.jsx'
import Icon from './Icon.jsx'

export default function PokemonList({ pokemon, favorites, onSelect, onToggleFavorite, isFavoritesPage, hasFilters, onReset, onExplore }) {
  if (!pokemon.length) {
    return (
      <section className="empty-state" aria-label="Nenhum Pokémon encontrado">
        <span className="empty-icon"><Icon name={isFavoritesPage && !hasFilters ? 'star' : 'search'} size={38} /></span>
        <h2>{isFavoritesPage && !hasFilters ? 'Nenhum favorito' : 'Nenhum Pokémon encontrado'}</h2>
        <p>{isFavoritesPage && !hasFilters ? 'Marque a estrela de um Pokémon para adicioná-lo.' : 'Altere a pesquisa ou os tipos selecionados.'}</p>
        <button className="button button-primary" type="button" onClick={hasFilters ? onReset : onExplore}>{hasFilters ? 'Limpar filtros' : 'Ver Pokémon'}<Icon name="arrow-right" size={16} /></button>
      </section>
    )
  }

  return <PokemonGrid pokemon={pokemon} favorites={favorites} onSelect={onSelect} onToggleFavorite={onToggleFavorite} />
}

// Com 1025 Pokémon, os cartões são exibidos em lotes: o próximo lote entra quando o fim da lista
// aparece na tela (ou pelo botão, se o navegador não tiver IntersectionObserver).
const PAGE_SIZE = 60

function PokemonGrid({ pokemon, favorites, onSelect, onToggleFavorite }) {
  const [visible, setVisible] = useState(PAGE_SIZE)
  const sentinel = useRef(null)
  const hasMore = visible < pokemon.length
  const favoriteIds = new Set(favorites)

  useEffect(() => {
    if (!hasMore || !sentinel.current || typeof IntersectionObserver === 'undefined') return undefined
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) setVisible((count) => count + PAGE_SIZE)
    }, { rootMargin: '600px' })
    observer.observe(sentinel.current)
    return () => observer.disconnect()
  }, [hasMore, visible])

  return (
    <>
      <div className="pokemon-grid">
        {pokemon.slice(0, visible).map((item) => <PokemonCard key={item.id} pokemon={item} isFavorite={favoriteIds.has(item.id)} onSelect={onSelect} onToggleFavorite={onToggleFavorite} />)}
      </div>
      {hasMore && (
        <div className="load-more" ref={sentinel}>
          <button type="button" className="button button-secondary" onClick={() => setVisible((count) => count + PAGE_SIZE)}>
            Mostrar mais ({pokemon.length - visible} restantes)
          </button>
        </div>
      )}
    </>
  )
}
