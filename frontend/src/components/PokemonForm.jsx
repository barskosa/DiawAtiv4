import { useEffect, useId, useRef, useState } from 'react'
import { MAX_TYPES, STAT_FIELDS, STAT_MAX, STAT_MIN, TYPELESS, formatId, toFormValues, typeLabels, validatePokemonForm } from '../data/pokedex.js'
import Icon from './Icon.jsx'

/**
 * Formulário de cadastro (POST /api/pokemons) e edição (PUT /api/pokemons/{id}).
 * Valida no navegador com as mesmas regras da API e exibe os erros devolvidos pelo backend.
 */
export default function PokemonForm({ pokemon, types, allPokemon, onSubmit, onClose }) {
  const isEditing = Boolean(pokemon)
  const dialogRef = useRef(null)
  const titleId = useId()
  const [values, setValues] = useState(() => toFormValues(pokemon))
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const selectableTypes = types.filter((type) => type.name !== TYPELESS)

  useEffect(() => {
    const dialog = dialogRef.current
    const previousFocus = document.activeElement
    const previousOverflow = document.body.style.overflow
    if (!dialog.open) dialog.showModal()
    document.body.style.overflow = 'hidden'

    return () => {
      dialog.close()
      document.body.style.overflow = previousOverflow
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus({ preventScroll: true })
    }
  }, [])

  function dismiss() {
    if (!isSubmitting) onClose()
  }

  function updateField(field, value) {
    setValues((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  function updateStat(key, value) {
    setValues((current) => ({ ...current, stats: { ...current.stats, [key]: value } }))
    setErrors((current) => ({ ...current, [key]: undefined }))
  }

  function toggleType(name) {
    setValues((current) => {
      if (current.types.includes(name)) return { ...current, types: current.types.filter((type) => type !== name) }
      return current.types.length < MAX_TYPES ? { ...current, types: [...current.types, name] } : current
    })
    setErrors((current) => ({ ...current, types: undefined }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError('')
    const validation = validatePokemonForm(values, { isEditing, existing: allPokemon })
    setErrors(validation)
    if (Object.keys(validation).length) {
      focusFirstError(validation)
      return
    }

    setIsSubmitting(true)
    try {
      await onSubmit(values)
    } catch (error) {
      const fieldErrors = error.fieldErrors || {}
      setErrors(fieldErrors)
      setFormError(error.message || 'Não foi possível salvar o Pokémon.')
      setIsSubmitting(false)
      focusFirstError(fieldErrors)
    }
  }

  function focusFirstError(fieldErrors) {
    const order = ['id', 'name', ...STAT_FIELDS.map(({ key }) => key), 'types']
    const first = order.find((field) => fieldErrors[field])
    if (first) dialogRef.current?.querySelector(`[data-field="${first}"]`)?.focus()
  }

  const errorId = (field) => (errors[field] ? `${titleId}-${field}-error` : undefined)

  return (
    <dialog
      ref={dialogRef}
      className="pokemon-dialog form-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => { event.preventDefault(); dismiss() }}
    >
      <button className="dialog-close icon-button" type="button" aria-label="Fechar formulário" onClick={dismiss} disabled={isSubmitting}>
        <Icon name="x" />
      </button>

      <form className="pokemon-form" onSubmit={handleSubmit} noValidate>
        <header className="form-header">
          <h2 id={titleId}>{isEditing ? `Editar ${pokemon.name} (${formatId(pokemon.id)})` : 'Cadastrar Pokémon'}</h2>
          <p>Os dados são salvos no MongoDB pela API.</p>
        </header>

        {formError && <p className="form-alert" role="alert">{formError}</p>}

        <div className="form-grid">
          <label className="form-field">
            <span>Número da Pokédex</span>
            <input
              data-field="id" type="number" inputMode="numeric" min="1" step="1"
              value={values.id} disabled={isEditing}
              placeholder={isEditing ? '' : 'Automático'}
              onChange={(event) => updateField('id', event.target.value)}
              aria-invalid={Boolean(errors.id)} aria-describedby={errorId('id')}
            />
            {errors.id && <small className="field-error" id={errorId('id')}>{errors.id}</small>}
          </label>

          <label className="form-field">
            <span>Nome *</span>
            <input
              data-field="name" type="text" maxLength={30} autoComplete="off" autoFocus
              value={values.name}
              onChange={(event) => updateField('name', event.target.value)}
              aria-invalid={Boolean(errors.name)} aria-describedby={errorId('name')} required
            />
            {errors.name && <small className="field-error" id={errorId('name')}>{errors.name}</small>}
          </label>
        </div>

        <fieldset className="form-fieldset">
          <legend>Atributos base <small>({STAT_MIN} a {STAT_MAX})</small></legend>
          <div className="form-stats">
            {STAT_FIELDS.map(({ key, label }) => (
              <label className="form-field" key={key}>
                <span>{label}</span>
                <input
                  data-field={key} type="number" inputMode="numeric" min={STAT_MIN} max={STAT_MAX} step="1"
                  value={values.stats[key]}
                  onChange={(event) => updateStat(key, event.target.value)}
                  aria-invalid={Boolean(errors[key])} aria-describedby={errorId(key)} required
                />
                {errors[key] && <small className="field-error" id={errorId(key)}>{errors[key]}</small>}
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset className="form-fieldset">
          <legend>Tipos <small>(até {MAX_TYPES}; nenhum = sem tipo)</small></legend>
          <div className="quick-filters form-types" role="group" aria-describedby={errorId('types')}>
            {selectableTypes.map(({ id, name }, index) => {
              const isSelected = values.types.includes(name)
              const isDisabled = values.types.length >= MAX_TYPES && !isSelected
              return (
                <button
                  key={id} type="button" data-field={index === 0 ? 'types' : undefined}
                  className={`filter-chip type-${name.toLowerCase()} ${isSelected ? 'is-selected' : ''}`}
                  aria-pressed={isSelected} disabled={isDisabled} onClick={() => toggleType(name)}
                >
                  {typeLabels[name] || name}
                </button>
              )
            })}
          </div>
          {errors.types && <small className="field-error" id={errorId('types')}>{errors.types}</small>}
        </fieldset>

        <footer className="form-actions">
          <button type="button" className="button button-secondary" onClick={dismiss} disabled={isSubmitting}>Cancelar</button>
          <button type="submit" className="button button-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando…' : isEditing ? 'Salvar alterações' : 'Cadastrar Pokémon'}
          </button>
        </footer>
      </form>
    </dialog>
  )
}
