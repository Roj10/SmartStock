import { useCallback, useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';

const NAV_ITEMS = [
  { to: '/', label: 'Painel', icon: '▦', end: true, permissao: null },
  { to: '/pastilhas', label: 'Pastilhas', icon: '●', permissao: 'PASTILHAS' },
  { to: '/movimentacoes', label: 'Movimentações', icon: '⇆', permissao: 'MOVIMENTACOES' },
  { to: '/progresso', label: 'Progresso', icon: '◧', permissao: 'PROJETOS' },
  { to: '/calendario', label: 'Calendário', icon: '◱', permissao: 'PROJETOS' },
  { to: '/fornecedores', label: 'Fornecedores', icon: '⚑', permissao: 'FORNECEDORES' },
  { to: '/financeiro', label: 'Financeiro', icon: '$', permissao: 'FINANCEIRO' },
  { to: '/mensagens', label: 'Mensagens', icon: '✉', permissao: null, badge: true },
  { to: '/usuarios', label: 'Usuários', icon: '👤', permissao: 'USUARIOS' },
];

export default function Layout() {
  const { user, logout, hasPermissao } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [naoLidas, setNaoLidas] = useState(0);
  const [aviso, setAviso] = useState(null);
  const [pulsando, setPulsando] = useState(false);

  const totalAnteriorRef = useRef(null);
  const naMensagensRef = useRef(false);
  naMensagensRef.current = location.pathname.startsWith('/mensagens');
  const tituloOriginalRef = useRef(document.title);

  const atualizarNaoLidas = useCallback(() => {
    api
      .get('/mensagens/nao-lidas')
      .then(({ data }) => {
        const anterior = totalAnteriorRef.current;
        totalAnteriorRef.current = data.total;
        setNaoLidas(data.total);
        // chegou mensagem nova: a bolinha pisca e, fora da tela de Mensagens, aparece um aviso
        if (anterior !== null && data.total > anterior) {
          setPulsando(true);
          if (!naMensagensRef.current) {
            const novas = data.total - anterior;
            setAviso({
              id: Date.now(),
              texto:
                novas === 1
                  ? `Nova mensagem de ${data.ultimoRemetente ?? 'outro setor'}`
                  : `${novas} novas mensagens (a última de ${data.ultimoRemetente ?? 'outro setor'})`,
            });
          }
        }
      })
      .catch(() => {});
  }, []);

  // contador de mensagens novas: atualiza a cada poucos segundos, ao voltar para a aba
  // e logo que a tela de Mensagens lê alguma conversa
  useEffect(() => {
    atualizarNaoLidas();
    const timer = setInterval(() => {
      if (!document.hidden) atualizarNaoLidas();
    }, 3000);
    window.addEventListener('mensagens-atualizadas', atualizarNaoLidas);
    window.addEventListener('focus', atualizarNaoLidas);
    document.addEventListener('visibilitychange', atualizarNaoLidas);
    return () => {
      clearInterval(timer);
      window.removeEventListener('mensagens-atualizadas', atualizarNaoLidas);
      window.removeEventListener('focus', atualizarNaoLidas);
      document.removeEventListener('visibilitychange', atualizarNaoLidas);
    };
  }, [atualizarNaoLidas]);

  // contador também no título da aba do navegador, ex.: "(2) SmartStock"
  useEffect(() => {
    const base = tituloOriginalRef.current;
    document.title = naoLidas > 0 ? `(${naoLidas}) ${base}` : base;
    return () => {
      document.title = base;
    };
  }, [naoLidas]);

  useEffect(() => {
    if (!pulsando) return undefined;
    const timer = setTimeout(() => setPulsando(false), 2500);
    return () => clearTimeout(timer);
  }, [pulsando]);

  useEffect(() => {
    if (!aviso) return undefined;
    const timer = setTimeout(() => setAviso(null), 7000);
    return () => clearTimeout(timer);
  }, [aviso]);

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
              {item.badge && naoLidas > 0 && (
                <span className={'nav-badge' + (pulsando ? ' nav-badge-pulso' : '')} title="Mensagens não lidas">
                  {naoLidas > 99 ? '99+' : naoLidas}
                </span>
              )}
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

      {aviso && (
        <div className="toast-mensagem" role="status" key={aviso.id}>
          <button
            type="button"
            className="toast-mensagem-corpo"
            onClick={() => {
              setAviso(null);
              navigate('/mensagens');
            }}
          >
            <span className="toast-mensagem-icone">✉</span>
            <span>
              <strong>{aviso.texto}</strong>
              <small>Clique para abrir a conversa</small>
            </span>
          </button>
          <button type="button" className="btn-icon" aria-label="Fechar aviso" onClick={() => setAviso(null)}>
            &times;
          </button>
        </div>
      )}
    </div>
  );
}
