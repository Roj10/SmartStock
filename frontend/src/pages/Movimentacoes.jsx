import { useEffect, useState } from 'react';
import api from '../api/client';
import Modal from '../components/Modal';

const EMPTY_FORM = { pastilhaId: '', quantidade: 1, fornecedorId: '', observacao: '' };

export default function Movimentacoes() {
  const [lista, setLista] = useState([]);
  const [pastilhas, setPastilhas] = useState([]);
  const [fornecedores, setFornecedores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalTipo, setModalTipo] = useState(null); // 'ENTRADA' | 'SAIDA' | null
  const [form, setForm] = useState(EMPTY_FORM);
  const [salvando, setSalvando] = useState(false);
  const [formError, setFormError] = useState('');

  function carregar() {
    setLoading(true);
    Promise.all([api.get('/movimentacoes'), api.get('/pastilhas'), api.get('/fornecedores')])
      .then(([m, p, f]) => {
        setLista(m.data);
        setPastilhas(p.data);
        setFornecedores(f.data);
      })
      .catch(() => setError('Não foi possível carregar as movimentações.'))
      .finally(() => setLoading(false));
  }

  useEffect(carregar, []);

  function abrirModal(tipo) {
    setModalTipo(tipo);
    setForm(EMPTY_FORM);
    setFormError('');
  }

  async function salvar(e) {
    e.preventDefault();
    setSalvando(true);
    setFormError('');
    const payload = {
      pastilhaId: Number(form.pastilhaId),
      quantidade: Number(form.quantidade),
      fornecedorId: form.fornecedorId || null,
      observacao: form.observacao || null,
    };
    try {
      const endpoint = modalTipo === 'ENTRADA' ? '/movimentacoes/entrada' : '/movimentacoes/saida';
      await api.post(endpoint, payload);
      setModalTipo(null);
      carregar();
    } catch (err) {
      setFormError(err.response?.data?.message ?? 'Erro ao registrar movimentação.');
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div>
      <header className="page-header page-header-actions">
        <div>
          <h1>Movimentações de estoque</h1>
          <p>Registro de entradas e saídas de pastilhas industriais.</p>
        </div>
        <div className="header-actions">
          <button className="btn btn-success" onClick={() => abrirModal('ENTRADA')}>
            + Registrar entrada
          </button>
          <button className="btn btn-warning" onClick={() => abrirModal('SAIDA')}>
            − Registrar saída
          </button>
        </div>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="panel">
        {loading ? (
          <p className="empty-state">Carregando...</p>
        ) : lista.length === 0 ? (
          <p className="empty-state">Nenhuma movimentação registrada ainda.</p>
        ) : (
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Pastilha</th>
                  <th>Tipo</th>
                  <th>Quantidade</th>
                  <th>Fornecedor</th>
                  <th>Usuário</th>
                  <th>Observação</th>
                </tr>
              </thead>
              <tbody>
                {lista.map((m) => (
                  <tr key={m.id}>
                    <td>{new Date(m.dataHora).toLocaleString('pt-BR')}</td>
                    <td>
                      {m.pastilha?.codigo} <small>{m.pastilha?.descricao}</small>
                    </td>
                    <td>
                      <span className={`badge ${m.tipo === 'ENTRADA' ? 'badge-success' : 'badge-warning'}`}>
                        {m.tipo === 'ENTRADA' ? 'Entrada' : 'Saída'}
                      </span>
                    </td>
                    <td>{m.quantidade}</td>
                    <td>{m.fornecedor?.nome ?? '-'}</td>
                    <td>{m.usuario?.nome ?? '-'}</td>
                    <td>{m.observacao ?? '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modalTipo && (
        <Modal
          title={modalTipo === 'ENTRADA' ? 'Registrar entrada de estoque' : 'Registrar saída de estoque'}
          onClose={() => setModalTipo(null)}
        >
          <form onSubmit={salvar} className="form">
            <label>
              Pastilha
              <select
                value={form.pastilhaId}
                onChange={(e) => setForm({ ...form, pastilhaId: e.target.value })}
                required
                autoFocus
              >
                <option value="">Selecione...</option>
                {pastilhas.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.codigo} — {p.descricao} (atual: {p.quantidadeAtual})
                  </option>
                ))}
              </select>
            </label>
            <label>
              Quantidade
              <input
                type="number"
                min="1"
                value={form.quantidade}
                onChange={(e) => setForm({ ...form, quantidade: e.target.value })}
                required
              />
            </label>
            {modalTipo === 'ENTRADA' && (
              <label>
                Fornecedor
                <select
                  value={form.fornecedorId}
                  onChange={(e) => setForm({ ...form, fornecedorId: e.target.value })}
                >
                  <option value="">Não informado</option>
                  {fornecedores.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.nome}
                    </option>
                  ))}
                </select>
              </label>
            )}
            <label>
              Observação
              <input value={form.observacao} onChange={(e) => setForm({ ...form, observacao: e.target.value })} />
            </label>

            {formError && <div className="alert alert-error">{formError}</div>}

            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setModalTipo(null)}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary" disabled={salvando}>
                {salvando ? 'Salvando...' : 'Confirmar'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
