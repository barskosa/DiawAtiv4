import { useState } from 'react'
import { MAX_TYPES, TYPELESS, typeLabels } from '../../data/pokedex.js'
import Icon from '../Icon.jsx'
import { scoreTypeGuess } from './minigameLogic.js'

/** Escolher o tipo (ou os dois tipos) do Pokémon. "Sem tipo" não combina com outros. */
export default function TypeGame({ pokemon, types, result, onAnswer }) {
  const [selected, setSelected] = useState([])
  const answered = Boolean(result)
  const options = [...types.filter((type) => type.name !== TYPELESS), { id: 0, name: TYPELESS }]

  function toggle(name) {
    setSelected((current) => {
      if (current.includes(name)) return current.filter((item) => item !== name)
      if (name === TYPELESS) return [TYPELESS]
      const withoutTypeless = current.filter((item) => item !== TYPELESS)
      return withoutTypeless.length < MAX_TYPES ? [...withoutTypeless, name] : withoutTypeless
    })
  }

  function status(name) {
    if (!answered) return selected.includes(name) ? 'is-selected' : ''
    if (result.actual.includes(name)) return selected.includes(name) ? 'is-right' : 'is-missed'
    return selected.includes(name) ? 'is-wrong' : 'is-faded'
  }

  return (
    <div className="minigame-question">
      <p className="minigame-prompt">Qual é o tipo de <strong>{pokemon.name}</strong>? Escolha até {MAX_TYPES}.</p>
      <div className="quick-filters minigame-types" role="group" aria-label="Tipos">
        {options.map(({ id, name }) => {
          const isSelected = selected.includes(name)
          const full = !isSelected && name !== TYPELESS && selected.filter((item) => item !== TYPELESS).length >= MAX_TYPES
          return (
            <button key={id} type="button" aria-pressed={isSelected} disabled={answered || full} onClick={() => toggle(name)}
              className={`filter-chip type-${name.toLowerCase()} ${status(name)}`}>
              <span className="type-dot" aria-hidden="true" />{typeLabels[name] || name}
            </button>
          )
        })}
      </div>
      {!answered && (
        <button type="button" className="button button-primary" disabled={selected.length === 0}
          onClick={() => onAnswer({ ...scoreTypeGuess(selected, pokemon), guess: selected.map((name) => typeLabels[name] || name).join(' + ') })}>
          <Icon name="check" size={16} />Confirmar
        </button>
      )}
    </div>
  )
}
