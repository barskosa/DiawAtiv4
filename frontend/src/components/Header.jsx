import Icon from './Icon.jsx'

const navigation = [
  { id: 'pokedex', label: 'Pokédex' },
  { id: 'favorites', label: 'Favoritos' },
  { id: 'trainers', label: 'Treinadores' },
  { id: 'battles', label: 'Batalhas' },
  { id: 'minigames', label: 'Minigames' },
]

export default function Header({ activePage, onNavigate, favoriteCount }) {
  return (
    <header className="site-header">
      <div className="site-header-inner">
        <a className="brand" href="#/" onClick={(event) => { event.preventDefault(); onNavigate('pokedex') }}>
          <Icon name="pokeball" size={26} />
          <span>Pokédex</span>
        </a>
        <nav className="navigation" aria-label="Navegação principal">
          {navigation.map((item) => (
            <button key={item.id} type="button" className={`nav-item ${activePage === item.id ? 'is-active' : ''}`} aria-current={activePage === item.id ? 'page' : undefined} onClick={() => onNavigate(item.id)}>
              {item.label}
              {item.id === 'favorites' && favoriteCount > 0 && <span className="nav-count"> ({favoriteCount})</span>}
            </button>
          ))}
        </nav>
      </div>
    </header>
  )
}
