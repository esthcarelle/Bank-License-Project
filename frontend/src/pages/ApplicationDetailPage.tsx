import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getApplication, uploadDocument, workflowAction } from '../api'
import { useAuth } from '../authContext'
import type { ApplicationDetail } from '../types'
import {
  applicantMayAttachFiles,
  movesYouCanOfferHere,
  type CaseMove,
} from '../workflowUi'

function labelForMove(move: CaseMove): string {
  return move
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ')
}

export default function ApplicationDetailPage() {
  const { id } = useParams()
  const appId = Number(id)
  const { session } = useAuth()
  const navigate = useNavigate()
  const [data, setData] = useState<ApplicationDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = () => {
    if (!Number.isFinite(appId)) {
      return
    }
    setError(null)
    getApplication(appId)
      .then(setData)
      .catch((e) => {
        const msg = e instanceof Error ? e.message : 'Failed to load'
        setError(msg)
        setData(null)
      })
  }

  useEffect(() => {
    load()
  }, [appId])

  const runAction = async (action: CaseMove) => {
    if (!data || !session) {
      return
    }
    const reason =
      action === 'REJECT'
        ? window.prompt('Rejection reason (required):') ?? ''
        : undefined
    if (action === 'REJECT' && !reason?.trim()) {
      setError('Rejection reason is required')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const next = await workflowAction(appId, action, data.version, reason)
      setData(next)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Action failed')
    } finally {
      setBusy(false)
    }
  }

  const onUpload = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!data || !session) {
      return
    }
    const input = (e.target as HTMLFormElement).elements.namedItem('file') as HTMLInputElement
    const file = input.files?.[0]
    if (!file) {
      setError('Choose a file (max 5MB)')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const next = await uploadDocument(appId, file)
      setData(next)
      input.value = ''
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setBusy(false)
    }
  }

  if (!session) {
    navigate('/login')
    return null
  }

  const actions = data
    ? movesYouCanOfferHere(session.role, data.state, {
        yourEmail: session.email,
        reviewerEmailIfAny: data.reviewedByEmail,
      })
    : null

  return (
    <>
      <p className="small" style={{ marginBottom: '0.75rem' }}>
        <Link to="/">← All applications</Link>
      </p>

      {error ? <p className="error">{error}</p> : null}
      {!data && !error ? <p className="muted small">Loading.</p> : null}

      {data ? (
        <>
          <h2 style={{ border: 'none', padding: 0, marginBottom: '0.25rem' }}>{data.institutionName}</h2>
          <p className="meta-line">
            <span className="tag">{data.state.replace(/_/g, ' ')}</span>
            Row version {data.version}
          </p>
          <p className="meta-line muted small">
            Applicant: {data.applicantName} ({data.applicantEmail})
            {data.reviewedByEmail ? (
              <>
                <br />
                Review filed by: {data.reviewedByEmail}
              </>
            ) : null}
          </p>

          {data.lastRejectionReason ? (
            <div className="warn">
              <strong>Rejected:</strong> {data.lastRejectionReason}
            </div>
          ) : null}

          <hr className="divider" />

          <h3>Next step</h3>
          {actions && actions.length > 0 ? (
            <div className="btn-stack">
              {actions.map((a) => (
                <button
                  key={a}
                  type="button"
                  className="btn"
                  disabled={busy || data.state === 'APPROVED' || data.state === 'REJECTED'}
                  onClick={() => runAction(a)}
                >
                  {labelForMove(a)}
                </button>
              ))}
            </div>
          ) : (
            <p className="muted small">Nothing to do here for your role right now.</p>
          )}

          {applicantMayAttachFiles(session.role, data.state) ? (
            <>
              <h3>Add a file</h3>
              <form onSubmit={onUpload} style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}>
                <input type="file" name="file" />
                <button type="submit" className="btn" disabled={busy}>
                  Upload
                </button>
              </form>
              <p className="small muted">Max 5 MB. Earlier uploads stay on record.</p>
            </>
          ) : null}

          <hr className="divider" />

          <h3>Files on record</h3>
          {data.documents.length === 0 ? (
            <p className="muted small">None yet.</p>
          ) : (
            <ul className="row-list" style={{ borderTop: 'none' }}>
              {data.documents.map((d) => (
                <li key={d.id} style={{ borderBottom: '1px solid #ddd', padding: '0.5rem 0' }}>
                  <span className="tag">rev {d.revision}</span>
                  {d.originalFilename}
                  <span className="muted small" style={{ display: 'block', marginTop: '0.2rem' }}>
                    {(d.sizeBytes / 1024).toFixed(1)} KB · {d.uploadedByEmail}
                  </span>
                </li>
              ))}
            </ul>
          )}

          <hr className="divider" />

          <h3>Activity log</h3>
          {data.auditTrail.length === 0 ? (
            <p className="muted small">No entries.</p>
          ) : (
            <ol className="audit-list">
              {data.auditTrail.map((e) => (
                <li key={e.id}>
                  <strong>{e.action}</strong> — {e.actorEmail}
                  <span className="muted">
                    {' '}
                    ({e.stateBefore} → {e.stateAfter}) · {new Date(e.createdAt).toLocaleString()}
                  </span>
                  {e.detailsJson ? <pre>{e.detailsJson}</pre> : null}
                </li>
              ))}
            </ol>
          )}
        </>
      ) : null}
    </>
  )
}
