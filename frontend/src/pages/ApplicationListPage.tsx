import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { createApplication, listApplications } from '../api'
import { useAuth } from '../authContext'
import type { ApplicationSummary } from '../types'
import { applicantMayOpenNewCase } from '../workflowUi'

export default function ApplicationListPage() {
  const { session } = useAuth()
  const [items, setItems] = useState<ApplicationSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')

  const load = () => {
    setError(null)
    listApplications()
      .then(setItems)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load'))
  }

  useEffect(() => {
    load()
  }, [])

  const onCreate = async (e: FormEvent) => {
    e.preventDefault()
    if (!session || !newName.trim()) {
      return
    }
    setCreating(true)
    setError(null)
    try {
      await createApplication(newName.trim())
      setNewName('')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed')
    } finally {
      setCreating(false)
    }
  }

  return (
    <>
      <h2>Applications</h2>

      {session && applicantMayOpenNewCase(session.role) ? (
        <form onSubmit={onCreate} className="inline-form">
          <label className="field" style={{ flex: 1, marginBottom: 0, minWidth: '12rem' }}>
            <span>New institution name</span>
            <input
              type="text"
              placeholder="e.g. Example Bank Ltd"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
            />
          </label>
          <button type="submit" className="btn btn-primary" disabled={creating}>
            {creating ? '…' : 'Submit'}
          </button>
        </form>
      ) : null}

      {error ? <p className="error">{error}</p> : null}

      {items === null ? <p className="muted small">Loading.</p> : null}

      {items && items.length === 0 ? (
        <p className="muted small">No applications yet.</p>
      ) : null}

      {items && items.length > 0 ? (
        <ul className="row-list">
          {items.map((a) => (
            <li key={a.id}>
              <Link to={`/applications/${a.id}`}>
                <div className="inst">{a.institutionName}</div>
                <div className="sub">
                  <span className="tag">{a.state.replace(/_/g, ' ')}</span>
                  #{a.id} · {a.applicantEmail}
                </div>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </>
  )
}
