import { useState } from 'react'
import Icon from '../Icon.jsx'
import PokemonImage from '../PokemonImage.jsx'
import TypeBadge from '../TypeBadge.jsx'
import { ACTIONS, hpTone, specialMoveName, switchOptions } from './battleRules.js'

function HpBar({ fighter, label }) {
  const percent = Math.max(0, Math.round((fighter.currentHp / fighter.maxHp) * 100))
  return (
    <div className="battle-hp">
      <div className="battle-hp-label"><span>HP</span><strong>{fighter.currentHp} / {fighter.maxHp}</strong></div>
      <div className="battle-hp-track" role="progressbar" aria-label={label} aria-valuemin={0} aria-valuemax={fighter.maxHp} aria-valuenow={fighter.currentHp}>
        <div className={`battle-hp-fill ${hpTone(fighter.currentHp, fighter.maxHp)}`} style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}

function ActiveFighter({ fighter, side, owner, highlight }) {
  const types = fighter.types.length ? fighter.types : ['Typeless']
  return (
    <article className={`battle-fighter side-${side}${highlight ? ` is-${highlight}` : ''}`} aria-label={`${owner}: ${fighter.pokemonName}`}>
      <header className="battle-fighter-header">
        <span className="battle-owner">{owner}</span>
        {fighter.defending && fighter.currentHp > 0 && <span className="battle-status"><Icon name="shield" size={13} />Protegido</span>}
      </header>
      <PokemonImage pokemon={{ id: fighter.pokemonId, name: fighter.pokemonName }} className="battle-fighter-image" alt="" />
      <h3>{fighter.pokemonName}</h3>
      <span className="type-badges">{types.map((name) => <TypeBadge key={name} type={{ id: name, name }} />)}</span>
      <HpBar fighter={fighter} label={`HP de ${fighter.pokemonName}`} />
    </article>
  )
}

function TeamStrip({ team, activeIndex, side, title }) {
  return (
    <section className={`battle-team side-${side}`} aria-label={title}>
      <h4>{title}</h4>
      <ul>
        {team.map((fighter, index) => {
          const fainted = fighter.currentHp <= 0
          return (
            <li key={`${fighter.pokemonId}-${index}`} className={`${index === activeIndex ? 'is-active' : ''}${fainted ? ' is-fainted' : ''}`}
              title={`${fighter.pokemonName}: ${fainted ? 'desmaiado' : `${fighter.currentHp}/${fighter.maxHp} HP`}`}>
              <PokemonImage pokemon={{ id: fighter.pokemonId, name: fighter.pokemonName }} className="battle-team-image" alt="" />
              <span className="battle-team-name">{fighter.pokemonName}</span>
              <span className="battle-team-hp"><span className={hpTone(fighter.currentHp, fighter.maxHp)} style={{ width: `${Math.round((fighter.currentHp / fighter.maxHp) * 100)}%` }} /></span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

function ActionPanel({ battle, busy, onAction, onForfeit }) {
  const [choosingSwitch, setChoosingSwitch] = useState(false)
  const fighter = battle.playerTeam[battle.playerActive]
  const options = switchOptions(battle.playerTeam, battle.playerActive)

  function act(action, switchTo) {
    setChoosingSwitch(false)
    onAction(action, switchTo)
  }

  return (
    <section className="battle-actions" aria-labelledby="battle-turn-title">
      <h3 id="battle-turn-title">O que {fighter.pokemonName} vai fazer?</h3>
      {choosingSwitch ? (
        <>
          <p className="battle-actions-hint">Escolha quem entra em campo. A troca acontece antes dos golpes e gasta a rodada.</p>
          <div className="battle-switch-list">
            {options.map(({ fighter: option, index }) => (
              <button key={index} type="button" className="battle-switch-option" disabled={busy} onClick={() => act('SWITCH', index)}>
                <PokemonImage pokemon={{ id: option.pokemonId, name: option.pokemonName }} className="battle-team-image" alt="" />
                <span><strong>{option.pokemonName}</strong><small>{option.currentHp}/{option.maxHp} HP · {specialMoveName(option.types)}</small></span>
              </button>
            ))}
          </div>
          <button type="button" className="battle-link" onClick={() => setChoosingSwitch(false)}><Icon name="arrow-left" size={14} />Voltar</button>
        </>
      ) : (
        <>
          <div className="battle-action-buttons">
            {ACTIONS.map((action) => {
              const blocked = action.id === 'DEFEND' && fighter.lastAction === 'DEFEND'
              return (
                <button key={action.id} type="button" className="battle-action" disabled={busy || blocked}
                  onClick={() => act(action.id)} title={blocked ? 'Não é possível defender duas rodadas seguidas' : action.hint}>
                  <Icon name={action.icon} size={18} />
                  <span className="battle-action-name">{action.id === 'SPECIAL' ? specialMoveName(fighter.types) : action.label}</span>
                  <small>{blocked ? 'Indisponível nesta rodada' : action.hint}</small>
                </button>
              )
            })}
            <button type="button" className="battle-action" disabled={busy || options.length === 0} onClick={() => setChoosingSwitch(true)}
              title={options.length ? 'Colocar outro Pokémon em campo' : 'Nenhum outro Pokémon disponível'}>
              <Icon name="refresh" size={18} />
              <span className="battle-action-name">Trocar Pokémon</span>
              <small>{options.length ? `${options.length} disponível${options.length === 1 ? '' : 'is'} no time.` : 'Nenhum outro disponível.'}</small>
            </button>
          </div>
          <button type="button" className="battle-link battle-forfeit" onClick={onForfeit} disabled={busy}><Icon name="flag" size={14} />Desistir do desafio</button>
        </>
      )}
    </section>
  )
}

function BattleLog({ log }) {
  const rounds = new Map()
  for (const event of log) {
    if (!rounds.has(event.round)) rounds.set(event.round, [])
    rounds.get(event.round).push(event)
  }
  const ordered = [...rounds.entries()].sort(([first], [second]) => second - first)

  return (
    <section className="battle-log" aria-labelledby="battle-log-title">
      <h3 id="battle-log-title">Registro da batalha</h3>
      {ordered.length === 0 ? <p className="battle-empty">O desafio começou! Escolha a primeira ação.</p> : (
        <ol className="battle-log-list">
          {ordered.map(([round, events], index) => (
            <li key={round} className={index === 0 ? 'is-latest' : ''}>
              <span className="battle-log-round">{round === 0 ? 'Início' : `Rodada ${round}`}</span>
              <ul aria-live={index === 0 ? 'polite' : undefined}>
                {events.map((event, position) => (
                  <li key={position} className={`battle-log-event side-${event.side?.toLowerCase()}${event.effectiveness > 1 ? ' is-super' : ''}${event.critical ? ' is-critical' : ''}${event.action ? '' : ' is-narration'}`}>
                    {event.message}
                  </li>
                ))}
              </ul>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}

export default function BattleView({ battle, busy, error, onAction, onReroll, onForfeit, onRematch, onChooseTrainer }) {
  const finished = battle.status === 'FINISHED'
  const playerWon = battle.winner === 'PLAYER'
  const player = battle.playerTeam[battle.playerActive]
  const trainer = battle.trainerTeam[battle.trainerActive]
  const remainingPlayer = battle.playerTeam.filter((fighter) => fighter.currentHp > 0).length
  const remainingTrainer = battle.trainerTeam.filter((fighter) => fighter.currentHp > 0).length

  return (
    <div className="battle-view">
      <div className="battle-round-bar">
        <span><Icon name="swords" size={15} />{battle.playerName} x {battle.trainer.name} · {finished ? 'Desafio encerrado' : `Rodada ${battle.round + 1}`}</span>
        <span className="battle-score">{remainingPlayer} x {remainingTrainer} Pokémon em pé</span>
        {!finished && battle.round === 0 && (
          <button type="button" className="button button-secondary battle-reroll" onClick={onReroll} disabled={busy}>
            <Icon name="shuffle" size={15} />Sortear outro time
          </button>
        )}
      </div>

      <div className="battle-stage">
        <ActiveFighter fighter={player} side="player" owner={battle.playerName}
          highlight={finished ? (playerWon ? 'winner' : 'loser') : null} />
        <span className="battle-versus" aria-hidden="true">VS</span>
        <ActiveFighter fighter={trainer} side="trainer" owner={battle.trainer.name}
          highlight={finished ? (playerWon ? 'loser' : 'winner') : null} />
      </div>

      <div className="battle-teams">
        <TeamStrip team={battle.playerTeam} activeIndex={battle.playerActive} side="player" title={`Seu time (${battle.playerName})`} />
        <TeamStrip team={battle.trainerTeam} activeIndex={battle.trainerActive} side="trainer" title={`Equipe de ${battle.trainer.name}`} />
      </div>

      {error && <p className="form-alert" role="alert">{error}</p>}

      {finished ? (
        <section className={`battle-result${playerWon ? ' is-victory' : ' is-defeat'}`} role="status">
          <span className="battle-result-icon" aria-hidden="true"><Icon name={playerWon ? 'award' : 'flag'} size={30} /></span>
          <h3>{playerWon ? `Você derrotou ${battle.trainer.name}!` : `${battle.trainer.name} venceu o desafio`}</h3>
          <p>{playerWon
            ? `Insígnia conquistada em ${battle.round} ${battle.round === 1 ? 'rodada' : 'rodadas'}.`
            : battle.finishReason === 'FORFEIT' ? 'Você desistiu do desafio.' : 'Todos os seus Pokémon desmaiaram.'} A batalha foi salva no histórico.</p>
          <div className="battle-result-actions">
            <button type="button" className="button button-primary" onClick={onRematch} disabled={busy}><Icon name="refresh" size={16} />Desafiar de novo</button>
            <button type="button" className="button button-secondary" onClick={onChooseTrainer} disabled={busy}>Escolher outro treinador</button>
          </div>
        </section>
      ) : (
        <ActionPanel key={`${battle.round}-${battle.playerActive}`} battle={battle} busy={busy} onAction={onAction} onForfeit={onForfeit} />
      )}

      <BattleLog log={battle.log} />
    </div>
  )
}
