import Icon from '../Icon.jsx'

function describe(battle) {
  const { player1, player2 } = battle
  if (battle.status !== 'FINISHED') return `${player1.pokemonName} x ${player2.pokemonName}`
  const [winner, loser] = battle.winner === 1 ? [player1, player2] : [player2, player1]
  return `${winner.playerName} (${winner.pokemonName}) venceu ${loser.playerName} (${loser.pokemonName})`
}

export default function ArenaSidebar({ history, ranking, status, currentId, onResume, onRefresh }) {
  return (
    <aside className="arena-sidebar" aria-label="Ranking e histórico de batalhas">
      <section className="arena-panel">
        <header className="arena-panel-header">
          <h3><Icon name="trophy" size={16} />Ranking</h3>
          <button type="button" className="icon-button" onClick={onRefresh} aria-label="Atualizar ranking e histórico" title="Atualizar"><Icon name="refresh" size={16} /></button>
        </header>
        {status === 'error' && <p className="arena-empty">Não foi possível carregar.</p>}
        {status !== 'error' && ranking.length === 0 && <p className="arena-empty">Nenhuma batalha terminada ainda.</p>}
        {ranking.length > 0 && (
          <ol className="arena-ranking">
            {ranking.map((entry, index) => (
              <li key={entry.pokemonId}>
                <span className="arena-rank">{index + 1}</span>
                <span className="arena-rank-name">{entry.pokemonName}</span>
                <span className="arena-rank-score"><strong>{entry.wins}</strong> {entry.wins === 1 ? 'vitória' : 'vitórias'} · {entry.winRate}%</span>
              </li>
            ))}
          </ol>
        )}
      </section>

      <section className="arena-panel">
        <header className="arena-panel-header"><h3><Icon name="book" size={16} />Últimas batalhas</h3></header>
        {status !== 'error' && history.length === 0 && <p className="arena-empty">O histórico aparece aqui.</p>}
        {history.length > 0 && (
          <ul className="arena-history">
            {history.map((battle) => (
              <li key={battle.id} className={battle.id === currentId ? 'is-current' : ''}>
                <span>{describe(battle)}</span>
                <small>
                  {battle.status === 'FINISHED'
                    ? `${battle.finishReason === 'FORFEIT' ? 'Desistência' : 'Nocaute'} · ${battle.rounds} ${battle.rounds === 1 ? 'rodada' : 'rodadas'}`
                    : 'Em andamento'}
                </small>
                {battle.status !== 'FINISHED' && battle.id !== currentId && (
                  <button type="button" className="arena-resume" onClick={() => onResume(battle.id)}>Continuar</button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </aside>
  )
}
