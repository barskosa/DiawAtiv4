import { typeLabels } from '../data/pokedex.js'

export default function TypeBadge({ type }) {
  return (
    <span className={`type-badge type-${type.name.toLowerCase()}`} title={type.name}>
      {typeLabels[type.name] || type.name}
    </span>
  )
}
