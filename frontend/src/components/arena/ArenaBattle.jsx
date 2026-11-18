import Icon from '../Icon.jsx'
import PokemonImage from '../PokemonImage.jsx'
import TypeBadge from '../TypeBadge.jsx'
import { ACTIONS, hpTone, specialMoveName } from './arenaRules.js'

function FighterCard({ fighter, player, isWinner, isLoser, isTurn }) {
  const percent = Math.max(0, Math.round((fighter.currentHp / fighter.maxHp) * 100))
  const types = fighter.types.length ? fighter.types : ['Typeless']

  return (
    <article className={`arena-fighter player-${player}${isWinner ? ' is-winner' : ''}${isLoser ? ' is-loser' : ''}${isTurn ? ' is-turn' : ''}`}
      aria-label={`${fighter.playerName} com ${fighter.pokemonName}`}>
      <header className="arena-fighter-header">
        <span className="arena-player-tag">{fighter.playerName}</span>
        {fighter.defending && !isLoser && <span className="arena-status"><Icon name="shield" size={13} />Protegido</span>}
        {isWinner && <span className="arena-status is-winner"><Icon name="trophy" size={13} />Vencedor</span>}
      </header>
      <PokemonImage pokemon={{ id: fighter.pokemonId, name: fighter.pokemonName }} className="arena-fighter-image" alt="" />
      <h3>{fighter.pokemonName}</h3>
      <span className="type-badges">{types.map((name) => <TypeBadge key={name} type={{ id: name, name }} />)}</span>
      <div className="arena-hp">
        <div className="arena-hp-label"><span>HP</span><strong>{fighter.currentHp} / {fighter.maxHp}</strong></div>
        <div className="arena-hp-track" role="progressbar" aria-label={`HP de ${fighter.pokemonName}`}
          aria-valuemin={0} aria-valuemax={fighter.maxHp} aria-valuenow={fighter.currentHp}>
          <div className={`arena-hp-fill ${hpTone(fighter.currentHp, fighter.maxHp)}`} style={{ width: `${percent}%` }} />
        </div>
      </div>
    </article>
  )
}

function ActionPanel({ battle, step, busy, onChoose, onForfeit }) {
  const player = step === 'player1' ? 1 : 2
  const fighter = player === 1 ? battle.player1 : battle.player2
  const other = player === 1 ? battle.player2 : battle.player1

  return (
    <section className={`arena-actions player-${player}`} aria-labelledby="arena-turn-title">
      <div className="arena-turn">
        <h3 id="arena-turn-title">Vez de {fighter.playerName}</h3>
        <p>{player === 1
          ? <>Escolha a ação de <strong>{fighter.pokemonName}</strong>. {other.playerName}, não espie!</>
          : <>{other.playerName} já escolheu (em segredo). Escolha a ação de <strong>{fighter.pokemonName}</strong>.</>}
        </p>
      </div>
      <div className="arena-action-buttons">
        {ACTIONS.map((action) => {
          const blocked = action.id === 'DEFEND' && fighter.lastAction === 'DEFEND'
          return (
            <button key={action.id} type="button" className={`arena-action action-${action.id.toLowerCase()}`}
              disabled={busy || blocked} onClick={() => onChoose(action.id)}
              title={blocked ? 'Não é possível defender duas rodadas seguidas' : action.hint}>
              <Icon name={action.icon} size={18} />
              <span className="arena-action-name">{action.id === 'SPECIAL' ? specialMoveName(fighter.types) : action.label}</span>
              <small>{blocked ? 'Indisponível nesta rodada' : action.hint}</small>
            </button>
          )
        })}
      </div>
      <button type="button" className="arena-forfeit" onClick={() => onForfeit(player)} disabled={busy}>
        <Icon name="flag" size={14} />{fighter.playerName} desiste
      </button>
    </section>
  )
}

function BattleLog({ battle }) {
  const rounds = new Map()
  for (const event of battle.log) {
    if (!rounds.has(event.round)) rounds.set(event.round, [])
    rounds.get(event.round).push(event)
  }
  const ordered = [...rounds.entries()].sort(([first], [second]) => second - first)

  return (
    <section className="arena-log" aria-labelledby="arena-log-title">
      <h3 id="arena-log-title">Registro da batalha</h3>
      {ordered.length === 0 ? <p className="arena-empty">A batalha começou! Escolham suas ações.</p> : (
        <ol className="arena-log-list">
          {ordered.map(([round, events], index) => (
            <li key={round} className={index === 0 ? 'is-latest' : ''}>
              <span className="arena-log-round">{round === 0 ? 'Início' : `Rodada ${round}`}</span>
              <ul aria-live={index === 0 ? 'polite' : undefined}>
                {events.map((event, position) => (
                  <li key={position} className={`arena-log-event player-${event.player}${event.effectiveness > 1 ? ' is-super' : ''}${event.critical ? ' is-critical' : ''}`}>
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

export default function ArenaBattle({ battle, step, busy, error, onChoose, onForfeit, onRematch, onNewBattle }) {
  const finished = battle.status === 'FINISHED'
  const winner = finished ? (battle.winner === 1 ? battle.player1 : battle.player2) : null

  return (
    <div className="arena-battle">
      <div className="arena-round-bar">
        <span><Icon name="swords" size={15} />{finished ? 'Batalha encerrada' : `Rodada ${battle.round + 1}`}</span>
        {!finished && <span className="arena-pending">{step === 'player2' ? `${battle.player1.playerName} já escolheu` : 'Aguardando as escolhas'}</span>}
      </div>

      <div className="arena-stage">
        <FighterCard fighter={battle.player1} player={1} isTurn={!finished && step === 'player1'}
          isWinner={finished && battle.winner === 1} isLoser={finished && battle.winner === 2} />
        <span className="arena-versus" aria-hidden="true">VS</span>
        <FighterCard fighter={battle.player2} player={2} isTurn={!finished && step === 'player2'}
          isWinner={finished && battle.winner === 2} isLoser={finished && battle.winner === 1} />
      </div>

      {error && <p className="form-alert" role="alert">{error}</p>}

      {finished ? (
        <section className="arena-result" role="status">
          <span className="arena-result-icon" aria-hidden="true"><Icon name="trophy" size={30} /></span>
          <h3>{winner.playerName} venceu com {winner.pokemonName}!</h3>
          <p>{battle.finishReason === 'FORFEIT' ? 'Vitória por desistência' : `Nocaute em ${battle.round} ${battle.round === 1 ? 'rodada' : 'rodadas'}`}. A batalha foi salva no histórico.</p>
          <div className="arena-result-actions">
            <button type="button" className="button button-primary" onClick={onRematch} disabled={busy}><Icon name="refresh" size={16} />Revanche</button>
            <button type="button" className="button button-secondary" onClick={onNewBattle} disabled={busy}>Nova batalha</button>
          </div>
        </section>
      ) : (
        <ActionPanel battle={battle} step={step} busy={busy} onChoose={onChoose} onForfeit={onForfeit} />
      )}

      <BattleLog battle={battle} />
    </div>
  )
}
