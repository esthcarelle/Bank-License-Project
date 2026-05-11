import type { ApplicationDetail, ApplicationSummary, LoginResponse } from './types'

const TOKEN_KEY = 'bnr_token'

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setStoredToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

async function request<T>(
  path: string,
  options: RequestInit & { json?: unknown } = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getStoredToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  let body = options.body
  if (options.json !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.json)
  }
  const res = await fetch(path, { ...options, headers, body })
  if (!res.ok) {
    let msg = res.statusText
    try {
      const j = await res.json()
      if (j?.message) {
        msg = j.message
      }
    } catch {
      /* ignore */
    }
    throw new Error(msg || `HTTP ${res.status}`)
  }
  if (res.status === 204) {
    return undefined as T
  }
  const ct = res.headers.get('content-type')
  if (ct?.includes('application/json')) {
    return (await res.json()) as T
  }
  return undefined as T
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    json: { email, password },
  })
}

export async function listApplications(): Promise<ApplicationSummary[]> {
  return request<ApplicationSummary[]>('/api/applications')
}

export async function getApplication(id: number): Promise<ApplicationDetail> {
  return request<ApplicationDetail>(`/api/applications/${id}`)
}

export async function createApplication(institutionName: string): Promise<ApplicationDetail> {
  return request<ApplicationDetail>('/api/applications', {
    method: 'POST',
    json: { institutionName },
  })
}

export async function workflowAction(
  id: number,
  action: string,
  expectedVersion: number,
  rejectionReason?: string,
): Promise<ApplicationDetail> {
  return request<ApplicationDetail>(`/api/applications/${id}/workflow`, {
    method: 'POST',
    json: { action, expectedVersion, rejectionReason },
  })
}

export async function uploadDocument(id: number, file: File): Promise<ApplicationDetail> {
  const fd = new FormData()
  fd.append('file', file)
  const headers = new Headers()
  const token = getStoredToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const res = await fetch(`/api/applications/${id}/documents`, {
    method: 'POST',
    headers,
    body: fd,
  })
  if (!res.ok) {
    let msg = res.statusText
    try {
      const j = await res.json()
      if (j?.message) {
        msg = j.message
      }
    } catch {
      /* ignore */
    }
    throw new Error(msg || `HTTP ${res.status}`)
  }
  return (await res.json()) as ApplicationDetail
}
