import { useCallback, useEffect, useState } from 'react'
import { api } from '../services/api.js'
import Icon from './Icon.jsx'
import TrainerPicker from './battle/TrainerPicker.jsx'
import BattleView from './battle/BattleView.jsx'
import BattleSidebar from './battle/BattleSidebar.jsx'
import DailyBattleCard from './battle/DailyBattleCard.jsx'

const NAME_KEY = 'pokedex-player-name'
const DAILY_KEY = 'pokedex-daily-battle'

// Tentativa do desafio do dia neste navegador: { date, battleId, finished, won, rounds }.
function readAttempt() {
  try {
    return JSON.parse(localStorage.getItem(DAILY_KEY) || 'null')
  } catch {
    return null
  }
}

function attemptOf(battle) {
  return {
    date: battle.dailyDate,
    battleId: battle.id,
    finished: battle.status === 'FINISHED',
    won: battle.winner === 'PLAYER',
    rounds: battle.round,
  }
}

function readName() {
  try {
    return localStorage.getItem(NAME_KEY) || ''
  } catch {
    return ''
  }
}

/**
 * Desafios contra os treinadores: o jogador escolhe um treinador e recebe um time sorteado do mesmo
 * tamanho da equipe dele. A API sorteia, joga pelo treinador, calcula cada rodada e salva no MongoDB.
 */
export default function Battles({ trainers, pokemon, initialTrainerId }) {
  const [playerName, setPlayerName] = useState(readName)
  const [selectedId, setSelectedId] = useState(initialTrainerId ?? null)
  const [battle, setBattle] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [board, setBoard] = useState({ status: 'loading', history: [], stats: [], dailyRanking: [] })
  const [daily, setDaily] = useState({ status: 'loading', data: null })
  const [attempt, setAttempt] = useState(readAttempt)
  const pokemonById = new Map(pokemon.map((entry) => [entry.id, entry]))

  const loadBoard = useCallback(async () => {
    try {
      const [history, stats, dailyRanking] = await Promise.all([api.listBattles(6), api.battleStats(), api.dailyBattleRanking()])
      setBoard({ status: 'ready', history, stats, dailyRanking })
    } catch {
      setBoard((current) => ({ ...current, status: 'error' }))
    }
  }, [])

  useEffect(() => { loadBoard() }, [loadBoard])

  useEffect(() => {
    const controller = new AbortController()
    api.dailyBattle(controller.signal)
      .then((data) => setDaily({ status: 'ready', data }))
      .catch((failure) => { if (failure.name !== 'AbortError') setDaily({ status: 'error', data: null }) })
    return () => controller.abort()
  }, [])

  // A tentativa do dia acompanha a batalha diária em andamento até o fim.
  useEffect(() => {
    if (!battle?.dailyDate) return
    const next = attemptOf(battle)
    setAttempt(next)
    try {
      localStorage.setItem(DAILY_KEY, JSON.stringify(next))
    } catch {
      // Sem armazenamento, o desafio do dia pode ser repetido neste navegador.
    }
  }, [battle])

  useEffect(() => {
    try {
      localStorage.setItem(NAME_KEY, playerName)
    } catch {
      // O nome continua valendo nesta sessão.
    }
  }, [playerName])

  async function run(task) {
    setBusy(true)
    setError('')
    try {
      await task()
    } catch (failure) {
      setError(failure.message || 'Não foi possível falar com a API.')
    } finally {
      setBusy(false)
    }
  }

  function challenge(trainerId = selectedId) {
    return run(async () => {
      setBattle(await api.challengeTrainer(trainerId, playerName.trim() || undefined))
      loadBoard()
    })
  }

  function challengeDaily() {
    return run(async () => {
      setBattle(await api.challengeDaily(playerName.trim() || undefined))
      loadBoard()
    })
  }

  function play(action, switchTo) {
    run(async () => {
      const updated = await api.playRound(battle.id, action, switchTo)
      setBattle(updated)
      if (updated.status === 'FINISHED') loadBoard()
    })
  }

  function reroll() {
    run(async () => setBattle(await api.rerollTeam(battle.id)))
  }

  function forfeit() {
    if (!window.confirm(`Desistir do desafio contra ${battle.trainer.name}?`)) return
    run(async () => {
      setBattle(await api.forfeitBattle(battle.id))
      loadBoard()
    })
  }

  function resume(id) {
    run(async () => {
      const resumed = await api.getBattle(id)
      setBattle(resumed)
      if (!resumed.dailyDate) setSelectedId(resumed.trainer.id)
    })
  }

  function chooseTrainer() {
    setBattle(null)
    setError('')
    loadBoard()
  }

  return (
    <section aria-labelledby="battles-title">
      <div className="section-heading battle-heading">
        <div>
          <h1 className="section-title" id="battles-title">Desafie os treinadores</h1>
          <p className="section-description">Encare o desafio do dia, igual para todos, ou escolha um treinador e enfrente a equipe dele com um time sorteado do mesmo tamanho.</p>
        </div>
        {battle && battle.status !== 'FINISHED' && (
          <button type="button" className="button button-secondary" onClick={chooseTrainer}><Icon name="arrow-left" size={15} />Escolher treinador</button>
        )}
      </div>

      <div className="battle-layout">
        <div className="battle-main">
          {battle
            ? <BattleView battle={battle} busy={busy} error={error} onAction={play} onReroll={reroll} onForfeit={forfeit}
                onRematch={() => challenge(battle.trainer.id)} onChooseTrainer={chooseTrainer} />
            : <>
              <DailyBattleCard daily={daily} attempt={attempt} pokemonById={pokemonById} busy={busy} onAccept={challengeDaily} onResume={resume} />
              <TrainerPicker trainers={trainers} pokemon={pokemon} stats={board.stats} selectedId={selectedId} onSelect={setSelectedId}
                playerName={playerName} onPlayerNameChange={setPlayerName} onChallenge={() => challenge()} busy={busy} error={error} />
            </>}
        </div>
        <BattleSidebar history={board.history} stats={board.stats} dailyRanking={board.dailyRanking} playerName={playerName.trim() || 'Você'} status={board.status} currentId={battle?.id}
          onResume={resume} onRefresh={loadBoard} />
      </div>
    </section>
  )
}
