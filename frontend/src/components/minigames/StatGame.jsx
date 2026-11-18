import { STAT_FIELDS } from '../../data/pokedex.js'
import { scoreStatGuess } from './minigameLogic.js'

/** Adivinhar qual dos seis atributos base é o maior. Depois da resposta, mostra todos os valores. */
export default function StatGame({ pokemon, result, onAnswer }) {
  const best = Math.max(...STAT_FIELDS.map(({ key }) => pokemon.stats[key]))

  return (
    <div className="minigame-question">
      <p className="minigame-prompt">Qual é o maior atributo de <strong>{pokemon.name}</strong>?</p>
      <div className="minigame-stats" role="group" aria-label="Atributos">
        {STAT_FIELDS.map(({ key, label }) => {
          const chosen = result?.choice === key
          const isBest = result && result.highest.includes(key)
          return (
            <button key={key} type="button" disabled={Boolean(result)}
              className={`minigame-stat${chosen ? ' is-chosen' : ''}${isBest ? ' is-best' : ''}${result && chosen && !isBest ? ' is-wrong' : ''}`}
              onClick={() => onAnswer({ ...scoreStatGuess(key, pokemon), choice: key, guess: label })}>
              <span className="minigame-stat-label">{label}</span>
              {result && (
                <>
                  <span className="minigame-stat-value">{pokemon.stats[key]}</span>
                  <span className="minigame-stat-track" aria-hidden="true"><span style={{ width: `${(pokemon.stats[key] / best) * 100}%` }} /></span>
                </>
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
