import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const NAV_ITEMS = [
  { to: '/', label: 'Painel', icon: '▦', end: true, permissao: null },
  { to: '/pastilhas', label: 'Pastilhas', icon: '●', permissao: 'PASTILHAS' },
  { to: '/movimentacoes', label: 'Movimentações', icon: '⇆', permissao: 'MOVIMENTACOES' },
  { to: '/progresso', label: 'Progresso', icon: '◧', permissao: 'PROJETOS' },
  { to: '/calendario', label: 'Calendário', icon: '◱', permissao: 'PROJETOS' },
  { to: '/fornecedores', label: 'Fornecedores', icon: '⚑', permissao: 'FORNECEDORES' },
  { to: '/usuarios', label: 'Usuários', icon: '👤', permissao: 'USUARIOS' },
];

export default function Layout() {
  const { user, logout, hasPermissao } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  const itensVisiveis = NAV_ITEMS.filter((item) => !item.permissao || hasPermissao(item.permissao));

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">SS</span>
          <div>
            <strong>SmartStock</strong>
            <small>Controle de Pastilhas</small>
          </div>
        </div>

        <nav className="nav">
          {itensVisiveis.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            <div className="avatar">{user?.nome?.[0]?.toUpperCase() ?? '?'}</div>
            <div>
              <strong>{user?.nome}</strong>
              <small>{user?.role}</small>
            </div>
          </div>
          <button className="btn btn-ghost" onClick={handleLogout}>
            Sair
          </button>
        </div>
      </aside>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
