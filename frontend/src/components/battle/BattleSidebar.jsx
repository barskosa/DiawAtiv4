import Icon from '../Icon.jsx'

function describe(battle) {
  const opponent = battle.trainer.name
  if (battle.status !== 'FINISHED') return `${battle.playerName} x ${opponent}`
  return battle.winner === 'PLAYER' ? `${battle.playerName} venceu ${opponent}` : `${opponent} venceu ${battle.playerName}`
}

export default function BattleSidebar({ history, stats, dailyRanking = [], playerName, status, currentId, onResume, onRefresh }) {
  const badges = stats.filter((entry) => entry.playerWins > 0).length

  return (
    <aside className="battle-sidebar" aria-label="Estatísticas e histórico de desafios">
      <section className="battle-panel">
        <header className="battle-panel-header"><h3><Icon name="calendar" size={16} />Ranking do dia</h3></header>
        {status !== 'error' && dailyRanking.length === 0 && <p className="battle-empty">Ninguém venceu o desafio de hoje ainda.</p>}
        {dailyRanking.length > 0 && (
          <ol className="minigame-leaderboard">
            {dailyRanking.map((entry) => (
              <li key={entry.battleId} className={entry.playerName === playerName ? 'is-mine' : ''}>
                <span className="minigame-rank">{entry.position}</span>
                <span className="minigame-rank-name">{entry.playerName}</span>
                <span className="minigame-rank-score" title={`${entry.hpPercent}% de HP no fim`}>{entry.rounds} {entry.rounds === 1 ? 'rodada' : 'rodadas'}</span>
              </li>
            ))}
          </ol>
        )}
      </section>

      <section className="battle-panel">
        <header className="battle-panel-header">
          <h3><Icon name="award" size={16} />Desempenho</h3>
          <button type="button" className="icon-button" onClick={onRefresh} aria-label="Atualizar estatísticas e histórico" title="Atualizar"><Icon name="refresh" size={16} /></button>
        </header>
        {status === 'error' && <p className="battle-empty">Não foi possível carregar.</p>}
        {status !== 'error' && stats.length === 0 && <p className="battle-empty">Nenhum desafio terminado ainda.</p>}
        {stats.length > 0 && (
          <>
            <p className="battle-badge-count"><strong>{badges}</strong> {badges === 1 ? 'treinador derrotado' : 'treinadores derrotados'}</p>
            <ul className="battle-stats">
              {stats.map((entry) => (
                <li key={entry.trainerId}>
                  <span className="battle-stats-name">{entry.playerWins > 0 && <Icon name="trophy" size={13} />}{entry.trainerName}</span>
                  <span className="battle-stats-score">{entry.playerWins}V · {entry.trainerWins}D · {entry.playerWinRate}%</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>

      <section className="battle-panel">
        <header className="battle-panel-header"><h3><Icon name="book" size={16} />Últimos desafios</h3></header>
        {status !== 'error' && history.length === 0 && <p className="battle-empty">O histórico aparece aqui.</p>}
        {history.length > 0 && (
          <ul className="battle-history">
            {history.map((battle) => (
              <li key={battle.id} className={battle.id === currentId ? 'is-current' : ''}>
                <span>{describe(battle)}{battle.dailyDate && <span className="minigame-daily-tag">Diário</span>}</span>
                <small>
                  {battle.status === 'FINISHED'
                    ? `${battle.finishReason === 'FORFEIT' ? 'Desistência' : 'Nocaute'} · ${battle.rounds} ${battle.rounds === 1 ? 'rodada' : 'rodadas'}`
                    : `Em andamento · ${battle.playerRemaining} x ${battle.trainerRemaining} Pokémon`}
                </small>
                {battle.status !== 'FINISHED' && battle.id !== currentId && (
                  <button type="button" className="battle-link" onClick={() => onResume(battle.id)}>Continuar</button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </aside>
  )
}
