import { STAT_FIELDS, TYPELESS } from '../../data/pokedex.js'

export const ROUNDS = 5
export const POINTS_PER_ROUND = 100

export const GAMES = [
  { id: 'TYPE', icon: 'tag', title: 'Qual é o tipo?', description: 'Veja o Pokémon e escolha o tipo dele, ou os dois tipos.', rule: 'Acerto exato: 100. Cada tipo certo vale 50 e cada errado tira 25.' },
  { id: 'NUMBER', icon: 'hash', title: 'Qual é o número?', description: 'Adivinhe o número da Pokédex. Quanto mais perto, mais pontos.', rule: '100 pontos menos 2 por número de distância.' },
  { id: 'STAT', icon: 'chart', title: 'Qual é o maior atributo?', description: 'Descubra em qual atributo base o Pokémon é mais forte.', rule: '100 pontos se acertar (empates valem).' },
]

export function gameById(id) {
  return GAMES.find((game) => game.id === id)
}

/** Sorteia um Pokémon que ainda não apareceu na partida (se todos já saíram, sorteia de qualquer um). */
export function drawPokemon(pool, usedIds = [], random = Math.random) {
  const available = pool.filter((entry) => !usedIds.includes(entry.id))
  const source = available.length ? available : pool
  return source[Math.floor(random() * source.length)] ?? null
}

/** Tipos do Pokémon como nomes; sem tipo vira ['Typeless']. */
export function actualTypes(pokemon) {
  return pokemon.types.length ? pokemon.types.map((type) => type.name) : [TYPELESS]
}

/** Jogo do tipo: 100 no acerto exato; senão 50 por tipo certo − 25 por tipo errado (mínimo 0). */
export function scoreTypeGuess(selected, pokemon) {
  const actual = actualTypes(pokemon)
  const right = selected.filter((type) => actual.includes(type))
  const wrong = selected.filter((type) => !actual.includes(type))
  const missed = actual.filter((type) => !selected.includes(type))
  const correct = wrong.length === 0 && missed.length === 0
  const points = correct ? POINTS_PER_ROUND : Math.max(0, 50 * right.length - 25 * wrong.length)
  return { points, correct, right, wrong, missed, actual }
}

export function proximityHint(difference) {
  if (difference === 0) return 'Na mosca!'
  if (difference <= 5) return 'Muito perto!'
  if (difference <= 15) return 'Perto!'
  if (difference <= 40) return 'Longe…'
  return 'Muito longe!'
}

/** Jogo do número: 100 − 2 × distância (mínimo 0). */
export function scoreNumberGuess(guess, pokemon) {
  const difference = Math.abs(Number(guess) - pokemon.id)
  return {
    points: Math.max(0, POINTS_PER_ROUND - 2 * difference),
    correct: difference === 0,
    difference,
    hint: proximityHint(difference),
  }
}

/** Atributos com o maior valor (pode haver empate, como no Mew). */
export function highestStats(pokemon) {
  const best = Math.max(...STAT_FIELDS.map(({ key }) => pokemon.stats[key]))
  return STAT_FIELDS.filter(({ key }) => pokemon.stats[key] === best).map(({ key }) => key)
}

/** Jogo do maior atributo: 100 se escolher um dos maiores. */
export function scoreStatGuess(statKey, pokemon) {
  const highest = highestStats(pokemon)
  const correct = highest.includes(statKey)
  return { points: correct ? POINTS_PER_ROUND : 0, correct, highest }
}

export function maxDexNumber(pool) {
  return pool.reduce((max, entry) => Math.max(max, entry.id), 1)
}
