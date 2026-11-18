import Icon from './Icon.jsx'

// Estado exibido enquanto os dados chegam da API ou quando ela não responde.
export default function StatusPanel({ status, error, onRetry }) {
  if (status === 'loading') {
    return (
      <section className="status-panel" role="status" aria-live="polite">
        <span className="status-spinner" aria-hidden="true"><Icon name="pokeball" size={42} /></span>
        <h2>Carregando a Pokédex…</h2>
        <p>Buscando Pokémon, tipos e treinadores na API.</p>
      </section>
    )
  }

  return (
    <section className="status-panel is-error" role="alert">
      <span className="empty-icon"><Icon name="x" size={34} /></span>
      <h2>Não foi possível carregar a Pokédex</h2>
      <p>{error?.message || 'Erro inesperado ao consultar a API.'}</p>
      <p className="status-hint">Confira se o backend Spring Boot está em execução em <code>http://localhost:8080</code> e se o MongoDB está acessível.</p>
      <button className="button button-primary" type="button" onClick={onRetry}>Tentar novamente <Icon name="arrow-right" size={16} /></button>
    </section>
  )
}
