import { useState } from 'react'
import { formatId } from '../../data/pokedex.js'
import Icon from '../Icon.jsx'
import { scoreNumberGuess } from './minigameLogic.js'

/** Adivinhar o número da Pokédex: quanto mais perto, mais pontos. */
export default function NumberGame({ pokemon, maxNumber, result, onAnswer }) {
  const [guess, setGuess] = useState('')
  const value = Number(guess)
  const valid = guess !== '' && Number.isInteger(value) && value >= 1 && value <= maxNumber

  function submit(event) {
    event.preventDefault()
    if (valid) onAnswer({ ...scoreNumberGuess(value, pokemon), guess: formatId(value) })
  }

  return (
    <form className="minigame-question" onSubmit={submit}>
      <p className="minigame-prompt">Qual é o número de <strong>{pokemon.name}</strong> na Pokédex? (1 a {maxNumber})</p>
      <div className="minigame-number">
        <label className="form-field">
          <span>Seu palpite</span>
          <input id="minigame-number-input" type="number" inputMode="numeric" min={1} max={maxNumber} step={1} value={guess}
            disabled={Boolean(result)} onChange={(event) => setGuess(event.target.value)} autoFocus />
        </label>
        <input type="range" min={1} max={maxNumber} value={valid ? value : Math.ceil(maxNumber / 2)} disabled={Boolean(result)}
          onChange={(event) => setGuess(event.target.value)} aria-label="Ajustar palpite" />
      </div>
      {result && (
        <p className="minigame-reveal">Era o <strong>{formatId(pokemon.id)}</strong>{result.difference > 0 ? ` — diferença de ${result.difference}.` : '!'}</p>
      )}
      {!result && (
        <button type="submit" className="button button-primary" disabled={!valid}><Icon name="check" size={16} />Confirmar</button>
      )}
    </form>
  )
}
