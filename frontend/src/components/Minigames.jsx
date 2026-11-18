import { useCallback, useEffect, useState } from 'react'
import { api } from '../services/api.js'
import { formatId, STAT_FIELDS, typeLabels } from '../data/pokedex.js'
import Icon from './Icon.jsx'
import PokemonImage from './PokemonImage.jsx'
import TypeBadge from './TypeBadge.jsx'
import TypeGame from './minigames/TypeGame.jsx'
import NumberGame from './minigames/NumberGame.jsx'
import StatGame from './minigames/StatGame.jsx'
import { GAMES, POINTS_PER_ROUND, ROUNDS, drawPokemon, gameById, maxDexNumber } from './minigames/minigameLogic.js'

const NAME_KEY = 'pokedex-player-name'

function readName() {
  try {
    return localStorage.getItem(NAME_KEY) || ''
  } catch {
    return ''
  }
}

function newMatch(game, pokemon) {
  const first = drawPokemon(pokemon)
  return { game, round: 1, current: first, usedIds: first ? [first.id] : [], result: null, rounds: [], saved: null, saveError: '' }
}

function feedbackTitle(result) {
  if (result.correct) return 'Acertou!'
  if (result.hint) return result.hint
  return result.points > 0 ? 'Quase!' : 'Errou!'
}

function answerText(game, pokemon, result) {
  if (game === 'TYPE') return `Tipo: ${result.actual.map((name) => typeLabels[name] || name).join(' + ')}`
  if (game === 'NUMBER') return `Número: ${formatId(pokemon.id)}`
  return `Maior atributo: ${result.highestLabels}`
}

/**
 * Página de minigames: em cada rodada um Pokémon é sorteado e o jogador aplica o minigame escolhido.
 * Partidas de 5 rodadas; o resultado vai para a API (coleção minigame_scores) e entra no ranking.
 */
export default function Minigames({ pokemon, types }) {
  const [playerName, setPlayerName] = useState(readName)
  const [match, setMatch] = useState(null)
  const [boardGame, setBoardGame] = useState('TYPE')
  const [board, setBoard] = useState({ status: 'loading', leaderboard: [], stats: [] })
  const maxNumber = maxDexNumber(pokemon)

  const loadBoard = useCallback(async (game) => {
    try {
      const [leaderboard, stats] = await Promise.all([api.minigameLeaderboard(game), api.minigameStats()])
      setBoard({ status: 'ready', leaderboard, stats })
    } catch {
      setBoard((current) => ({ ...current, status: 'error' }))
    }
  }, [])

  useEffect(() => { loadBoard(boardGame) }, [boardGame, loadBoard])

  useEffect(() => {
    try {
      localStorage.setItem(NAME_KEY, playerName)
    } catch {
      // O nome continua valendo nesta sessão.
    }
  }, [playerName])

  function start(game) {
    setBoardGame(game)
    setMatch(newMatch(game, pokemon))
  }

  function answer(result) {
    const highestLabels = result.highest?.map((key) => STAT_FIELDS.find((field) => field.key === key).label).join(' / ')
    const full = { ...result, highestLabels }
    setMatch((current) => ({
      ...current,
      result: full,
      rounds: [...current.rounds, { pokemon: current.current, points: full.points, guess: full.guess, correct: full.correct }],
    }))
  }

  async function next() {
    if (match.round < ROUNDS) {
      const upcoming = drawPokemon(pokemon, match.usedIds)
      setMatch((current) => ({ ...current, round: current.round + 1, current: upcoming, usedIds: [...current.usedIds, upcoming.id], result: null }))
      return
    }
    const total = match.rounds.reduce((sum, round) => sum + round.points, 0)
    setMatch((current) => ({ ...current, finished: true }))
    try {
      const saved = await api.saveMinigameScore(match.game, playerName.trim() || undefined, total, ROUNDS)
      setMatch((current) => ({ ...current, saved }))
    } catch (failure) {
      setMatch((current) => ({ ...current, saveError: failure.message || 'Não foi possível salvar o placar.' }))
    }
    loadBoard(match.game)
  }

  const game = match && gameById(match.game)
  const total = match ? match.rounds.reduce((sum, round) => sum + round.points, 0) : 0

  return (
    <section aria-labelledby="minigames-title">
      <div className="section-heading minigame-heading">
        <div>
          <h1 className="section-title" id="minigames-title">Minigames</h1>
          <p className="section-description">Um Pokémon é sorteado a cada rodada. Partidas de {ROUNDS} rodadas, até {POINTS_PER_ROUND} pontos por rodada.</p>
        </div>
        {match && <button type="button" className="button button-secondary" onClick={() => setMatch(null)}><Icon name="arrow-left" size={15} />Escolher jogo</button>}
      </div>

      <div className="minigame-layout">
        <div className="minigame-main">
          {!match && (
            <>
              <label className="form-field minigame-name">
                <span>Seu nome no ranking</span>
                <input id="minigame-player-name" type="text" maxLength={20} placeholder="Anônimo" value={playerName}
                  onChange={(event) => setPlayerName(event.target.value)} />
              </label>
              <div className="minigame-cards">
                {GAMES.map((item) => (
                  <article key={item.id} className="minigame-card">
                    <span className="minigame-card-icon" aria-hidden="true"><Icon name={item.icon} size={24} /></span>
                    <h2>{item.title}</h2>
                    <p>{item.description}</p>
                    <small>{item.rule}</small>
                    <button type="button" className="button button-primary" onClick={() => start(item.id)} disabled={pokemon.length === 0}>
                      <Icon name="gamepad" size={16} />Jogar
                    </button>
                  </article>
                ))}
              </div>
            </>
          )}

          {match && !match.finished && match.current && (
            <div className="minigame-round">
              <div className="minigame-round-bar">
                <span><Icon name={game.icon} size={15} />{game.title}</span>
                <span className="minigame-progress" aria-label={`Rodada ${match.round} de ${ROUNDS}`}>
                  {Array.from({ length: ROUNDS }, (_, index) => (
                    <span key={index} className={index < match.rounds.length ? (match.rounds[index].correct ? 'is-right' : match.rounds[index].points > 0 ? 'is-partial' : 'is-wrong') : index === match.round - 1 ? 'is-current' : ''} />
                  ))}
                </span>
                <span>Rodada {match.round}/{ROUNDS} · <strong>{total}</strong> pts</span>
              </div>

              <div className="minigame-stage">
                <div className="minigame-pokemon">
                  <PokemonImage pokemon={match.current} className="minigame-image" loading="eager" />
                  <h2>{match.current.name}</h2>
                  {match.game !== 'TYPE' && (
                    <span className="type-badges">{(match.current.types.length ? match.current.types : [{ id: 0, name: 'Typeless' }]).map((type) => <TypeBadge key={type.id} type={type} />)}</span>
                  )}
                  {match.game !== 'NUMBER' && <span className="minigame-dex">{formatId(match.current.id)}</span>}
                </div>
                <div className="minigame-play">
                  {match.game === 'TYPE' && <TypeGame key={match.round} pokemon={match.current} types={types} result={match.result} onAnswer={answer} />}
                  {match.game === 'NUMBER' && <NumberGame key={match.round} pokemon={match.current} maxNumber={maxNumber} result={match.result} onAnswer={answer} />}
                  {match.game === 'STAT' && <StatGame key={match.round} pokemon={match.current} result={match.result} onAnswer={answer} />}

                  {match.result && (
                    <div className={`minigame-feedback ${match.result.correct ? 'is-right' : match.result.points > 0 ? 'is-partial' : 'is-wrong'}`} role="status">
                      <strong>{feedbackTitle(match.result)} +{match.result.points} pontos</strong>
                      <span>{answerText(match.game, match.current, match.result)}</span>
                      <button type="button" className="button button-primary" onClick={next} autoFocus>
                        {match.round < ROUNDS ? <>Próximo Pokémon <Icon name="arrow-right" size={15} /></> : <>Ver resultado <Icon name="trophy" size={15} /></>}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {match?.finished && (
            <section className="minigame-summary" aria-labelledby="minigame-summary-title">
              <span className="minigame-card-icon" aria-hidden="true"><Icon name="trophy" size={28} /></span>
              <h2 id="minigame-summary-title">{total} de {ROUNDS * POINTS_PER_ROUND} pontos</h2>
              <p>
                {game.title} · {match.rounds.filter((round) => round.correct).length} {match.rounds.filter((round) => round.correct).length === 1 ? 'acerto' : 'acertos'} em {ROUNDS} rodadas.
                {match.saved && <> Você ficou em <strong>{match.saved.position}º lugar</strong> no ranking.</>}
                {!match.saved && !match.saveError && ' Salvando no ranking…'}
              </p>
              {match.saveError && <p className="form-alert" role="alert">{match.saveError}</p>}
              <ol className="minigame-rounds">
                {match.rounds.map((round, index) => (
                  <li key={index}>
                    <PokemonImage pokemon={round.pokemon} className="minigame-round-image" alt="" />
                    <span><strong>{round.pokemon.name}</strong><small>Seu palpite: {round.guess}</small></span>
                    <span className={`minigame-round-points ${round.correct ? 'is-right' : round.points > 0 ? 'is-partial' : 'is-wrong'}`}>+{round.points}</span>
                  </li>
                ))}
              </ol>
              <div className="minigame-summary-actions">
                <button type="button" className="button button-primary" onClick={() => start(match.game)}><Icon name="refresh" size={16} />Jogar de novo</button>
                <button type="button" className="button button-secondary" onClick={() => setMatch(null)}>Escolher outro jogo</button>
              </div>
            </section>
          )}
        </div>

        <aside className="minigame-sidebar" aria-label="Ranking dos minigames">
          <section className="battle-panel">
            <header className="battle-panel-header"><h3><Icon name="trophy" size={16} />Ranking</h3></header>
            <div className="minigame-tabs" role="tablist" aria-label="Escolher ranking">
              {GAMES.map((item) => (
                <button key={item.id} type="button" role="tab" aria-selected={boardGame === item.id} className={boardGame === item.id ? 'is-active' : ''}
                  onClick={() => setBoardGame(item.id)} title={item.title}>
                  <Icon name={item.icon} size={14} />{item.id === 'TYPE' ? 'Tipo' : item.id === 'NUMBER' ? 'Número' : 'Atributo'}
                </button>
              ))}
            </div>
            {board.status === 'error' && <p className="battle-empty">Não foi possível carregar.</p>}
            {board.status !== 'error' && board.leaderboard.length === 0 && <p className="battle-empty">Ninguém jogou ainda. Seja o primeiro!</p>}
            {board.leaderboard.length > 0 && (
              <ol className="minigame-leaderboard">
                {board.leaderboard.map((entry, index) => (
                  <li key={entry.id} className={match?.saved?.score.id === entry.id ? 'is-mine' : ''}>
                    <span className="minigame-rank">{index + 1}</span>
                    <span className="minigame-rank-name">{entry.playerName}</span>
                    <span className="minigame-rank-score">{entry.score}</span>
                  </li>
                ))}
              </ol>
            )}
          </section>
          <section className="battle-panel">
            <header className="battle-panel-header"><h3><Icon name="chart" size={16} />Estatísticas</h3></header>
            <ul className="minigame-stats-list">
              {board.stats.map((entry) => (
                <li key={entry.game}>
                  <strong>{gameById(entry.game)?.title}</strong>
                  <span>{entry.plays} {entry.plays === 1 ? 'partida' : 'partidas'} · recorde {entry.bestScore} · média {entry.averagePercent}%</span>
                </li>
              ))}
            </ul>
          </section>
        </aside>
      </div>
    </section>
  )
}
