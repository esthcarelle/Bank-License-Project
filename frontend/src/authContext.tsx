import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import type { UserRole } from './types'
import { getStoredToken, login as apiLogin, setStoredToken } from './api'

export interface Session {
  token: string
  email: string
  fullName: string
  role: UserRole
}

const AuthContext = createContext<{
  session: Session | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
} | null>(null)

function parseJwtPayload(token: string): { role?: UserRole; sub?: string } {
  try {
    const part = token.split('.')[1]
    const json = atob(part.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json) as { role?: UserRole; sub?: string }
  } catch {
    return {}
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => {
    const t = getStoredToken()
    if (!t) {
      return null
    }
    const p = parseJwtPayload(t)
    if (!p.role || !p.sub) {
      return null
    }
    return {
      token: t,
      email: p.sub,
      fullName: '',
      role: p.role,
    }
  })

  const login = async (email: string, password: string) => {
    const res = await apiLogin(email, password)
    setStoredToken(res.token)
    setSession({
      token: res.token,
      email: res.email,
      fullName: res.fullName,
      role: res.role,
    })
  }

  const logout = () => {
    setStoredToken(null)
    setSession(null)
  }

  const value = useMemo(
    () => ({
      session,
      login,
      logout,
    }),
    [session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth outside AuthProvider')
  }
  return ctx
}
