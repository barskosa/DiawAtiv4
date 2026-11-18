// Os dados vêm da API (Spring Boot + MongoDB, populado pelo Script_Pokedex.sql).
// Este módulo concentra a conversão entre os documentos da API e o formato usado pelos
// componentes da Atividade 03, além da pesquisa e da ordenação.

export const STAT_FIELDS = [
  { key: 'hp', label: 'HP', shortLabel: 'HP' },
  { key: 'attack', label: 'Ataque', shortLabel: 'ATQ' },
  { key: 'defense', label: 'Defesa', shortLabel: 'DEF' },
  { key: 'specialAttack', label: 'Ataque especial', shortLabel: 'ATQ. ESP.' },
  { key: 'specialDefense', label: 'Defesa especial', shortLabel: 'DEF. ESP.' },
  { key: 'speed', label: 'Velocidade', shortLabel: 'VEL' },
];

export const typeLabels = {
  Typeless: 'Sem tipo',
  Normal: 'Normal',
  Fire: 'Fogo',
  Water: 'Água',
  Grass: 'Planta',
  Electric: 'Elétrico',
  Ice: 'Gelo',
  Fighting: 'Lutador',
  Poison: 'Veneno',
  Ground: 'Terra',
  Flying: 'Voador',
  Psychic: 'Psíquico',
  Bug: 'Inseto',
  Rock: 'Pedra',
  Ghost: 'Fantasma',
  Dark: 'Sombrio',
  Dragon: 'Dragão',
  Steel: 'Aço',
};

export const STAT_MIN = 1;
export const STAT_MAX = 255;
export const MAX_TYPES = 2;
export const TYPELESS = 'Typeless';

/**
 * Documento da API → formato da interface.
 * { id, name, hp, ..., types: ["Grass"] } → { id, name, stats: { hp, ... }, types: [{ id: 4, name: "Grass" }] }
 */
export function toPokemonView(document, typeCatalog = []) {
  const typeIds = new Map(typeCatalog.map((type) => [type.name, type.id]));
  return {
    id: document.id,
    name: document.name,
    stats: Object.fromEntries(STAT_FIELDS.map(({ key }) => [key, Number(document[key]) || 0])),
    types: (document.types || []).map((name) => ({ id: typeIds.get(name) ?? name, name })),
  };
}

/** Formato da interface → corpo das requisições POST/PUT da API. */
export function toPokemonPayload(entry) {
  return {
    ...(entry.id ? { id: Number(entry.id) } : {}),
    name: entry.name.trim().replace(/\s+/g, ' '),
    ...Object.fromEntries(STAT_FIELDS.map(({ key }) => [key, Number(entry.stats[key])])),
    types: entry.types.map((type) => (typeof type === 'string' ? type : type.name)),
  };
}

export function sortById(list) {
  return [...list].sort((left, right) => left.id - right.id);
}

export function formatId(id) {
  return `#${String(id).padStart(3, '0')}`;
}

export function getArtwork(id) {
  return `${import.meta.env.BASE_URL}images/pokemon/${id}.png`;
}

export function normalizeSearch(text) {
  return String(text ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

export function filterPokemon(
  list,
  { query = '', selectedTypes = [], favoritesOnly = false, favorites = [], sort = 'id' } = {},
) {
  const search = normalizeSearch(query);
  const searchedId = /^#?\d+$/.test(search) ? Number(search.replace('#', '')) : null;
  const normalizedTypes = selectedTypes.map(normalizeSearch);
  const favoriteIds = new Set(favorites.map(Number));
  const statSort = STAT_FIELDS.some(({ key }) => key === sort);

  return list
    .filter((entry) => {
      const matchesQuery = !search || (searchedId !== null
        ? entry.id === searchedId
        : normalizeSearch(entry.name).includes(search));
      const matchesTypes = normalizedTypes.every((selectedType) => (
        (selectedType === 'typeless' && entry.types.length === 0)
        || entry.types.some((entryType) => normalizeSearch(entryType.name) === selectedType)
      ));
      const matchesFavorites = !favoritesOnly || favoriteIds.has(entry.id);
      return matchesQuery && matchesTypes && matchesFavorites;
    })
    .sort((left, right) => {
      if (sort === 'name') {
        return left.name.localeCompare(right.name, 'pt-BR') || left.id - right.id;
      }
      if (statSort) {
        return right.stats[sort] - left.stats[sort] || left.id - right.id;
      }
      return left.id - right.id;
    });
}

/** Valores iniciais do formulário de cadastro/edição (campos como texto, como nos inputs). */
export function toFormValues(entry) {
  return {
    id: entry ? String(entry.id) : '',
    name: entry ? entry.name : '',
    stats: Object.fromEntries(STAT_FIELDS.map(({ key }) => [key, entry ? String(entry.stats[key]) : '50'])),
    types: entry ? entry.types.map((type) => type.name) : [],
  };
}

/**
 * Validação do formulário com as mesmas regras do backend. Devolve um objeto { campo: mensagem }
 * com as chaves usadas pela API (id, name, hp, attack, ..., types).
 */
export function validatePokemonForm(values, { isEditing = false, existing = [] } = {}) {
  const errors = {};
  const name = values.name.trim().replace(/\s+/g, ' ');
  const currentId = isEditing ? Number(values.id) : null;

  if (!isEditing && values.id.trim() !== '') {
    const id = Number(values.id);
    if (!Number.isInteger(id) || id < 1) errors.id = 'Informe um número inteiro positivo ou deixe em branco.';
    else if (existing.some((entry) => entry.id === id)) errors.id = `Já existe um Pokémon com o número ${id}.`;
  }

  if (!name) errors.name = 'Informe o nome.';
  else if (name.length > 30) errors.name = 'Use no máximo 30 caracteres.';
  else {
    const duplicate = existing.find((entry) => entry.id !== currentId && normalizeSearch(entry.name) === normalizeSearch(name));
    if (duplicate) errors.name = `O nome ${duplicate.name} já pertence ao Pokémon ${formatId(duplicate.id)}.`;
  }

  for (const { key, label } of STAT_FIELDS) {
    const raw = String(values.stats[key] ?? '').trim();
    const value = Number(raw);
    if (raw === '' || !Number.isInteger(value) || value < STAT_MIN || value > STAT_MAX) {
      errors[key] = `${label}: inteiro de ${STAT_MIN} a ${STAT_MAX}.`;
    }
  }

  if (values.types.length > MAX_TYPES) errors.types = `Selecione no máximo ${MAX_TYPES} tipos.`;
  return errors;
}
