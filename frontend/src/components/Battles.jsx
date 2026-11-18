import { useCallback, useEffect, useState } from 'react'
import { api } from '../services/api.js'
import Icon from './Icon.jsx'
import TrainerPicker from './battle/TrainerPicker.jsx'
import BattleView from './battle/BattleView.jsx'
import BattleSidebar from './battle/BattleSidebar.jsx'

const NAME_KEY = 'pokedex-player-name'

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
  const [board, setBoard] = useState({ status: 'loading', history: [], stats: [] })

  const loadBoard = useCallback(async () => {
    try {
      const [history, stats] = await Promise.all([api.listBattles(6), api.battleStats()])
      setBoard({ status: 'ready', history, stats })
    } catch {
      setBoard((current) => ({ ...current, status: 'error' }))
    }
  }, [])

  useEffect(() => { loadBoard() }, [loadBoard])

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
      setSelectedId(resumed.trainer.id)
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
          <p className="section-description">Escolha um treinador e enfrente a equipe dele com um time sorteado do mesmo tamanho.</p>
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
            : <TrainerPicker trainers={trainers} pokemon={pokemon} stats={board.stats} selectedId={selectedId} onSelect={setSelectedId}
                playerName={playerName} onPlayerNameChange={setPlayerName} onChallenge={() => challenge()} busy={busy} error={error} />}
        </div>
        <BattleSidebar history={board.history} stats={board.stats} status={board.status} currentId={battle?.id}
          onResume={resume} onRefresh={loadBoard} />
      </div>
    </section>
  )
}
