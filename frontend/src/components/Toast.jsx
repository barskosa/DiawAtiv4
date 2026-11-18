import { useEffect } from 'react'
import Icon from './Icon.jsx'

// Aviso temporário confirmando cadastro, edição ou exclusão.
export default function Toast({ message, onDismiss }) {
  useEffect(() => {
    if (!message) return undefined
    const timer = setTimeout(onDismiss, 4000)
    return () => clearTimeout(timer)
  }, [message, onDismiss])

  return (
    <div className="toast-region" role="status" aria-live="polite">
      {message && (
        <div className="toast">
          <span>{message}</span>
          <button type="button" className="icon-button" aria-label="Fechar aviso" onClick={onDismiss}><Icon name="x" size={16} /></button>
        </div>
      )}
    </div>
  )
}
