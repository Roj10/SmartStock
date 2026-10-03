import { useEffect, useMemo, useRef, useState } from 'react';
import api from '../api/client';
import ImagemDropzone from '../components/ImagemDropzone';
import Modal from '../components/Modal';
import VisualizadorImagem from '../components/VisualizadorImagem';

const API_ORIGIN = 'http://localhost:8080';
const EMPTY_FORM = { codigo: '', descricao: '', fabricanteId: '', estoqueMinimo: 0, quantidadeAtual: 0 };
const PALAVRAS_GALERIA = ['imagem', 'imagens'];

function urlImagem(imagemUrl) {
  return imagemUrl ? `${API_ORIGIN}${imagemUrl}` : null;
}

// Barra de estoque: mostra a quantidade atual em relação ao dobro do mínimo (cheia = folga confortável).
function percentualEstoque(p) {
  const referencia = Math.max(p.estoqueMinimo * 2, 1);
  return Math.max(4, Math.min(100, Math.round((p.quantidadeAtual / referencia) * 100)));
}

function IconeAcao({ nome }) {
  const props = {
    width: 15,
    height: 15,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
  };
  if (nome === 'editar') {
    return (
      <svg {...props}>
        <path d="M12 20h9" />
        <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
      </svg>
    );
  }
  return (
    <svg {...props}>
      <path d="M3 6h18" />
      <path d="M8 6V4h8v2" />
      <path d="M19 6l-1 14H6L5 6" />
      <path d="M10 11v5M14 11v5" />
    </svg>
  );
}

export default function Pastilhas() {
  const [lista, setLista] = useState([]);
  const [fornecedores, setFornecedores] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [arquivoImagem, setArquivoImagem] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const [formError, setFormError] = useState('');
  const [filtro, setFiltro] = useState('');
  const [destacado, setDestacado] = useState(null);
  const [imagemAmpliada, setImagemAmpliada] = useState(null);

  const linhasRef = useRef(new Map());

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
    setArquivoImagem(null);
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
    setArquivoImagem(null);
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
      let pastilhaId = editando?.id;
      if (editando) {
        await api.put(`/pastilhas/${editando.id}`, payload);
      } else {
        const { data } = await api.post('/pastilhas', payload);
        pastilhaId = data.id;
      }

      if (arquivoImagem) {
        const dadosImagem = new FormData();
        dadosImagem.append('arquivo', arquivoImagem);
        await api.post(`/pastilhas/${pastilhaId}/imagem`, dadosImagem);
      }

      setModalAberto(false);
      carregar();
    } catch (err) {
      setFormError(err.response?.data?.message ?? 'Erro ao salvar pastilha.');
    } finally {
      setSalvando(false);
    }
  }

  async function removerImagem() {
    if (!editando || !window.confirm('Remover a imagem desta pastilha?')) return;
    try {
      await api.delete(`/pastilhas/${editando.id}/imagem`);
      setEditando({ ...editando, imagemUrl: null });
      carregar();
    } catch {
      alert('Não foi possível remover a imagem.');
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

  const termo = filtro.trim().toLowerCase();
  const modoGaleria = PALAVRAS_GALERIA.includes(termo);

  const listaFiltrada = useMemo(() => {
    if (!termo || modoGaleria) return lista;
    return lista.filter(
      (p) => p.codigo.toLowerCase().includes(termo) || p.descricao.toLowerCase().includes(termo)
    );
  }, [lista, termo, modoGaleria]);

  const totalCriticas = useMemo(() => lista.filter((p) => p.abaixoDoMinimo).length, [lista]);

  const pastilhasComImagem = useMemo(() => lista.filter((p) => p.imagemUrl), [lista]);

  function irParaProduto(id) {
    const linha = linhasRef.current.get(id);
    if (linha) {
      linha.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
    setDestacado(id);
    setTimeout(() => setDestacado((atual) => (atual === id ? null : atual)), 2000);
  }

  return (
    <div className="pagina-fixa">
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

      <section className="panel painel-tabela">
        <div className="tabela-barra">
          <input
            className="busca-tabela"
            type="search"
            placeholder="Buscar por código, descrição ou digite 'imagem'"
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
          />
          {!loading && (
            <div className="tabela-resumo">
              <span className="badge badge-neutral">
                {termo && !modoGaleria ? `${listaFiltrada.length} de ${lista.length}` : lista.length} pastilhas
              </span>
              {totalCriticas > 0 && (
                <span className="badge badge-danger">
                  {totalCriticas} {totalCriticas === 1 ? 'crítica' : 'críticas'}
                </span>
              )}
            </div>
          )}
        </div>

        {modoGaleria && (
          <div className="galeria-imagens">
            {pastilhasComImagem.length === 0 ? (
              <p className="empty-state">Nenhuma pastilha com imagem cadastrada ainda.</p>
            ) : (
              pastilhasComImagem.map((p) => (
                <button key={p.id} type="button" className="galeria-item" onClick={() => irParaProduto(p.id)}>
                  <img src={urlImagem(p.imagemUrl)} alt={p.codigo} />
                  <span>{p.codigo}</span>
                </button>
              ))
            )}
          </div>
        )}

        {loading ? (
          <p className="empty-state">Carregando...</p>
        ) : listaFiltrada.length === 0 ? (
          <p className="empty-state">Nenhuma pastilha encontrada.</p>
        ) : (
          <div className="table-scroll rolavel">
            <table className="table tabela-pastilhas">
              <thead>
                <tr>
                  <th>Imagem</th>
                  <th>Pastilha</th>
                  <th>Fabricante</th>
                  <th>Estoque</th>
                  <th>Situação</th>
                  <th className="acoes-coluna">Ações</th>
                </tr>
              </thead>
              <tbody>
                {listaFiltrada.map((p) => (
                  <tr
                    key={p.id}
                    ref={(el) => {
                      if (el) linhasRef.current.set(p.id, el);
                      else linhasRef.current.delete(p.id);
                    }}
                    className={(destacado === p.id ? 'row-highlight ' : '') + (p.abaixoDoMinimo ? 'critica' : '')}
                  >
                    <td className="col-imagem">
                      {p.imagemUrl ? (
                        <button
                          type="button"
                          className="thumb-botao"
                          title="Clique para ampliar"
                          onClick={() => setImagemAmpliada(urlImagem(p.imagemUrl))}
                        >
                          <img className="thumb" src={urlImagem(p.imagemUrl)} alt={p.codigo} />
                        </button>
                      ) : (
                        <span className="thumb thumb-placeholder" title="Sem imagem">
                          sem imagem
                        </span>
                      )}
                    </td>
                    <td className="col-produto">
                      <strong>{p.codigo}</strong>
                      <span>{p.descricao}</span>
                    </td>
                    <td className="col-fabricante">{p.fabricante?.nome ?? <span className="text-muted">-</span>}</td>
                    <td className="col-estoque">
                      <div className="estoque-valor">
                        <strong className={p.abaixoDoMinimo ? 'critico' : ''}>{p.quantidadeAtual}</strong>
                        <small>un.</small>
                      </div>
                      <div
                        className={'estoque-barra' + (p.abaixoDoMinimo ? ' critico' : '')}
                        title={`${p.quantidadeAtual} em estoque · mínimo ${p.estoqueMinimo}`}
                      >
                        <span style={{ width: `${percentualEstoque(p)}%` }} />
                      </div>
                      <small className="estoque-minimo">mínimo: {p.estoqueMinimo}</small>
                    </td>
                    <td>
                      {p.abaixoDoMinimo ? (
                        <span className="badge badge-danger badge-ponto">Estoque crítico</span>
                      ) : (
                        <span className="badge badge-success badge-ponto">Normal</span>
                      )}
                    </td>
                    <td className="acoes-coluna">
                      <div className="acoes-linha">
                        <button type="button" className="btn-acao" onClick={() => abrirEdicao(p)}>
                          <IconeAcao nome="editar" />
                          Editar
                        </button>
                        <button type="button" className="btn-acao btn-acao-perigo" onClick={() => excluir(p)}>
                          <IconeAcao nome="excluir" />
                          Excluir
                        </button>
                      </div>
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
            <div className="form-row">
              <label>
                Código
                <input
                  value={form.codigo}
                  onChange={(e) => setForm({ ...form, codigo: e.target.value })}
                  placeholder="Ex: CNMG120408"
                  required
                  autoFocus
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
            </div>
            <label>
              Descrição
              <input
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
                placeholder="Ex: Pastilha de torneamento"
                required
              />
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
                <span className="campo-ajuda">Abaixo disso o estoque fica crítico</span>
              </label>
              <label>
                Estoque atual
                <input
                  type="number"
                  min="0"
                  value={form.quantidadeAtual}
                  onChange={(e) => setForm({ ...form, quantidadeAtual: e.target.value })}
                  required
                />
                <span className="campo-ajuda">{editando ? 'Ajuste manual' : 'Estoque inicial'}</span>
              </label>
            </div>

            <div className="campo-imagem">
              <span className="campo-imagem-titulo">Imagem do produto</span>
              <ImagemDropzone arquivo={arquivoImagem} onChange={setArquivoImagem} onErro={setFormError} />
            </div>
            {editando?.imagemUrl && !arquivoImagem && (
              <div className="imagem-atual">
                <img src={urlImagem(editando.imagemUrl)} alt={editando.codigo} />
                <button type="button" className="btn-link btn-link-danger" onClick={removerImagem}>
                  Remover imagem atual
                </button>
              </div>
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

      {imagemAmpliada && <VisualizadorImagem src={imagemAmpliada} onFechar={() => setImagemAmpliada(null)} />}
    </div>
  );
}
