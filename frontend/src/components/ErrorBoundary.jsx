import { Component } from 'react'
import Icon from './Icon.jsx'

/**
 * Se uma página quebrar ao renderizar (ex.: dado inesperado da API), mostra um aviso no lugar dela
 * em vez de deixar a tela em branco. O menu continua funcionando para ir a outra página.
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    console.error('Erro ao exibir a página:', error, info.componentStack)
  }

  render() {
    if (!this.state.error) return this.props.children
    return (
      <section className="status-panel is-error" role="alert">
        <span className="empty-icon"><Icon name="x" size={34} /></span>
        <h2>Algo deu errado nesta página</h2>
        <p>{this.state.error.message || 'Erro inesperado.'}</p>
        <p className="status-hint">Tente de novo ou use o menu para ir a outra página.</p>
        <div className="status-actions">
          <button className="button button-primary" type="button" onClick={() => this.setState({ error: null })}>Tentar novamente</button>
          <button className="button button-secondary" type="button" onClick={() => window.location.reload()}>Recarregar a página</button>
        </div>
      </section>
    )
  }
}
