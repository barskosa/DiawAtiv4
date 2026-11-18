import { typeLabels } from '../../data/pokedex.js'

// Textos e cálculos exibidos na interface; as regras de verdade rodam no backend (BattleEngine).
export const ACTIONS = [
  { id: 'ATTACK', icon: 'zap', label: 'Investida', hint: 'Golpe físico, sem tipo (Ataque × Defesa).' },
  { id: 'SPECIAL', icon: 'swords', label: null, hint: 'Golpe do tipo do Pokémon (Ataque esp. × Defesa esp.).' },
  { id: 'DEFEND', icon: 'shield', label: 'Defender', hint: 'Age primeiro e reduz o dano à metade. Não repete em seguida.' },
]

export function battleHp(baseHp) {
  return baseHp + 60
}

export function specialMoveName(types) {
  return types?.length ? `Golpe ${typeLabels[types[0]] || types[0]}` : 'Rajada Mística'
}

export function hpTone(current, max) {
  const ratio = max ? current / max : 0
  if (ratio > 0.5) return 'is-high'
  if (ratio > 0.2) return 'is-mid'
  return 'is-low'
}

/** Pokémon do time que podem entrar no lugar do ativo. */
export function switchOptions(team, activeIndex) {
  return team.map((fighter, index) => ({ fighter, index }))
    .filter(({ fighter, index }) => index !== activeIndex && fighter.currentHp > 0)
}
