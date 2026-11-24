const paths = {
  search: <><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></>,
  star: <path d="m12 3 2.8 5.7 6.3.9-4.6 4.5 1.1 6.3-5.6-3-5.6 3 1.1-6.3L3 9.6l6.2-.9Z" />,
  users: <><circle cx="9" cy="7" r="3" /><path d="M3 21v-3a6 6 0 0 1 12 0v3M16 4a3 3 0 0 1 0 6m2 4a5 5 0 0 1 3 4v3" /></>,
  pokeball: <><circle cx="12" cy="12" r="9" /><path d="M3 12h6m6 0h6" /><circle cx="12" cy="12" r="3" /></>,
  grid: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></>,
  shuffle: <><path d="m17 3 4 4-4 4M3 7h3c5 0 7 10 12 10h3M3 17h3c1.6 0 3-1.1 4.3-3M14 9c1.3-1.3 2.5-2 4-2h3m-4 6 4 4-4 4" /></>,
  'arrow-right': <><path d="M4 12h16m-6-6 6 6-6 6" /></>,
  'arrow-left': <><path d="M20 12H4m6-6-6 6 6 6" /></>,
  'chevron-right': <path d="m9 5 7 7-7 7" />,
  x: <path d="m6 6 12 12M6 18 18 6" />,
  award: <><circle cx="12" cy="8" r="5" /><path d="m8 12-1 9 5-3 5 3-1-9" /></>,
  globe: <><circle cx="12" cy="12" r="9" /><ellipse cx="12" cy="12" rx="4" ry="9" /><path d="M3 12h18" /></>,
  book: <><path d="M12 5c-3-2-6-2-9-1v15c3-1 6-1 9 1 3-2 6-2 9-1V4c-3-1-6-1-9 1v15" /></>,
  leaf: <><path d="M20 3C9 2 3 8 5 15c1 4 8 6 12 1 3-4 3-10 3-13ZM4 21 15 10" /></>,
  filter: <><path d="M4 7h16M7 12h10m-7 5h4" /></>,
  plus: <path d="M12 5v14M5 12h14" />,
  edit: <><path d="M4 20h4L19 9l-4-4L4 16v4Z" /><path d="m13.5 6.5 4 4" /></>,
  trash: <><path d="M4 7h16M9 7V4h6v3m-8 0 1 13h8l1-13" /><path d="M10 11v6m4-6v6" /></>,
  swords: <><path d="M14.5 17.5 3 6V3h3l11.5 11.5M13 19l6-6M16 16l4 4M19 21l2-2" /><path d="M9.5 6.5 6 3H3v3l3.5 3.5M5 14l4 4M7 17l-3 3M3 19l2 2" /></>,
  shield: <path d="M12 3 5 6v6c0 4.4 3 7.6 7 9 4-1.4 7-4.6 7-9V6Z" />,
  zap: <path d="M13 2 4 14h7l-1 8 9-12h-7Z" />,
  flag: <><path d="M5 21V4" /><path d="M5 4h11l-2 4 2 4H5" /></>,
  trophy: <><path d="M8 4h8v5a4 4 0 0 1-8 0Z" /><path d="M8 6H5a3 3 0 0 0 3 4m8-4h3a3 3 0 0 1-3 4M12 13v4m-4 4h8m-6-4h4" /></>,
  refresh: <><path d="M20 11a8 8 0 0 0-14.9-3.9L4 9m0-5v5h5" /><path d="M4 13a8 8 0 0 0 14.9 3.9L20 15m0 5v-5h-5" /></>,
  gamepad: <><rect x="2" y="7" width="20" height="11" rx="5" /><path d="M7 10.5v4M5 12.5h4M15.5 11.5h.01M18 13.5h.01" /></>,
  tag: <><path d="M3 12V4h8l10 10-8 8Z" /><circle cx="7.5" cy="8.5" r="1.5" /></>,
  hash: <path d="M5 9h15M4 15h15M10 3 8 21M16 3l-2 18" />,
  chart: <path d="M4 20V10m6 10V4m6 16v-7m4 7H3" />,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" /></>,
  check: <path d="m5 12 5 5L20 7" />,
}

export default function Icon({ name, size = 20, className = '', ...props }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className={className} {...props}>
      {paths[name] || paths.pokeball}
    </svg>
  )
}
