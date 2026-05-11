import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './authContext'
import ApplicationDetailPage from './pages/ApplicationDetailPage'
import ApplicationListPage from './pages/ApplicationListPage'
import LoginPage from './pages/LoginPage'

function Protected({ children }: { children: React.ReactNode }) {
  const { session } = useAuth()
  if (!session) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

function Layout({ children }: { children: React.ReactNode }) {
  const { session, logout } = useAuth()
  return (
    <div className="shell">
      <header className="bar">
        <Link to={session ? '/' : '/login'} className="bar-title" style={{ textDecoration: 'none', color: 'inherit' }}>
          Licensing portal
        </Link>
        {session ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <span className="bar-meta">
              {session.fullName || session.email} · {session.role.replace(/_/g, ' ')}
            </span>
            <button type="button" className="btn btn-quiet" onClick={logout}>
              Sign out
            </button>
          </div>
        ) : null}
      </header>
      <main className="page">{children}</main>
    </div>
  )
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <Protected>
              <ApplicationListPage />
            </Protected>
          }
        />
        <Route
          path="/applications/:id"
          element={
            <Protected>
              <ApplicationDetailPage />
            </Protected>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}
