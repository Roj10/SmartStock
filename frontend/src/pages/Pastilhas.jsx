import { useEffect, useState } from 'react';
import api from '../api/client';
import Modal from '../components/Modal';

const EMPTY_FORM = { codigo: '', descricao: '', fabricanteId: '', estoqueMinimo: 0, quantidadeAtual: 0 };

export default function Pastilhas() {
  const [lista, setLista] = useState([]);
  const [fornecedores, setFornecedores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [salvando, setSalvando] = useState(false);
  const [formError, setFormError] = useState('');
  const [filtro, setFiltro] = useState('');

  function carregar() {
    setLoading(true);
    Promise.all([api.get('/pastilhas'), api.get('/fornecedores')])
      .then(([p, f]) => {
        setLista(p.data);
        setFornecedores(f.data);
      })
      .catch(() => setError('Não foi possível carregar as pastilhas.'))
      .finally(() => setLoading(false));
  }

  useEffect(carregar, []);

  function abrirNovo() {
    setEditando(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setModalAberto(true);
  }

  function abrirEdicao(p) {
    setEditando(p);
    setForm({
      codigo: p.codigo,
      descricao: p.descricao,
      fabricanteId: p.fabricante?.id ?? '',
      estoqueMinimo: p.estoqueMinimo,
      quantidadeAtual: p.quantidadeAtual,
    });
    setFormError('');
    setModalAberto(true);
  }

  async function salvar(e) {
    e.preventDefault();
    setSalvando(true);
    setFormError('');
    const payload = {
      ...form,
      fabricanteId: form.fabricanteId || null,
      estoqueMinimo: Number(form.estoqueMinimo),
      quantidadeAtual: Number(form.quantidadeAtual),
    };
    try {
      if (editando) {
        await api.put(`/pastilhas/${editando.id}`, payload);
      } else {
        await api.post('/pastilhas', payload);
      }
      setModalAberto(false);
      carregar();
    } catch (err) {
      setFormError(err.response?.data?.message ?? 'Erro ao salvar pastilha.');
    } finally {
      setSalvando(false);
    }
  }

  async function excluir(p) {
    if (!window.confirm(`Excluir a pastilha "${p.codigo}"?`)) return;
    try {
      await api.delete(`/pastilhas/${p.id}`);
      carregar();
    } catch {
      alert('Não foi possível excluir. Verifique se há movimentações vinculadas a esta pastilha.');
    }
  }

  const listaFiltrada = lista.filter((p) => {
    const termo = filtro.trim().toLowerCase();
    if (!termo) return true;
    return p.codigo.toLowerCase().includes(termo) || p.descricao.toLowerCase().includes(termo);
  });

  return (
    <div>
      <header className="page-header page-header-actions">
        <div>
          <h1>Pastilhas industriais</h1>
          <p>Cadastro e acompanhamento do estoque de pastilhas.</p>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>
          + Nova pastilha
        </button>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="panel">
        <input
          className="search-input"
          placeholder="Buscar por código ou descrição..."
          value={filtro}
          onChange={(e) => setFiltro(e.target.value)}
        />

        {loading ? (
          <p className="empty-state">Carregando...</p>
        ) : listaFiltrada.length === 0 ? (
          <p className="empty-state">Nenhuma pastilha encontrada.</p>
        ) : (
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Descrição</th>
                  <th>Fabricante</th>
                  <th>Estoque atual</th>
                  <th>Estoque mínimo</th>
                  <th>Situação</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {listaFiltrada.map((p) => (
                  <tr key={p.id}>
                    <td>{p.codigo}</td>
                    <td>{p.descricao}</td>
                    <td>{p.fabricante?.nome ?? '-'}</td>
                    <td>{p.quantidadeAtual}</td>
                    <td>{p.estoqueMinimo}</td>
                    <td>
                      {p.abaixoDoMinimo ? (
                        <span className="badge badge-danger">Estoque crítico</span>
                      ) : (
                        <span className="badge badge-success">Normal</span>
                      )}
                    </td>
                    <td className="table-actions">
                      <button className="btn-link" onClick={() => abrirEdicao(p)}>
                        Editar
                      </button>
                      <button className="btn-link btn-link-danger" onClick={() => excluir(p)}>
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
        <Modal title={editando ? 'Editar pastilha' : 'Nova pastilha'} onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar} className="form">
            <label>
              Código
              <input
                value={form.codigo}
                onChange={(e) => setForm({ ...form, codigo: e.target.value })}
                required
                autoFocus
              />
            </label>
            <label>
              Descrição
              <input
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
                required
              />
            </label>
            <label>
              Fabricante
              <select
                value={form.fabricanteId}
                onChange={(e) => setForm({ ...form, fabricanteId: e.target.value })}
              >
                <option value="">Não informado</option>
                {fornecedores.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.nome}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-row">
              <label>
                Estoque mínimo
                <input
                  type="number"
                  min="0"
                  value={form.estoqueMinimo}
                  onChange={(e) => setForm({ ...form, estoqueMinimo: e.target.value })}
                  required
                />
              </label>
              <label>
                Estoque atual {editando && <small>(ajuste manual)</small>}
                <input
                  type="number"
                  min="0"
                  value={form.quantidadeAtual}
                  onChange={(e) => setForm({ ...form, quantidadeAtual: e.target.value })}
                  disabled={!editando}
                />
              </label>
            </div>
            {!editando && (
              <p className="form-hint">
                Para pastilhas novas, use as telas de Movimentações para registrar a entrada inicial de estoque.
              </p>
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
