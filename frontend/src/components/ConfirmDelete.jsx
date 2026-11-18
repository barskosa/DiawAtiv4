import { useEffect, useId, useRef, useState } from 'react'
import { formatId } from '../data/pokedex.js'
import Icon from './Icon.jsx'

// Confirmação antes de excluir um Pokémon (DELETE /api/pokemons/{id}).
export default function ConfirmDelete({ pokemon, trainers, onConfirm, onCancel }) {
  const dialogRef = useRef(null)
  const titleId = useId()
  const descriptionId = useId()
  const [isDeleting, setIsDeleting] = useState(false)
  const [error, setError] = useState('')
  const owners = trainers.filter((trainer) => trainer.pokemonIds.includes(pokemon.id)).map((trainer) => trainer.name)

  useEffect(() => {
    const dialog = dialogRef.current
    const previousFocus = document.activeElement
    if (!dialog.open) dialog.showModal()
    return () => {
      dialog.close()
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus({ preventScroll: true })
    }
  }, [])

  function cancel() {
    if (!isDeleting) onCancel()
  }

  async function confirm() {
    setIsDeleting(true)
    setError('')
    try {
      await onConfirm()
    } catch (failure) {
      setError(failure.message || 'Não foi possível excluir o Pokémon.')
      setIsDeleting(false)
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="pokemon-dialog confirm-dialog"
      role="alertdialog"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      onCancel={(event) => { event.preventDefault(); cancel() }}
    >
      <div className="confirm-body">
        <span className="confirm-icon" aria-hidden="true"><Icon name="trash" size={26} /></span>
        <h2 id={titleId}>Excluir {pokemon.name}?</h2>
        <p id={descriptionId}>
          {formatId(pokemon.id)} será removido do MongoDB e não poderá ser recuperado pela interface.
          {owners.length > 0 && <> Ele também sairá da equipe de <strong>{owners.join(', ')}</strong>.</>}
        </p>
        {error && <p className="form-alert" role="alert">{error}</p>}
      </div>
      <footer className="form-actions confirm-actions">
        <button type="button" className="button button-secondary" onClick={cancel} disabled={isDeleting} autoFocus>Cancelar</button>
        <button type="button" className="button button-primary button-delete" onClick={confirm} disabled={isDeleting}>
          {isDeleting ? 'Excluindo…' : 'Excluir Pokémon'}
        </button>
      </footer>
    </dialog>
  )
}
