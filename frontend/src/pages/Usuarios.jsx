import { useEffect, useState } from 'react';
import api from '../api/client';
import Modal from '../components/Modal';
import { useAuth } from '../context/AuthContext';

const MODULOS = [
  { value: 'PASTILHAS', label: 'Estoque (Pastilhas)' },
  { value: 'MOVIMENTACOES', label: 'Movimentações (entrada/saída)' },
  { value: 'PROJETOS', label: 'Progresso de produção e calendário' },
  { value: 'FORNECEDORES', label: 'Fornecedores' },
  { value: 'FINANCEIRO', label: 'Financeiro (valores, vendas e planos)' },
  { value: 'USUARIOS', label: 'Usuários (gerenciar contas)' },
];

const ROLE_LABEL = { ADMIN: 'Administrador', OPERADOR: 'Funcionário' };

const EMPTY_FORM = { nome: '', username: '', senha: '', role: 'OPERADOR', permissoes: [] };

export default function Usuarios() {
  const { user: usuarioLogado, isAdmin } = useAuth();
  const [lista, setLista] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [salvando, setSalvando] = useState(false);
  const [formError, setFormError] = useState('');

  function carregar() {
    setLoading(true);
    api
      .get('/usuarios')
      .then((res) => setLista(res.data))
      .catch(() => setError('Não foi possível carregar os usuários.'))
      .finally(() => setLoading(false));
  }

  useEffect(carregar, []);

  function abrirNovo() {
    setEditando(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setModalAberto(true);
  }

  function abrirEdicao(u) {
    setEditando(u);
    setForm({ nome: u.nome, username: u.username, senha: '', role: u.role, permissoes: u.permissoes ?? [] });
    setFormError('');
    setModalAberto(true);
  }

  function alternarPermissao(modulo) {
    setForm((f) => ({
      ...f,
      permissoes: f.permissoes.includes(modulo)
        ? f.permissoes.filter((m) => m !== modulo)
        : [...f.permissoes, modulo],
    }));
  }

  async function salvar(e) {
    e.preventDefault();
    setSalvando(true);
    setFormError('');
    const payload = { ...form };
    if (editando && !payload.senha) {
      delete payload.senha;
    }
    try {
      if (editando) {
        await api.put(`/usuarios/${editando.id}`, payload);
      } else {
        await api.post('/usuarios', payload);
      }
      setModalAberto(false);
      carregar();
    } catch (err) {
      setFormError(err.response?.data?.message ?? 'Erro ao salvar usuário.');
    } finally {
      setSalvando(false);
    }
  }

  async function excluir(u) {
    if (!window.confirm(`Excluir o usuário "${u.nome}"?`)) return;
    try {
      await api.delete(`/usuarios/${u.id}`);
      carregar();
    } catch (err) {
      alert(err.response?.data?.message ?? 'Não foi possível excluir este usuário.');
    }
  }

  const ehMinhaConta = (u) => u.username === usuarioLogado?.username;

  return (
    <div className="pagina-fixa">
      <header className="page-header page-header-actions">
        <div>
          <h1>Usuários e permissões</h1>
          <p>Crie contas para a equipe e controle a quais abas do sistema cada uma tem acesso.</p>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>
          + Novo usuário
        </button>
      </header>

      <div className="alert alert-info">
        Por segurança, senhas ficam armazenadas de forma criptografada — depois de criada, a senha não pode ser
        visualizada novamente, apenas redefinida.
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="panel painel-tabela">
        {loading ? (
          <p className="empty-state">Carregando...</p>
        ) : lista.length === 0 ? (
          <p className="empty-state">Nenhum usuário cadastrado.</p>
        ) : (
          <div className="table-scroll rolavel">
            <table className="table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>E-mail / usuário</th>
                  <th>Perfil</th>
                  <th>Acesso</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {lista.map((u) => (
                  <tr key={u.id}>
                    <td>{u.nome}</td>
                    <td>{u.username}</td>
                    <td>
                      <span className={`badge ${u.role === 'ADMIN' ? 'badge-success' : 'badge-neutral'}`}>
                        {ROLE_LABEL[u.role]}
                      </span>
                    </td>
                    <td>
                      {u.role === 'ADMIN' ? (
                        <span className="text-muted">Acesso total</span>
                      ) : u.permissoes?.length ? (
                        <div className="permissao-tags">
                          {u.permissoes.map((p) => (
                            <span key={p} className="badge badge-neutral">
                              {MODULOS.find((m) => m.value === p)?.label ?? p}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span className="text-muted">Sem acesso a nenhuma aba</span>
                      )}
                    </td>
                    <td className="table-actions">
                      <button className="btn-link" onClick={() => abrirEdicao(u)}>
                        Editar
                      </button>
                      {!ehMinhaConta(u) && (
                        <button className="btn-link btn-link-danger" onClick={() => excluir(u)}>
                          Excluir
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modalAberto && (
        <Modal title={editando ? 'Editar usuário' : 'Novo usuário'} onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar} className="form">
            <label>
              Nome
              <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required autoFocus />
            </label>
            <label>
              E-mail / usuário
              <input
                type="text"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                placeholder="funcionario@empresa.com"
                required
              />
            </label>
            <label>
              {editando ? 'Nova senha' : 'Senha'}
              <input
                type="password"
                value={form.senha}
                onChange={(e) => setForm({ ...form, senha: e.target.value })}
                placeholder={editando ? 'Deixe em branco para manter a senha atual' : ''}
                required={!editando}
                minLength={6}
              />
            </label>
            <label>
              Perfil
              <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                <option value="OPERADOR">Funcionário (acesso limitado)</option>
                {isAdmin && <option value="ADMIN">Administrador (acesso total)</option>}
              </select>
            </label>

            {form.role !== 'ADMIN' && (
              <div className="permissoes-field">
                <span className="permissoes-label">Abas que este usuário poderá acessar</span>
                {MODULOS.map((m) => (
                  <label key={m.value} className="checkbox-row">
                    <input
                      type="checkbox"
                      checked={form.permissoes.includes(m.value)}
                      onChange={() => alternarPermissao(m.value)}
                    />
                    {m.label}
                  </label>
                ))}
              </div>
            )}
            {form.role === 'ADMIN' && (
              <p className="form-hint">Administradores têm acesso automático a todas as abas do sistema.</p>
            )}

            {formError && <div className="alert alert-error">{formError}</div>}

            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setModalAberto(false)}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary" disabled={salvando}>
                {salvando ? 'Salvando...' : 'Salvar'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
