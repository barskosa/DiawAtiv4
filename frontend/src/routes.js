// Rotas da aplicação na URL (hash), para que recarregar mantenha a página, os botões voltar e
// avançar do navegador funcionem e seja possível compartilhar um link direto (ex.: #/batalhas).
// O hash funciona em qualquer servidor estático, sem configuração extra.

export const PAGES = {
  pokedex: { slug: '', title: 'Pokédex' },
  favorites: { slug: 'favoritos', title: 'Favoritos' },
  trainers: { slug: 'treinadores', title: 'Treinadores' },
  battles: { slug: 'batalhas', title: 'Batalhas' },
  minigames: { slug: 'minigames', title: 'Minigames' },
}

/**
 * Página correspondente ao hash. Hashes que não são rotas (âncoras como #conteudo) devolvem null,
 * para não trocar de página; rotas desconhecidas levam à Pokédex.
 */
export function pageFromHash(hash) {
  const value = String(hash ?? '')
  if (value === '' || value === '#') return 'pokedex'
  if (!value.startsWith('#/')) return null
  const slug = decodeURIComponent(value.slice(2)).replace(/\/+$/, '').toLowerCase()
  return Object.keys(PAGES).find((page) => PAGES[page].slug === slug) ?? 'pokedex'
}

export function hashForPage(page) {
  return `#/${PAGES[page]?.slug ?? ''}`
}

export function documentTitle(page) {
  const title = PAGES[page]?.title ?? 'Pokédex'
  return page === 'pokedex' ? 'Pokédex — 9 gerações' : `${title} · Pokédex`
}
