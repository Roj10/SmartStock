import { useEffect, useMemo, useState } from 'react';
import api from '../api/client';
import Modal from '../components/Modal';
import { moeda } from '../utils/format';

const TIPO_LABEL = {
  FABRICANTE: 'Fabricante',
  FORNECEDOR: 'Fornecedor',
  AMBOS: 'Fabricante e fornecedor',
};

const EMPTY_FORM = { nome: '', tipo: 'FABRICANTE', contato: '', telefone: '', email: '' };

function novoProduto() {
  return { pastilhaId: '', preco: '' };
}

export default function Fornecedores() {
  const [lista, setLista] = useState([]);
  const [vinculos, setVinculos] = useState([]);
  const [pastilhas, setPastilhas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [produtos, setProdutos] = useState([]);
  const [salvando, setSalvando] = useState(false);
  const [formError, setFormError] = useState('');

  function carregar() {
    setLoading(true);
    Promise.all([api.get('/fornecedores'), api.get('/fornecedores/produtos'), api.get('/pastilhas')])
      .then(([f, v, p]) => {
        setLista(f.data);
        setVinculos(v.data);
        setPastilhas(p.data);
      })
      .catch(() => setError('Não foi possível carregar os fornecedores.'))
      .finally(() => setLoading(false));
  }

  useEffect(carregar, []);

  const produtosPorFornecedor = useMemo(() => {
    const mapa = new Map();
    vinculos.forEach((v) => {
      const id = v.fornecedor.id;
      if (!mapa.has(id)) mapa.set(id, []);
      mapa.get(id).push(v);
    });
    return mapa;
  }, [vinculos]);

  function abrirNovo() {
    setEditando(null);
    setForm(EMPTY_FORM);
    setProdutos([]);
    setFormError('');
    setModalAberto(true);
  }

  function abrirEdicao(f) {
    setEditando(f);
    setForm({ nome: f.nome, tipo: f.tipo, contato: f.contato ?? '', telefone: f.telefone ?? '', email: f.email ?? '' });
    setProdutos(
      (produtosPorFornecedor.get(f.id) ?? []).map((v) => ({ pastilhaId: String(v.pastilha.id), preco: String(v.preco) }))
    );
    setFormError('');
    setModalAberto(true);
  }

  function atualizarProduto(index, campo, valor) {
    setProdutos((ps) => ps.map((p, i) => (i === index ? { ...p, [campo]: valor } : p)));
  }

  async function salvar(e) {
    e.preventDefault();
    setSalvando(true);
    setFormError('');
    try {
      let id = editando?.id;
      if (editando) {
        await api.put(`/fornecedores/${editando.id}`, form);
      } else {
        const { data } = await api.post('/fornecedores', form);
        id = data.id;
      }
      await api.put(`/fornecedores/${id}/produtos`, {
        produtos: produtos
          .filter((p) => p.pastilhaId)
          .map((p) => ({ pastilhaId: Number(p.pastilhaId), preco: Number(p.preco) || 0 })),
      });
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
    <div className="pagina-fixa">
      <header className="page-header page-header-actions">
        <div>
          <h1>Fornecedores e fabricantes</h1>
          <p>Cadastro de fabricantes e fornecedores, com os produtos que cada um entrega e o preço praticado.</p>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>
          + Novo fornecedor
        </button>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="panel painel-tabela">
        {loading ? (
          <p className="empty-state">Carregando...</p>
        ) : lista.length === 0 ? (
          <p className="empty-state">Nenhum fornecedor cadastrado ainda.</p>
        ) : (
          <div className="table-scroll rolavel">
            <table className="table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Tipo</th>
                  <th>Produtos fornecidos</th>
                  <th>Contato</th>
                  <th>Telefone</th>
                  <th>E-mail</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {lista.map((f) => {
                  const itens = produtosPorFornecedor.get(f.id) ?? [];
                  return (
                    <tr key={f.id}>
                      <td>{f.nome}</td>
                      <td>{TIPO_LABEL[f.tipo]}</td>
                      <td>
                        {itens.length === 0 ? (
                          <span className="text-muted">Nenhum produto</span>
                        ) : (
                          <div className="permissao-tags">
                            {itens.map((v) => (
                              <span key={v.id} className="badge badge-neutral" title={v.pastilha.descricao}>
                                {v.pastilha.codigo} · {moeda(v.preco)}
                              </span>
                            ))}
                          </div>
                        )}
                      </td>
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
                  );
                })}
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

            <div className="dynamic-field">
              <span className="permissoes-label">Produtos que este fornecedor entrega</span>
              {produtos.map((p, i) => (
                <div key={i} className="dynamic-row">
                  <select value={p.pastilhaId} onChange={(e) => atualizarProduto(i, 'pastilhaId', e.target.value)}>
                    <option value="">Selecione a peça...</option>
                    {pastilhas.map((ps) => (
                      <option key={ps.id} value={ps.id}>
                        {ps.codigo} — {ps.descricao}
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    className="dynamic-preco"
                    placeholder="Preço (R$)"
                    value={p.preco}
                    onChange={(e) => atualizarProduto(i, 'preco', e.target.value)}
                    aria-label="Preço unitário"
                  />
                  <button
                    type="button"
                    className="btn-icon"
                    onClick={() => setProdutos((ps) => ps.filter((_, idx) => idx !== i))}
                    aria-label="Remover produto"
                  >
                    &times;
                  </button>
                </div>
              ))}
              <button type="button" className="btn-link" onClick={() => setProdutos((ps) => [...ps, novoProduto()])}>
                + Adicionar produto
              </button>
            </div>

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
