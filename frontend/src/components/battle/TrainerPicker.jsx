import Icon from '../Icon.jsx'
import PokemonImage from '../PokemonImage.jsx'

function getInitials(name) {
  return name.split(' ').slice(0, 2).map((part) => part[0]).join('')
}

function record(stats) {
  if (!stats) return 'Nunca desafiado'
  return `${stats.playerWins} ${stats.playerWins === 1 ? 'vitória' : 'vitórias'} · ${stats.trainerWins} ${stats.trainerWins === 1 ? 'derrota' : 'derrotas'}`
}

export default function TrainerPicker({ trainers, pokemon, stats, selectedId, onSelect, playerName, onPlayerNameChange, onChallenge, busy, error }) {
  const pokemonById = new Map(pokemon.map((entry) => [entry.id, entry]))
  const selected = trainers.find((trainer) => trainer.id === selectedId)
  const teamSize = selected?.pokemonIds.length ?? 0

  return (
    <div className="battle-picker">
      <div className="battle-picker-bar">
        <label className="form-field battle-name-field">
          <span>Seu nome de treinador</span>
          <input id="battle-player-name" type="text" maxLength={20} placeholder="Você" value={playerName}
            onChange={(event) => onPlayerNameChange(event.target.value)} />
        </label>
        <div className="battle-picker-cta">
          <p>{selected
            ? <>Você vai receber <strong>{teamSize} Pokémon {teamSize === 1 ? 'sorteado' : 'sorteados'}</strong>, o mesmo número da equipe de {selected.name}.</>
            : 'Escolha um treinador para desafiar.'}</p>
          <button type="button" className="button button-primary" onClick={onChallenge} disabled={!selected || teamSize === 0 || busy}>
            <Icon name="swords" size={17} />{busy ? 'Sorteando seu time…' : selected ? `Desafiar ${selected.name}` : 'Desafiar'}
          </button>
        </div>
      </div>

      {error && <p className="form-alert" role="alert">{error}</p>}

      <div className="battle-trainer-grid" role="radiogroup" aria-label="Treinadores para desafiar">
        {trainers.map((trainer) => {
          const isSelected = trainer.id === selectedId
          const trainerStats = stats.find((entry) => entry.trainerId === trainer.id)
          const empty = trainer.pokemonIds.length === 0
          return (
            <button key={trainer.id} type="button" role="radio" aria-checked={isSelected} disabled={empty}
              className={`battle-trainer-card${isSelected ? ' is-selected' : ''}`} onClick={() => onSelect(trainer.id)}>
              <span className="battle-trainer-head">
                <span className="trainer-avatar" aria-hidden="true">{getInitials(trainer.name)}</span>
                <span className="battle-trainer-meta">
                  <strong>{trainer.name}</strong>
                  <small><Icon name="award" size={13} />{trainer.badges} {trainer.badges === 1 ? 'insígnia' : 'insígnias'} · {trainer.pokemonIds.length} Pokémon</small>
                </span>
                {trainerStats?.playerWins > 0 && <span className="battle-badge-won" title="Você já venceu este treinador"><Icon name="trophy" size={14} /></span>}
              </span>
              <span className="battle-trainer-team" aria-hidden="true">
                {trainer.pokemonIds.map((id, index) => {
                  const member = pokemonById.get(id)
                  return member ? <PokemonImage key={`${id}-${index}`} pokemon={member} className="battle-team-thumb" alt="" /> : null
                })}
              </span>
              <span className="battle-trainer-record">{empty ? 'Sem Pokémon para batalhar' : record(trainerStats)}</span>
            </button>
          )
        })}
      </div>

      <details className="battle-rules">
        <summary>Como funciona o desafio</summary>
        <ul>
          <li>Seu time é <strong>sorteado</strong> da Pokédex com o mesmo número de Pokémon da equipe do treinador. Antes da primeira rodada, dá para sortear de novo.</li>
          <li><strong>Investida</strong>: golpe físico neutro. <strong>Golpe especial</strong>: usa o tipo principal do Pokémon e pode ser super efetivo (2x/4x), pouco efetivo ou não ter efeito.</li>
          <li><strong>Defender</strong> age primeiro e corta o dano pela metade (não repete em seguida). <strong>Trocar</strong> coloca outro Pokémon em campo e gasta a rodada.</li>
          <li>O mais rápido ataca primeiro. Quando um Pokémon desmaia, o próximo do time entra. Vence quem derrotar o time inteiro do adversário.</li>
          <li>O treinador escolhe sempre o golpe que causa mais dano e se defende quando está com pouco HP. Tudo é calculado pela API e salvo no MongoDB.</li>
        </ul>
      </details>
    </div>
  )
}
