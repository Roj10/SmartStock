import { useEffect, useMemo, useState } from 'react';
import api from '../api/client';
import Modal from '../components/Modal';
import StatCard from '../components/StatCard';
import { dataBr, moeda, urlImagem } from '../utils/format';

const ABAS = [
  { id: 'pecas', rotulo: 'Peças e valores' },
  { id: 'vendas', rotulo: 'Vendas e planos' },
  { id: 'projetos', rotulo: 'Projetos' },
];

const ETAPA = {
  AGUARDANDO: 'Aguardando início',
  EM_PRODUCAO: 'Em produção',
  EM_ESTOQUE: 'Em estoque',
  PRONTO_ENTREGA: 'Pronto para entrega',
  PEDIDO_ENVIADO: 'Pedido enviado',
  ENTREGUE: 'Entregue',
};

const EMPTY_FORM = { cliente: '', projetoId: '', descricao: '', valor: '' };

function Margem({ valor }) {
  if (valor === null || valor === undefined) return <span className="text-muted">-</span>;
  return <span className={Number(valor) >= 0 ? 'margem-positiva' : 'margem-negativa'}>{moeda(valor)}</span>;
}

function Custo({ valor, incompleto }) {
  if (valor === null || valor === undefined) return <span className="text-muted">-</span>;
  return (
    <span title={incompleto ? 'Há materiais sem preço cadastrado nos fornecedores' : undefined}>
      {moeda(valor)}
      {incompleto && ' *'}
    </span>
  );
}

export default function Financeiro() {
  const [aba, setAba] = useState('pecas');
  const [resumo, setResumo] = useState(null);
  const [pecas, setPecas] = useState([]);
  const [vendas, setVendas] = useState([]);
  const [projetos, setProjetos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filtro, setFiltro] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [salvando, setSalvando] = useState(false);
  const [formError, setFormError] = useState('');

  function carregar() {
    setLoading(true);
    Promise.all([
      api.get('/financeiro/resumo'),
      api.get('/financeiro/pecas'),
      api.get('/vendas'),
      api.get('/financeiro/projetos'),
    ])
      .then(([r, p, v, pj]) => {
        setResumo(r.data);
        setPecas(p.data);
        setVendas(v.data);
        setProjetos(pj.data);
      })
      .catch(() => setError('Não foi possível carregar o financeiro.'))
      .finally(() => setLoading(false));
  }

  useEffect(carregar, []);

  const pecasFiltradas = useMemo(() => {
    const termo = filtro.trim().toLowerCase();
    if (!termo) return pecas;
    return pecas.filter(
      (p) =>
        p.codigo.toLowerCase().includes(termo) ||
        p.descricao.toLowerCase().includes(termo) ||
        p.fornecedores.some((f) => f.fornecedor.toLowerCase().includes(termo))
    );
  }, [pecas, filtro]);

  const planos = vendas.filter((v) => v.status === 'PLANO');
  const fechadas = vendas.filter((v) => v.status === 'VENDA');

  function abrirNovo() {
    setEditando(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setModalAberto(true);
  }

  function abrirEdicao(v) {
    setEditando(v);
    setForm({
      cliente: v.cliente,
      projetoId: v.projetoId ? String(v.projetoId) : '',
      descricao: v.descricao ?? '',
      valor: String(v.valor),
    });
    setFormError('');
    setModalAberto(true);
  }

  function escolherProjeto(id) {
    const projeto = projetos.find((p) => String(p.id) === id);
    setForm((f) => ({ ...f, projetoId: id, cliente: f.cliente || projeto?.cliente || '' }));
  }

  async function salvar(e) {
    e.preventDefault();
    setSalvando(true);
    setFormError('');
    const payload = {
      cliente: form.cliente,
      projetoId: form.projetoId ? Number(form.projetoId) : null,
      descricao: form.descricao,
      valor: Number(form.valor),
    };
    try {
      if (editando) {
        await api.put(`/vendas/${editando.id}`, payload);
      } else {
        await api.post('/vendas', payload);
      }
      setModalAberto(false);
      carregar();
    } catch (err) {
      setFormError(err.response?.data?.message ?? 'Erro ao salvar.');
    } finally {
      setSalvando(false);
    }
  }

  async function fechar(v) {
    if (!window.confirm(`Fechar a venda para ${v.cliente} no valor de ${moeda(v.valor)}?`)) return;
    try {
      await api.post(`/vendas/${v.id}/fechar`);
      carregar();
    } catch (err) {
      alert(err.response?.data?.message ?? 'Não foi possível fechar a venda.');
    }
  }

  async function excluir(v) {
    if (!window.confirm(`Excluir o registro de ${v.cliente} (${moeda(v.valor)})?`)) return;
    try {
      await api.delete(`/vendas/${v.id}`);
      carregar();
    } catch {
      alert('Não foi possível excluir.');
    }
  }

  function tabelaVendas(lista, tipo) {
    return (
      <div className="table-scroll">
        <table className="table">
          <thead>
            <tr>
              <th>Cliente</th>
              <th>Projeto</th>
              <th>Descrição</th>
              <th className="num">Valor</th>
              <th className="num">Custo dos materiais</th>
              <th className="num">Margem</th>
              <th>{tipo === 'PLANO' ? 'Plano criado em' : 'Vendido em'}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {lista.map((v) => (
              <tr key={v.id}>
                <td>{v.cliente}</td>
                <td>{v.projetoNome ?? <span className="text-muted">Sem projeto</span>}</td>
                <td>{v.descricao || '-'}</td>
                <td className="num">
                  <strong>{moeda(v.valor)}</strong>
                </td>
                <td className="num">
                  <Custo valor={v.custoMateriais} incompleto={v.custoIncompleto} />
                </td>
                <td className="num">
                  <Margem valor={v.margem} />
                </td>
                <td>{dataBr(tipo === 'PLANO' ? v.dataCriacao : v.dataVenda)}</td>
                <td className="table-actions">
                  {tipo === 'PLANO' && (
                    <button className="btn btn-success btn-sm" onClick={() => fechar(v)}>
                      Fechar venda
                    </button>
                  )}
                  <button className="btn-link" onClick={() => abrirEdicao(v)}>
                    Editar
                  </button>
                  <button className="btn-link btn-link-danger" onClick={() => excluir(v)}>
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <div>
      <header className="page-header page-header-actions">
        <div>
          <h1>Financeiro</h1>
          <p>Valor das peças por fornecedor, vendas e planos feitos para os clientes, e custo dos projetos.</p>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>
          + Novo plano para cliente
        </button>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      {resumo && (
        <div className="stats-grid">
          <StatCard label="Valor em estoque" value={moeda(resumo.valorEmEstoque)} />
          <StatCard label={`Vendas fechadas (${resumo.quantidadeVendas})`} value={moeda(resumo.totalVendas)} tone="success" />
          <StatCard label={`Planos em aberto (${resumo.quantidadePlanos})`} value={moeda(resumo.totalPlanos)} />
          <StatCard
            label="Margem das vendas"
            value={moeda(resumo.margemVendas)}
            tone={Number(resumo.margemVendas) >= 0 ? 'success' : 'danger'}
          />
        </div>
      )}

      <div className="subtabs" role="tablist">
        {ABAS.map((a) => (
          <button
            key={a.id}
            type="button"
            role="tab"
            aria-selected={aba === a.id}
            className={'subtab' + (aba === a.id ? ' ativa' : '')}
            onClick={() => setAba(a.id)}
          >
            {a.rotulo}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="empty-state">Carregando...</p>
      ) : (
        <>
          {aba === 'pecas' && (
            <section className="panel">
              <input
                className="search-input"
                placeholder="Buscar por código, descrição ou fornecedor..."
                value={filtro}
                onChange={(e) => setFiltro(e.target.value)}
              />
              {pecasFiltradas.length === 0 ? (
                <p className="empty-state">Nenhuma peça encontrada.</p>
              ) : (
                <div className="table-scroll">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Imagem</th>
                        <th>Código</th>
                        <th>Descrição</th>
                        <th>Fornecedores e preços</th>
                        <th className="num">Menor preço</th>
                        <th className="num">Estoque</th>
                        <th className="num">Valor em estoque</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pecasFiltradas.map((p) => (
                        <tr key={p.id}>
                          <td>
                            {p.imagemUrl ? (
                              <img className="thumb" src={urlImagem(p.imagemUrl)} alt={p.codigo} />
                            ) : (
                              <span className="thumb thumb-placeholder">—</span>
                            )}
                          </td>
                          <td>{p.codigo}</td>
                          <td>{p.descricao}</td>
                          <td>
                            {p.fornecedores.length === 0 ? (
                              <span className="text-muted">Sem fornecedor cadastrado</span>
                            ) : (
                              <div className="forn-precos">
                                {p.fornecedores.map((f, i) => (
                                  <span key={f.fornecedorId} className={i === 0 ? 'forn-preco melhor' : 'forn-preco'}>
                                    {f.fornecedor} <strong>{moeda(f.preco)}</strong>
                                  </span>
                                ))}
                              </div>
                            )}
                          </td>
                          <td className="num">{moeda(p.menorPreco)}</td>
                          <td className="num">{p.quantidadeAtual}</td>
                          <td className="num">
                            <strong>{moeda(p.valorEmEstoque)}</strong>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <p className="form-hint nota-rodape">
                O menor preço entre os fornecedores é usado como referência para o valor em estoque e para o custo dos
                projetos. Cadastre os produtos de cada fornecedor na aba Fornecedores.
              </p>
            </section>
          )}

          {aba === 'vendas' && (
            <>
              <section className="panel secao">
                <h2>Planos feitos para clientes ({planos.length})</h2>
                {planos.length === 0 ? (
                  <p className="empty-state">Nenhum plano em aberto.</p>
                ) : (
                  tabelaVendas(planos, 'PLANO')
                )}
              </section>
              <section className="panel secao">
                <h2>Vendas ({fechadas.length})</h2>
                {fechadas.length === 0 ? (
                  <p className="empty-state">Nenhuma venda fechada ainda.</p>
                ) : (
                  tabelaVendas(fechadas, 'VENDA')
                )}
              </section>
            </>
          )}

          {aba === 'projetos' && (
            <section className="panel">
              {projetos.length === 0 ? (
                <p className="empty-state">Nenhum projeto cadastrado.</p>
              ) : (
                <div className="table-scroll">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Projeto</th>
                        <th>Cliente</th>
                        <th>Etapa</th>
                        <th className="num">Custo dos materiais</th>
                        <th className="num">Plano</th>
                        <th className="num">Venda</th>
                        <th className="num">Margem</th>
                      </tr>
                    </thead>
                    <tbody>
                      {projetos.map((p) => (
                        <tr key={p.id}>
                          <td>
                            <span className="op-badge">OP-{String(p.id).padStart(3, '0')}</span> {p.nome}
                          </td>
                          <td>{p.cliente ?? '-'}</td>
                          <td>
                            <span className="badge badge-neutral">{ETAPA[p.status]}</span>
                          </td>
                          <td className="num">
                            <Custo valor={p.custoMateriais} incompleto={p.custoIncompleto} />
                          </td>
                          <td className="num">{Number(p.valorPlano) > 0 ? moeda(p.valorPlano) : '-'}</td>
                          <td className="num">{Number(p.valorVenda) > 0 ? moeda(p.valorVenda) : '-'}</td>
                          <td className="num">
                            <Margem valor={p.margem} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <p className="form-hint nota-rodape">
                Margem = valor da venda (ou do plano, se ainda não vendido) menos o custo dos materiais. * indica
                materiais sem preço cadastrado.
              </p>
            </section>
          )}
        </>
      )}

      {modalAberto && (
        <Modal title={editando ? 'Editar plano/venda' : 'Novo plano para cliente'} onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar} className="form">
            <label>
              Projeto (opcional)
              <select value={form.projetoId} onChange={(e) => escolherProjeto(e.target.value)}>
                <option value="">Sem projeto vinculado</option>
                {projetos.map((p) => (
                  <option key={p.id} value={p.id}>
                    OP-{String(p.id).padStart(3, '0')} — {p.nome}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Cliente
              <input
                value={form.cliente}
                onChange={(e) => setForm({ ...form, cliente: e.target.value })}
                required
                autoFocus
              />
            </label>
            <label>
              Descrição do plano
              <input
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
                placeholder="Ex: Cerca sob medida com instalação"
              />
            </label>
            <label>
              Valor (R$)
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.valor}
                onChange={(e) => setForm({ ...form, valor: e.target.value })}
                required
              />
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
