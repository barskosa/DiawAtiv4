import { useCallback, useEffect, useState } from 'react'
import { api } from '../services/api.js'
import Icon from './Icon.jsx'
import ArenaSetup from './arena/ArenaSetup.jsx'
import ArenaBattle from './arena/ArenaBattle.jsx'
import ArenaSidebar from './arena/ArenaSidebar.jsx'

function defaultChoice(pokemon, preferredId, fallbackIndex) {
  const preferred = pokemon.find((entry) => entry.id === preferredId)
  return { name: '', pokemonId: (preferred ?? pokemon[fallbackIndex] ?? pokemon[0])?.id ?? null }
}

/**
 * Arena PvP: dois jogadores no mesmo dispositivo. A API calcula cada rodada e salva a batalha no
 * MongoDB; a interface só coleta as escolhas e mostra o resultado.
 */
export default function Arena({ pokemon }) {
  const [setup, setSetup] = useState(() => ({ player1: defaultChoice(pokemon, 25, 0), player2: defaultChoice(pokemon, 7, 1) }))
  const [battle, setBattle] = useState(null)
  const [step, setStep] = useState('player1')
  const [player1Action, setPlayer1Action] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [board, setBoard] = useState({ status: 'loading', history: [], ranking: [] })

  const loadBoard = useCallback(async () => {
    try {
      const [history, ranking] = await Promise.all([api.listBattles(6), api.battleRanking(5)])
      setBoard({ status: 'ready', history, ranking })
    } catch {
      setBoard((current) => ({ ...current, status: 'error' }))
    }
  }, [])

  useEffect(() => { loadBoard() }, [loadBoard])

  // Se um Pokémon escolhido for excluído, volta para um padrão válido.
  useEffect(() => {
    setSetup((current) => ({
      player1: pokemon.some((entry) => entry.id === current.player1.pokemonId) ? current.player1 : { ...current.player1, pokemonId: defaultChoice(pokemon, 25, 0).pokemonId },
      player2: pokemon.some((entry) => entry.id === current.player2.pokemonId) ? current.player2 : { ...current.player2, pokemonId: defaultChoice(pokemon, 7, 1).pokemonId },
    }))
  }, [pokemon])

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

  function startBattle(choices = setup) {
    return run(async () => {
      const created = await api.createBattle(choices.player1, choices.player2)
      setBattle(created)
      setStep('player1')
      setPlayer1Action(null)
      loadBoard()
    })
  }

  function choose(action) {
    if (step === 'player1') {
      setPlayer1Action(action)
      setStep('player2')
      return
    }
    run(async () => {
      try {
        const updated = await api.playRound(battle.id, player1Action, action)
        setBattle(updated)
        if (updated.status === 'FINISHED') loadBoard()
      } finally {
        setPlayer1Action(null)
        setStep('player1')
      }
    })
  }

  function forfeit(player) {
    const fighter = player === 1 ? battle.player1 : battle.player2
    if (!window.confirm(`${fighter.playerName} quer mesmo desistir?`)) return
    run(async () => {
      setBattle(await api.forfeitBattle(battle.id, player))
      loadBoard()
    })
  }

  function resume(id) {
    run(async () => {
      setBattle(await api.getBattle(id))
      setStep('player1')
      setPlayer1Action(null)
    })
  }

  function rematch() {
    const choices = {
      player1: { name: battle.player1.playerName, pokemonId: battle.player1.pokemonId },
      player2: { name: battle.player2.playerName, pokemonId: battle.player2.pokemonId },
    }
    const available = (id) => pokemon.some((entry) => entry.id === id)
    if (!available(choices.player1.pokemonId) || !available(choices.player2.pokemonId)) {
      setError('Um dos Pokémon desta batalha não existe mais na Pokédex. Escolha outro em Nova batalha.')
      return
    }
    startBattle(choices)
  }

  function newBattle() {
    if (battle) {
      setSetup((current) => ({
        player1: { ...current.player1, name: battle.player1.playerName.startsWith('Jogador ') ? current.player1.name : battle.player1.playerName },
        player2: { ...current.player2, name: battle.player2.playerName.startsWith('Jogador ') ? current.player2.name : battle.player2.playerName },
      }))
    }
    setBattle(null)
    setError('')
    loadBoard()
  }

  return (
    <section aria-labelledby="arena-title">
      <div className="section-heading arena-heading">
        <div>
          <h1 className="section-title" id="arena-title">Arena PvP</h1>
          <p className="section-description">Dois jogadores, um dispositivo. Escolham seus Pokémon e batalhem por turnos.</p>
        </div>
        {battle && battle.status !== 'FINISHED' && (
          <button type="button" className="button button-secondary" onClick={newBattle}><Icon name="arrow-left" size={15} />Trocar Pokémon</button>
        )}
      </div>

      <div className="arena-layout">
        <div className="arena-main">
          {battle
            ? <ArenaBattle battle={battle} step={step} busy={busy} error={error} onChoose={choose} onForfeit={forfeit} onRematch={rematch} onNewBattle={newBattle} />
            : <ArenaSetup pokemon={pokemon} setup={setup} onChange={setSetup} onStart={() => startBattle()} busy={busy} error={error} />}
        </div>
        <ArenaSidebar history={board.history} ranking={board.ranking} status={board.status} currentId={battle?.id}
          onResume={resume} onRefresh={loadBoard} />
      </div>
    </section>
  )
}
