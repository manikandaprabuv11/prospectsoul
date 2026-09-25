import { createContext, useContext, useEffect, useState, useCallback } from 'react'

export type Theme = 'light' | 'dark' | 'system'

interface ThemeContextValue {
  theme: Theme
  resolved: 'light' | 'dark'
  setTheme: (t: Theme) => void
  toggle: () => void
}

const STORAGE_KEY = 'ps-theme'

function getSystemTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function resolve(theme: Theme): 'light' | 'dark' {
  return theme === 'system' ? getSystemTheme() : theme
}

function readStored(): Theme {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    if (v === 'light' || v === 'dark' || v === 'system') return v
  } catch { /* noop */ }
  return 'light'
}

export const ThemeContext = createContext<ThemeContextValue>({
  theme: 'light',
  resolved: 'light',
  setTheme: () => {},
  toggle: () => {},
})

export function useThemeState() {
  const [theme, setThemeRaw] = useState<Theme>(readStored)
  const [resolved, setResolved] = useState<'light' | 'dark'>(() => resolve(readStored()))

  const apply = useCallback((t: 'light' | 'dark') => {
    setResolved(t)
    const root = document.documentElement
    root.classList.toggle('dark', t === 'dark')
    root.setAttribute('data-theme', t)
  }, [])

  const setTheme = useCallback((t: Theme) => {
    setThemeRaw(t)
    try { localStorage.setItem(STORAGE_KEY, t) } catch { /* noop */ }
    apply(resolve(t))
  }, [apply])

  const toggle = useCallback(() => {
    setTheme(resolved === 'light' ? 'dark' : 'light')
  }, [resolved, setTheme])

  useEffect(() => {
    apply(resolve(theme))
  }, [theme, apply])

  useEffect(() => {
    if (theme !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const handler = () => apply(getSystemTheme())
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [theme, apply])

  return { theme, resolved, setTheme, toggle }
}

export function useTheme() {
  return useContext(ThemeContext)
}
