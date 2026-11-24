import Icon from '../Icon.jsx'
import PokemonImage from '../PokemonImage.jsx'

function formatDay(date) {
  const [year, month, day] = date.split('-')
  return `${day}/${month}/${year}`
}

/**
 * Desafio do dia: treinador e time iguais para todos os jogadores, definidos pela API a partir da data.
 * Uma tentativa por dia neste navegador; as vitórias entram no ranking do dia.
 */
export default function DailyBattleCard({ daily, attempt, pokemonById, busy, onAccept, onResume }) {
  if (daily.status === 'error') return null
  if (daily.status !== 'ready') return <section className="daily-battle is-loading" aria-busy="true">Carregando o desafio do dia…</section>

  const { date, trainer, trainerTeam, playerTeam } = daily.data
  const played = attempt?.date === date ? attempt : null

  return (
    <section className="daily-battle" aria-labelledby="daily-battle-title">
      <header className="daily-battle-header">
        <span className="daily-battle-kicker"><Icon name="calendar" size={15} />Desafio do dia · {formatDay(date)}</span>
        <h2 id="daily-battle-title">Você x {trainer.name}</h2>
        <p>O mesmo time para todos os jogadores. Vence quem derrotar {trainer.name} em menos rodadas; no empate, ganha quem terminar com mais HP.</p>
      </header>

      <div className="daily-battle-teams">
        <div className="daily-battle-team">
          <span className="daily-battle-label">Seu time de hoje</span>
          <span className="daily-battle-thumbs">
            {playerTeam.map((member) => <PokemonImage key={member.id} pokemon={member} className="daily-battle-thumb" title={member.name} />)}
          </span>
        </div>
        <span className="daily-battle-vs" aria-hidden="true">VS</span>
        <div className="daily-battle-team">
          <span className="daily-battle-label">Equipe de {trainer.name}</span>
          <span className="daily-battle-thumbs">
            {trainerTeam.map((id, index) => {
              const member = pokemonById.get(id)
              return member ? <PokemonImage key={`${id}-${index}`} pokemon={member} className="daily-battle-thumb" title={member.name} /> : null
            })}
          </span>
        </div>
      </div>

      <div className="daily-battle-actions">
        {!played && (
          <button type="button" className="button button-primary" onClick={onAccept} disabled={busy}>
            <Icon name="swords" size={17} />Aceitar o desafio do dia
          </button>
        )}
        {played && !played.finished && (
          <button type="button" className="button button-primary" onClick={() => onResume(played.battleId)} disabled={busy}>
            <Icon name="swords" size={17} />Continuar o desafio do dia
          </button>
        )}
        {played?.finished && (
          <p className={`daily-battle-result ${played.won ? 'is-win' : 'is-loss'}`}>
            <Icon name={played.won ? 'trophy' : 'flag'} size={16} />
            {played.won
              ? <>Você venceu em <strong>{played.rounds} {played.rounds === 1 ? 'rodada' : 'rodadas'}</strong>. Veja sua posição no ranking do dia.</>
              : <>Não foi desta vez. Um novo desafio começa à meia-noite.</>}
          </p>
        )}
      </div>
    </section>
  )
}
