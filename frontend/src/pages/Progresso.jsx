import { useEffect, useState } from 'react';
import api from '../api/client';
import Modal from '../components/Modal';

const EMPTY_FORM = { nome: '', descricao: '', checklist: [], materiais: [] };

function novoItemChecklist() {
  return { texto: '', concluido: false };
}

function novoMaterial() {
  return { pastilhaId: '', quantidade: 1 };
}

export default function Progresso() {
  const [projetos, setProjetos] = useState([]);
  const [pastilhas, setPastilhas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [salvando, setSalvando] = useState(false);
  const [formError, setFormError] = useState('');

  function carregar() {
    setLoading(true);
    Promise.all([api.get('/projetos/progresso'), api.get('/pastilhas')])
      .then(([p, m]) => {
        setProjetos(p.data);
        setPastilhas(m.data);
      })
      .catch(() => setError('Não foi possível carregar os projetos.'))
      .finally(() => setLoading(false));
  }

  useEffect(carregar, []);

  function abrirNovo() {
    setEditando(null);
    setForm({ nome: '', descricao: '', checklist: [novoItemChecklist()], materiais: [] });
    setFormError('');
    setModalAberto(true);
  }

  function abrirEdicao(p) {
    setEditando(p);
    setForm({
      nome: p.nome,
      descricao: p.descricao ?? '',
      checklist: p.checklist.map((c) => ({ texto: c.texto, concluido: c.concluido })),
      materiais: p.materiais.map((m) => ({ pastilhaId: String(m.pastilha.id), quantidade: m.quantidade })),
    });
    setFormError('');
    setModalAberto(true);
  }

  function atualizarItemChecklist(index, campo, valor) {
    setForm((f) => {
      const checklist = [...f.checklist];
      checklist[index] = { ...checklist[index], [campo]: valor };
      return { ...f, checklist };
    });
  }

  function removerItemChecklist(index) {
    setForm((f) => ({ ...f, checklist: f.checklist.filter((_, i) => i !== index) }));
  }

  function atualizarMaterial(index, campo, valor) {
    setForm((f) => {
      const materiais = [...f.materiais];
      materiais[index] = { ...materiais[index], [campo]: valor };
      return { ...f, materiais };
    });
  }

  function removerMaterial(index) {
    setForm((f) => ({ ...f, materiais: f.materiais.filter((_, i) => i !== index) }));
  }

  async function salvar(e) {
    e.preventDefault();
    setSalvando(true);
    setFormError('');
    const payload = {
      nome: form.nome,
      descricao: form.descricao,
      checklist: form.checklist
        .filter((c) => c.texto.trim())
        .map((c) => ({ texto: c.texto.trim(), concluido: c.concluido })),
      materiais: form.materiais
        .filter((m) => m.pastilhaId)
        .map((m) => ({ pastilhaId: Number(m.pastilhaId), quantidade: Number(m.quantidade) || 1 })),
    };
    try {
      if (editando) {
        await api.put(`/projetos/${editando.id}`, payload);
      } else {
        await api.post('/projetos', payload);
      }
      setModalAberto(false);
      carregar();
    } catch (err) {
      setFormError(err.response?.data?.message ?? 'Erro ao salvar projeto.');
    } finally {
      setSalvando(false);
    }
  }

  async function alternarChecklist(projeto, item) {
    try {
      await api.patch(`/projetos/${projeto.id}/checklist/${item.id}`, { concluido: !item.concluido });
      carregar();
    } catch {
      alert('Não foi possível atualizar o item do checklist.');
    }
  }

  async function moverStatus(projeto, novoStatus) {
    try {
      await api.patch(`/projetos/${projeto.id}/status`, { status: novoStatus });
      carregar();
    } catch (err) {
      alert(err.response?.data?.message ?? 'Não foi possível mover o projeto.');
    }
  }

  async function excluir(p) {
    if (!window.confirm(`Excluir o projeto "${p.nome}"?`)) return;
    try {
      await api.delete(`/projetos/${p.id}`);
      carregar();
    } catch {
      alert('Não foi possível excluir o projeto.');
    }
  }

  const aguardando = projetos.filter((p) => p.status === 'AGUARDANDO');
  const emProducao = projetos.filter((p) => p.status === 'EM_PRODUCAO');

  function renderCard(projeto, coluna) {
    return (
      <div key={projeto.id} className="kanban-card">
        <div className="kanban-card-header">
          <strong>{projeto.nome}</strong>
          <span className="checklist-progress">
            {projeto.concluidosChecklist}/{projeto.totalChecklist}
          </span>
        </div>
        {projeto.descricao && <p className="kanban-card-desc">{projeto.descricao}</p>}

        {projeto.materiais.length > 0 && (
          <div className="kanban-materiais">
            {projeto.materiais.map((m) => (
              <span
                key={m.id}
                className={`badge ${m.quantidade > m.pastilha.quantidadeAtual ? 'badge-danger' : 'badge-neutral'}`}
                title={`Estoque atual: ${m.pastilha.quantidadeAtual}`}
              >
                {m.pastilha.codigo} × {m.quantidade}
              </span>
            ))}
          </div>
        )}

        {projeto.checklist.length > 0 && (
          <ul className="kanban-checklist">
            {projeto.checklist.map((item) => (
              <li key={item.id}>
                <label>
                  <input
                    type="checkbox"
                    checked={item.concluido}
                    onChange={() => alternarChecklist(projeto, item)}
                  />
                  <span className={item.concluido ? 'checklist-done' : ''}>{item.texto}</span>
                </label>
              </li>
            ))}
          </ul>
        )}

        <div className="kanban-card-actions">
          <button className="btn-link" onClick={() => abrirEdicao(projeto)}>
            Editar
          </button>
          <button className="btn-link btn-link-danger" onClick={() => excluir(projeto)}>
            Excluir
          </button>
          <span className="spacer" />
          {coluna === 'AGUARDANDO' && (
            <button className="btn btn-primary btn-sm" onClick={() => moverStatus(projeto, 'EM_PRODUCAO')}>
              Iniciar produção →
            </button>
          )}
          {coluna === 'EM_PRODUCAO' && (
            <>
              <button className="btn btn-ghost btn-sm" onClick={() => moverStatus(projeto, 'AGUARDANDO')}>
                ← Voltar
              </button>
              <button className="btn btn-success btn-sm" onClick={() => moverStatus(projeto, 'FINALIZADO')}>
                Finalizar produção →
              </button>
            </>
          )}
        </div>
      </div>
    );
  }

  return (
    <div>
      <header className="page-header page-header-actions">
        <div>
          <h1>Progresso de produção</h1>
          <p>Projetos aguardando início e em produção, com checklist de etapas e materiais necessários.</p>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>
          + Novo projeto
        </button>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? (
        <p className="empty-state">Carregando...</p>
      ) : (
        <div className="kanban-board">
          <section className="kanban-column">
            <h2>Aguardando início ({aguardando.length})</h2>
            {aguardando.length === 0 ? (
              <p className="empty-state">Nenhum projeto aguardando.</p>
            ) : (
              aguardando.map((p) => renderCard(p, 'AGUARDANDO'))
            )}
          </section>

          <section className="kanban-column">
            <h2>Em produção ({emProducao.length})</h2>
            {emProducao.length === 0 ? (
              <p className="empty-state">Nenhum projeto em produção.</p>
            ) : (
              emProducao.map((p) => renderCard(p, 'EM_PRODUCAO'))
            )}
          </section>
        </div>
      )}

      {modalAberto && (
        <Modal title={editando ? 'Editar projeto' : 'Novo projeto'} onClose={() => setModalAberto(false)}>
          <form onSubmit={salvar} className="form">
            <label>
              Nome do projeto
              <input
                value={form.nome}
                onChange={(e) => setForm({ ...form, nome: e.target.value })}
                placeholder="Ex: Cerca de metal - Cliente ABC"
                required
                autoFocus
              />
            </label>
            <label>
              Descrição
              <input
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
              />
            </label>

            <div className="dynamic-field">
              <span className="permissoes-label">Materiais necessários</span>
              {form.materiais.map((m, i) => (
                <div key={i} className="dynamic-row">
                  <select
                    value={m.pastilhaId}
                    onChange={(e) => atualizarMaterial(i, 'pastilhaId', e.target.value)}
                  >
                    <option value="">Selecione a pastilha...</option>
                    {pastilhas.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.codigo} (estoque: {p.quantidadeAtual})
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    min="1"
                    className="dynamic-qty"
                    value={m.quantidade}
                    onChange={(e) => atualizarMaterial(i, 'quantidade', e.target.value)}
                  />
                  <button type="button" className="btn-icon" onClick={() => removerMaterial(i)}>
                    &times;
                  </button>
                </div>
              ))}
              <button
                type="button"
                className="btn-link"
                onClick={() => setForm((f) => ({ ...f, materiais: [...f.materiais, novoMaterial()] }))}
              >
                + Adicionar material
              </button>
            </div>

            <div className="dynamic-field">
              <span className="permissoes-label">Checklist de etapas</span>
              {form.checklist.map((item, i) => (
                <div key={i} className="dynamic-row">
                  {editando && (
                    <input
                      type="checkbox"
                      checked={item.concluido}
                      onChange={(e) => atualizarItemChecklist(i, 'concluido', e.target.checked)}
                    />
                  )}
                  <input
                    value={item.texto}
                    onChange={(e) => atualizarItemChecklist(i, 'texto', e.target.value)}
                    placeholder="Ex: Pegar material e fazer o ligamento"
                  />
                  <button type="button" className="btn-icon" onClick={() => removerItemChecklist(i)}>
                    &times;
                  </button>
                </div>
              ))}
              <button
                type="button"
                className="btn-link"
                onClick={() => setForm((f) => ({ ...f, checklist: [...f.checklist, novoItemChecklist()] }))}
              >
                + Adicionar etapa
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
