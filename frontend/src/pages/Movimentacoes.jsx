import { useEffect, useState } from 'react';
import api from '../api/client';
import Modal from '../components/Modal';

const EMPTY_FORM = { pastilhaId: '', quantidade: 1, fornecedorId: '', observacao: '' };

const LIMITE_VISIVEL = 30;

const COLUNAS = [
  { tipo: 'ENTRADA', titulo: 'Entradas', badge: 'badge-success', vazio: 'Nenhuma entrada registrada ainda.' },
  { tipo: 'SAIDA', titulo: 'Saídas', badge: 'badge-warning', vazio: 'Nenhuma saída registrada ainda.' },
];

function novaLinha(fornecedorId = '') {
  return { pastilhaId: '', quantidade: 1, fornecedorId };
}

export default function Movimentacoes() {
  const [lista, setLista] = useState([]);
  const [pastilhas, setPastilhas] = useState([]);
  const [fornecedores, setFornecedores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalTipo, setModalTipo] = useState(null); // 'ENTRADA' | 'SAIDA' | null
  const [form, setForm] = useState(EMPTY_FORM);
  const [linhas, setLinhas] = useState([novaLinha()]);
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
    setLinhas([novaLinha()]);
    setFormError('');
  }

  function atualizarLinha(index, campo, valor) {
    setLinhas((ls) => ls.map((l, i) => (i === index ? { ...l, [campo]: valor } : l)));
  }

  function adicionarLinha() {
    // a nova linha herda o fornecedor da anterior: numa compra geral costuma ser o mesmo
    setLinhas((ls) => [...ls, novaLinha(ls[ls.length - 1]?.fornecedorId ?? '')]);
  }

  function removerLinha(index) {
    setLinhas((ls) => (ls.length === 1 ? ls : ls.filter((_, i) => i !== index)));
  }

  async function salvar(e) {
    e.preventDefault();
    setSalvando(true);
    setFormError('');

    if (modalTipo === 'ENTRADA') {
      const itens = linhas
        .filter((l) => l.pastilhaId)
        .map((l) => ({
          pastilhaId: Number(l.pastilhaId),
          quantidade: Number(l.quantidade),
          fornecedorId: l.fornecedorId || null,
        }));
      if (itens.length === 0) {
        setFormError('Selecione ao menos uma pastilha.');
        setSalvando(false);
        return;
      }
      try {
        await api.post('/movimentacoes/entrada/lote', { observacao: form.observacao || null, itens });
        setModalTipo(null);
        carregar();
      } catch (err) {
        setFormError(err.response?.data?.message ?? 'Erro ao registrar a entrada.');
      } finally {
        setSalvando(false);
      }
      return;
    }

    const payload = {
      pastilhaId: Number(form.pastilhaId),
      quantidade: Number(form.quantidade),
      fornecedorId: form.fornecedorId || null,
      observacao: form.observacao || null,
    };
    try {
      await api.post('/movimentacoes/saida', payload);
      setModalTipo(null);
      carregar();
    } catch (err) {
      setFormError(err.response?.data?.message ?? 'Erro ao registrar movimentação.');
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="pagina-fixa">
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

      {loading ? (
        <section className="panel">
          <p className="empty-state">Carregando...</p>
        </section>
      ) : (
        <div className="mov-colunas">
          {COLUNAS.map((col) => {
            const itens = lista.filter((m) => m.tipo === col.tipo);
            return (
              <section key={col.tipo} className="panel mov-coluna">
                <div className="mov-coluna-header">
                  <h2>
                    <span className={`badge ${col.badge}`}>{col.titulo}</span>
                  </h2>
                  <small className="text-muted">
                    {itens.length > LIMITE_VISIVEL
                      ? `Mostrando as ${LIMITE_VISIVEL} mais recentes de ${itens.length}`
                      : `${itens.length} registro(s)`}
                  </small>
                </div>
                {itens.length === 0 ? (
                  <p className="empty-state">{col.vazio}</p>
                ) : (
                  <div className="table-scroll rolavel">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Data</th>
                          <th>Pastilha</th>
                          <th>Qtd.</th>
                          <th>{col.tipo === 'ENTRADA' ? 'Fornecedor' : 'Usuário'}</th>
                          <th>Observação</th>
                        </tr>
                      </thead>
                      <tbody>
                        {itens.slice(0, LIMITE_VISIVEL).map((m) => (
                          <tr key={m.id}>
                            <td>{new Date(m.dataHora).toLocaleString('pt-BR')}</td>
                            <td>
                              {m.pastilha?.codigo} <small>{m.pastilha?.descricao}</small>
                            </td>
                            <td>{m.quantidade}</td>
                            <td>{col.tipo === 'ENTRADA' ? (m.fornecedor?.nome ?? '-') : (m.usuario?.nome ?? '-')}</td>
                            <td>{m.observacao ?? '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            );
          })}
        </div>
      )}

      {modalTipo && (
        <Modal
          title={modalTipo === 'ENTRADA' ? 'Registrar entrada de estoque' : 'Registrar saída de estoque'}
          onClose={() => setModalTipo(null)}
          largo={modalTipo === 'ENTRADA'}
        >
          <form onSubmit={salvar} className="form">
            {modalTipo === 'ENTRADA' ? (
              <div className="dynamic-field">
                <span className="permissoes-label">Pastilhas desta entrada (ex.: uma compra geral)</span>
                <div className="entrada-linha entrada-cabecalho">
                  <span>Pastilha</span>
                  <span>Qtd.</span>
                  <span>Fornecedor</span>
                  <span />
                </div>
                {linhas.map((linha, i) => (
                  <div key={i} className="entrada-linha">
                    <select
                      value={linha.pastilhaId}
                      onChange={(e) => atualizarLinha(i, 'pastilhaId', e.target.value)}
                      aria-label="Pastilha"
                    >
                      <option value="">Selecione...</option>
                      {pastilhas.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.codigo} (atual: {p.quantidadeAtual})
                        </option>
                      ))}
                    </select>
                    <input
                      type="number"
                      min="1"
                      value={linha.quantidade}
                      onChange={(e) => atualizarLinha(i, 'quantidade', e.target.value)}
                      aria-label="Quantidade"
                    />
                    <select
                      value={linha.fornecedorId}
                      onChange={(e) => atualizarLinha(i, 'fornecedorId', e.target.value)}
                      aria-label="Fornecedor"
                    >
                      <option value="">Não informado</option>
                      {fornecedores.map((f) => (
                        <option key={f.id} value={f.id}>
                          {f.nome}
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      className="btn-icon"
                      onClick={() => removerLinha(i)}
                      disabled={linhas.length === 1}
                      aria-label="Remover linha"
                    >
                      &times;
                    </button>
                  </div>
                ))}
                <button type="button" className="btn-link" onClick={adicionarLinha}>
                  + Adicionar outra pastilha
                </button>
                <small className="text-muted">
                  {linhas.filter((l) => l.pastilhaId).length} pastilha(s) ·{' '}
                  {linhas.filter((l) => l.pastilhaId).reduce((soma, l) => soma + (Number(l.quantidade) || 0), 0)}{' '}
                  unidade(s) no total
                </small>
              </div>
            ) : (
              <>
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
              </>
            )}
            <label>
              Observação
              <input
                value={form.observacao}
                onChange={(e) => setForm({ ...form, observacao: e.target.value })}
                placeholder={modalTipo === 'ENTRADA' ? 'Ex: Compra geral - NF 1234' : ''}
              />
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
