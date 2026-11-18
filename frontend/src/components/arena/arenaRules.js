import { typeLabels } from '../../data/pokedex.js'

// Espelha as regras exibidas na interface; os cálculos de verdade acontecem no backend.
export const ACTIONS = [
  { id: 'ATTACK', icon: 'zap', label: 'Investida', hint: 'Golpe físico, sem tipo (Ataque × Defesa).' },
  { id: 'SPECIAL', icon: 'swords', label: null, hint: 'Golpe especial do tipo do Pokémon (Ataque esp. × Defesa esp.).' },
  { id: 'DEFEND', icon: 'shield', label: 'Defender', hint: 'Age primeiro e reduz o dano à metade. Não pode repetir em seguida.' },
]

export function battleHp(baseHp) {
  return Math.floor((2 * baseHp * 50) / 100) + 60
}

export function specialMoveName(types) {
  return types?.length ? `Golpe ${typeLabels[types[0]] || types[0]}` : 'Rajada Mística'
}

export function actionLabel(action, fighter) {
  if (action === 'SPECIAL') return specialMoveName(fighter.types)
  return ACTIONS.find((item) => item.id === action)?.label ?? action
}

export function hpTone(current, max) {
  const ratio = max ? current / max : 0
  if (ratio > 0.5) return 'is-high'
  if (ratio > 0.2) return 'is-mid'
  return 'is-low'
}

export function playerLabel(name, player) {
  return name?.trim() || `Jogador ${player}`
}
