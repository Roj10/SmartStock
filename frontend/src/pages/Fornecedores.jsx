import { useEffect, useState } from 'react';
import api from '../api/client';
import Modal from '../components/Modal';

const TIPO_LABEL = {
  FABRICANTE: 'Fabricante',
  FORNECEDOR: 'Fornecedor',
  AMBOS: 'Fabricante e fornecedor',
};

const EMPTY_FORM = { nome: '', tipo: 'FABRICANTE', contato: '', telefone: '', email: '' };

export default function Fornecedores() {
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
      .get('/fornecedores')
      .then((res) => setLista(res.data))
      .catch(() => setError('Não foi possível carregar os fornecedores.'))
      .finally(() => setLoading(false));
  }

  useEffect(carregar, []);

  function abrirNovo() {
    setEditando(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setModalAberto(true);
  }

  function abrirEdicao(f) {
    setEditando(f);
    setForm({ nome: f.nome, tipo: f.tipo, contato: f.contato ?? '', telefone: f.telefone ?? '', email: f.email ?? '' });
    setFormError('');
    setModalAberto(true);
  }

  async function salvar(e) {
    e.preventDefault();
    setSalvando(true);
    setFormError('');
    try {
      if (editando) {
        await api.put(`/fornecedores/${editando.id}`, form);
      } else {
        await api.post('/fornecedores', form);
      }
      setModalAberto(false);
      carregar();
    } catch (err) {
      setFormError(err.response?.data?.message ?? 'Erro ao salvar fornecedor.');
    } finally {
      setSalvando(false);
    }
  }

  async function excluir(f) {
    if (!window.confirm(`Excluir o fornecedor "${f.nome}"?`)) return;
    try {
      await api.delete(`/fornecedores/${f.id}`);
      carregar();
    } catch {
      alert('Não foi possível excluir. Verifique se há pastilhas vinculadas a este fornecedor.');
    }
  }

  return (
    <div>
      <header className="page-header page-header-actions">
        <div>
          <h1>Fornecedores e fabricantes</h1>
          <p>Cadastro de fabricantes e fornecedores das pastilhas industriais.</p>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>
          + Novo fornecedor
        </button>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="panel">
        {loading ? (
          <p className="empty-state">Carregando...</p>
        ) : lista.length === 0 ? (
          <p className="empty-state">Nenhum fornecedor cadastrado ainda.</p>
        ) : (
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Tipo</th>
                  <th>Contato</th>
                  <th>Telefone</th>
                  <th>E-mail</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {lista.map((f) => (
                  <tr key={f.id}>
                    <td>{f.nome}</td>
                    <td>{TIPO_LABEL[f.tipo]}</td>
                    <td>{f.contato}</td>
                    <td>{f.telefone}</td>
                    <td>{f.email}</td>
                    <td className="table-actions">
                      <button className="btn-link" onClick={() => abrirEdicao(f)}>
                        Editar
                      </button>
                      <button className="btn-link btn-link-danger" onClick={() => excluir(f)}>
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modalAberto && (
        <Modal title={editando ? 'Editar fornecedor' : 'Novo fornecedor'} onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar} className="form">
            <label>
              Nome
              <input
                value={form.nome}
                onChange={(e) => setForm({ ...form, nome: e.target.value })}
                required
                autoFocus
              />
            </label>
            <label>
              Tipo
              <select value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
                <option value="FABRICANTE">Fabricante</option>
                <option value="FORNECEDOR">Fornecedor</option>
                <option value="AMBOS">Fabricante e fornecedor</option>
              </select>
            </label>
            <label>
              Contato
              <input value={form.contato} onChange={(e) => setForm({ ...form, contato: e.target.value })} />
            </label>
            <label>
              Telefone
              <input value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
            </label>
            <label>
              E-mail
              <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>

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
