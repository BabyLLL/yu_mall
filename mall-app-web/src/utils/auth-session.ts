import type { AuthSession } from '@/types/member'

export const AUTH_SESSION_KEY = 'authSession'
const CLIENT_ID_KEY = 'miniappClientId'

const isAuthSession = (value: unknown): value is AuthSession => {
  if (!value || typeof value !== 'object') return false
  const session = value as Partial<AuthSession>
  return (
    (session.type === 'member' || session.type === 'admin') &&
    typeof session.token === 'string' &&
    session.token.length > 0 &&
    typeof session.tokenHead === 'string' &&
    typeof session.clientId === 'string' &&
    session.clientId.length > 0 &&
    typeof session.sessionId === 'string' &&
    session.sessionId.length > 0
  )
}

export const getMiniappClientId = () => {
  const stored = uni.getStorageSync(CLIENT_ID_KEY)
  if (stored) return String(stored)
  const value = `miniapp-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
  uni.setStorageSync(CLIENT_ID_KEY, value)
  return value
}

export const readAuthSession = (): AuthSession | undefined => {
  const stored = uni.getStorageSync(AUTH_SESSION_KEY)
  if (!stored) {
    uni.removeStorageSync('token')
    uni.removeStorageSync('username')
    uni.removeStorageSync('password')
    return undefined
  }
  try {
    const value = typeof stored === 'string' ? JSON.parse(stored) : stored
    if (isAuthSession(value)) return value
  } catch {
    // Invalid legacy state is cleared below.
  }
  uni.removeStorageSync(AUTH_SESSION_KEY)
  uni.removeStorageSync('token')
  return undefined
}

export const writeAuthSession = (session: AuthSession) => {
  uni.setStorageSync(AUTH_SESSION_KEY, session)
  uni.removeStorageSync('token')
}

export const removeAuthSession = () => {
  uni.removeStorageSync(AUTH_SESSION_KEY)
  uni.removeStorageSync('token')
}
