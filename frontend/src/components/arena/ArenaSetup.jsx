import { formatId, STAT_FIELDS } from '../../data/pokedex.js'
import Icon from '../Icon.jsx'
import PokemonImage from '../PokemonImage.jsx'
import TypeBadge from '../TypeBadge.jsx'
import { battleHp, specialMoveName } from './arenaRules.js'

function PlayerSetup({ player, choice, pokemon, onChange }) {
  const selected = pokemon.find((entry) => entry.id === choice.pokemonId)
  const inputId = `arena-player-${player}`

  function pickRandom() {
    const entry = pokemon[Math.floor(Math.random() * pokemon.length)]
    if (entry) onChange({ ...choice, pokemonId: entry.id })
  }

  return (
    <section className={`arena-setup-card player-${player}`} aria-labelledby={`${inputId}-title`}>
      <header className="arena-setup-header">
        <span className="arena-player-tag" id={`${inputId}-title`}>Jogador {player}</span>
        <button type="button" className="random-button" onClick={pickRandom}><Icon name="shuffle" size={15} />Aleatório</button>
      </header>

      <label className="form-field">
        <span>Nome do jogador</span>
        <input id={`${inputId}-name`} type="text" maxLength={20} placeholder={`Jogador ${player}`} value={choice.name}
          onChange={(event) => onChange({ ...choice, name: event.target.value })} />
      </label>

      <label className="form-field">
        <span>Pokémon</span>
        <select id={`${inputId}-pokemon`} className="arena-select" value={choice.pokemonId ?? ''}
          onChange={(event) => onChange({ ...choice, pokemonId: Number(event.target.value) })}>
          {pokemon.map((entry) => <option key={entry.id} value={entry.id}>{formatId(entry.id)} {entry.name}</option>)}
        </select>
      </label>

      {selected && (
        <div className="arena-preview">
          <PokemonImage pokemon={selected} className="arena-preview-image" alt="" />
          <div className="arena-preview-info">
            <strong>{selected.name}</strong>
            <span className="type-badges">
              {(selected.types.length ? selected.types : [{ id: 0, name: 'Typeless' }]).map((type) => <TypeBadge key={type.id} type={type} />)}
            </span>
            <dl className="arena-preview-stats">
              <div><dt>HP na batalha</dt><dd>{battleHp(selected.stats.hp)}</dd></div>
              {STAT_FIELDS.slice(1).map(({ key, shortLabel, label }) => (
                <div key={key}><dt><abbr title={label}>{shortLabel}</abbr></dt><dd>{selected.stats[key]}</dd></div>
              ))}
            </dl>
            <span className="arena-move-hint">Especial: <strong>{specialMoveName(selected.types.map((type) => type.name))}</strong></span>
          </div>
        </div>
      )}
    </section>
  )
}

export default function ArenaSetup({ pokemon, setup, onChange, onStart, busy, error }) {
  const ready = pokemon.some((entry) => entry.id === setup.player1.pokemonId)
    && pokemon.some((entry) => entry.id === setup.player2.pokemonId)

  return (
    <div className="arena-setup">
      <div className="arena-setup-grid">
        <PlayerSetup player={1} choice={setup.player1} pokemon={pokemon} onChange={(choice) => onChange({ ...setup, player1: choice })} />
        <span className="arena-versus" aria-hidden="true">VS</span>
        <PlayerSetup player={2} choice={setup.player2} pokemon={pokemon} onChange={(choice) => onChange({ ...setup, player2: choice })} />
      </div>

      {error && <p className="form-alert" role="alert">{error}</p>}

      <div className="arena-start">
        <button type="button" className="button button-primary" onClick={onStart} disabled={!ready || busy}>
          <Icon name="swords" size={17} />{busy ? 'Preparando a arena…' : 'Começar batalha'}
        </button>
      </div>

      <details className="arena-rules">
        <summary>Como funciona a batalha</summary>
        <ul>
          <li>Os dois jogadores usam o mesmo dispositivo. Em cada rodada, o <strong>Jogador 1</strong> escolhe a ação, depois o <strong>Jogador 2</strong>, sem ver a escolha do outro.</li>
          <li><strong>Investida</strong>: golpe físico neutro (Ataque contra Defesa).</li>
          <li><strong>Golpe especial</strong>: usa o tipo principal do Pokémon (Ataque especial contra Defesa especial). Pode ser super efetivo (2x ou 4x), pouco efetivo ou não ter efeito.</li>
          <li><strong>Defender</strong>: age antes de todos e reduz o dano recebido à metade, mas não pode ser usado duas rodadas seguidas.</li>
          <li>O Pokémon mais rápido ataca primeiro. Golpes críticos (1 em 16) causam 50% a mais. Vence quem deixar o adversário sem HP.</li>
          <li>Tudo é calculado pela API e cada batalha fica salva no MongoDB, alimentando o histórico e o ranking.</li>
        </ul>
      </details>
    </div>
  )
}
