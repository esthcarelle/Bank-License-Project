import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../authContext'

export default function LoginPage() {
  const { login, session } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('applicant@bnr.rw')
  const [password, setPassword] = useState('tentativepassword')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (session) {
      navigate('/', { replace: true })
    }
  }, [session, navigate])

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email.trim(), password)
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-box">
      <h1>Sign in</h1>
      <p className="muted small">Bank licensing — internal use</p>
      <form onSubmit={onSubmit}>
        <label className="field">
          <span>Email</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="username"
            required
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error ? <p className="error">{error}</p> : null}
        <button type="submit" className="btn btn-primary" disabled={loading} style={{ marginTop: '0.5rem' }}>
          {loading ? 'Please wait…' : 'Continue'}
        </button>
      </form>
      <p className="note small muted">
        Demo logins: applicant@bnr.rw, reviewer@bnr.rw, approver@bnr.rw — password{' '}
        <code>tentativepassword</code>
      </p>
    </div>
  )
}
