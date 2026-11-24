import { GENERATIONS, typeLabels } from '../data/pokedex.js'
import Icon from './Icon.jsx'

export default function SearchBar({ types, query, onQueryChange, generation, onGenerationChange, selectedTypes, onToggleType, onClearTypes, onReset, hasFilters }) {
  return (
    <section className="search-panel" aria-label="Pesquisa e filtros de Pokémon">
      <div className="search-row">
        <div className="search-input-wrap">
          <Icon name="search" size={21} />
          <label className="sr-only" htmlFor="pokemon-search">Pesquisar Pokémon por nome ou ID</label>
          <input id="pokemon-search" type="search" placeholder="Pesquisar por nome ou número" value={query} onChange={(event) => onQueryChange(event.target.value)} autoComplete="off" />
        </div>
      </div>
      <div className="filter-heading">
        <div className="filter-title"><span>Geração</span></div>
      </div>
      <div className="quick-filters generation-filters" role="group" aria-label="Filtrar por geração">
        <button type="button" className={`filter-chip ${generation === null ? 'is-selected' : ''}`} aria-pressed={generation === null} onClick={() => onGenerationChange(null)}>Todas</button>
        {GENERATIONS.map((item) => (
          <button key={item.id} type="button" className={`filter-chip ${generation === item.id ? 'is-selected' : ''}`} aria-pressed={generation === item.id} onClick={() => onGenerationChange(generation === item.id ? null : item.id)} title={`#${item.first} a #${item.last}`}>
            {item.label} <span className="generation-region">{item.region}</span>
          </button>
        ))}
      </div>
      <div className="filter-heading">
        <div className="filter-title"><Icon name="filter" size={16} /><span>Tipos</span><span className="filter-hint" id="type-filter-hint">{selectedTypes.length === 2 ? 'Pokémon com os dois tipos' : 'Selecione até 2'}</span></div>
        {hasFilters && <button type="button" className="clear-filters" onClick={onReset}><Icon name="x" size={13} /> Limpar filtros</button>}
      </div>
      <div className="quick-filters" role="group" aria-label="Filtrar por tipos" aria-describedby="type-filter-hint">
        <button type="button" className={`filter-chip ${selectedTypes.length === 0 ? 'is-selected' : ''}`} aria-pressed={selectedTypes.length === 0} onClick={onClearTypes}>Todos</button>
        {types.map(({ id, name }) => {
          const isSelected = selectedTypes.includes(name)
          const isDisabled = selectedTypes.length === 2 && !isSelected

          return (
            <button key={id} type="button" className={`filter-chip type-${name.toLowerCase()} ${isSelected ? 'is-selected' : ''}`} aria-pressed={isSelected} disabled={isDisabled} onClick={() => onToggleType(name)} title={isDisabled ? 'Desmarque um tipo para escolher outro' : name}>
              {typeLabels[name] || name}
            </button>
          )
        })}
      </div>
    </section>
  )
}
