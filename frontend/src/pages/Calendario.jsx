import { useEffect, useMemo, useState } from 'react';
import api from '../api/client';

const DIAS_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
const MESES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

function paraDataLocal(iso) {
  if (!iso) return null;
  const [ano, mes, dia] = iso.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

function paraChave(date) {
  const ano = date.getFullYear();
  const mes = String(date.getMonth() + 1).padStart(2, '0');
  const dia = String(date.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

function formatarData(iso) {
  if (!iso) return '-';
  const data = paraDataLocal(iso);
  return data.toLocaleDateString('pt-BR');
}

function gerarGrade(mesReferencia) {
  const primeiroDia = new Date(mesReferencia.getFullYear(), mesReferencia.getMonth(), 1);
  const inicioGrade = new Date(primeiroDia);
  inicioGrade.setDate(primeiroDia.getDate() - primeiroDia.getDay());

  const dias = [];
  for (let i = 0; i < 42; i++) {
    const dia = new Date(inicioGrade);
    dia.setDate(inicioGrade.getDate() + i);
    dias.push(dia);
  }
  return dias;
}

export default function Calendario() {
  const [projetos, setProjetos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [edicoes, setEdicoes] = useState({});
  const [salvandoId, setSalvandoId] = useState(null);
  const [mesReferencia, setMesReferencia] = useState(() => {
    const hoje = new Date();
    return new Date(hoje.getFullYear(), hoje.getMonth(), 1);
  });

  function carregar() {
    setLoading(true);
    api
      .get('/projetos/calendario')
      .then((res) => {
        setProjetos(res.data);
        const iniciais = {};
        res.data.forEach((p) => {
          iniciais[p.id] = { dataPedido: p.dataPedido ?? '', metaEntrega: p.metaEntrega ?? '' };
        });
        setEdicoes(iniciais);
      })
      .catch(() => setError('Não foi possível carregar o calendário.'))
      .finally(() => setLoading(false));
  }

  useEffect(carregar, []);

  function atualizarEdicao(id, campo, valor) {
    setEdicoes((e) => ({ ...e, [id]: { ...e[id], [campo]: valor } }));
  }

  async function salvarDatas(id) {
    setSalvandoId(id);
    try {
      await api.patch(`/projetos/${id}/calendario`, edicoes[id]);
      carregar();
    } catch {
      alert('Não foi possível salvar as datas.');
    } finally {
      setSalvandoId(null);
    }
  }

  async function marcarEntregue(id) {
    if (!window.confirm('Marcar este pedido como entregue?')) return;
    try {
      await api.post(`/projetos/${id}/entregar`);
      carregar();
    } catch (err) {
      alert(err.response?.data?.message ?? 'Não foi possível marcar como entregue.');
    }
  }

  const pedidosAbertos = projetos.filter((p) => p.status === 'PEDIDO_ENVIADO');
  const historicoEntregas = projetos
    .filter((p) => p.status === 'ENTREGUE')
    .sort((a, b) => new Date(b.dataEntrega) - new Date(a.dataEntrega));

  const eventosPorDia = useMemo(() => {
    const mapa = new Map();
    function registrar(iso, evento) {
      if (!iso) return;
      const chave = paraChave(paraDataLocal(iso));
      if (!mapa.has(chave)) mapa.set(chave, []);
      mapa.get(chave).push(evento);
    }
    pedidosAbertos.forEach((p) => registrar(p.metaEntrega, { tipo: 'meta', projeto: p }));
    historicoEntregas.forEach((p) => {
      if (p.dataEntrega) {
        const chave = paraChave(new Date(p.dataEntrega));
        if (!mapa.has(chave)) mapa.set(chave, []);
        mapa.get(chave).push({ tipo: 'entrega', projeto: p });
      }
    });
    return mapa;
  }, [pedidosAbertos, historicoEntregas]);

  const dias = useMemo(() => gerarGrade(mesReferencia), [mesReferencia]);

  return (
    <div>
      <header className="page-header">
        <h1>Calendário de entregas</h1>
        <p>Pedidos enviados aguardando entrega e histórico de entregas já realizadas.</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="panel calendario-mes">
        <div className="calendario-mes-header">
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={() => setMesReferencia((m) => new Date(m.getFullYear(), m.getMonth() - 1, 1))}
          >
            ← Anterior
          </button>
          <h2>
            {MESES[mesReferencia.getMonth()]} {mesReferencia.getFullYear()}
          </h2>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={() => setMesReferencia((m) => new Date(m.getFullYear(), m.getMonth() + 1, 1))}
          >
            Próximo →
          </button>
        </div>

        <div className="calendario-grade">
          {DIAS_SEMANA.map((d) => (
            <div key={d} className="calendario-dia-semana">
              {d}
            </div>
          ))}
          {dias.map((dia) => {
            const chave = paraChave(dia);
            const eventos = eventosPorDia.get(chave) ?? [];
            const temMeta = eventos.some((e) => e.tipo === 'meta');
            const temEntrega = eventos.some((e) => e.tipo === 'entrega');
            const outroMes = dia.getMonth() !== mesReferencia.getMonth();
            const hoje = paraChave(new Date()) === chave;
            return (
              <div
                key={chave}
                className={
                  'calendario-cel' +
                  (outroMes ? ' outro-mes' : '') +
                  (hoje ? ' hoje' : '') +
                  (temMeta && temEntrega ? ' ev-ambos' : temMeta ? ' ev-meta' : temEntrega ? ' ev-entrega' : '')
                }
              >
                <span className="calendario-num">{dia.getDate()}</span>
                <div className="calendario-eventos">
                  {eventos.map((ev, i) => (
                    <span
                      key={i}
                      className={`calendario-evento ${ev.tipo === 'meta' ? 'evento-meta' : 'evento-entrega'}`}
                      title={`${ev.tipo === 'meta' ? 'Meta de entrega' : 'Entregue'}: ${ev.projeto.nome}${ev.projeto.cliente ? ` (${ev.projeto.cliente})` : ''}`}
                    >
                      {ev.projeto.nome}
                    </span>
                  ))}
                </div>
              </div>
            );
          })}
        </div>

        <div className="calendario-legenda">
          <span>
            <span className="legenda-cor legenda-meta" /> Meta de entrega
          </span>
          <span>
            <span className="legenda-cor legenda-entrega" /> Entregue
          </span>
        </div>
      </section>

      {loading ? (
        <p className="empty-state">Carregando...</p>
      ) : (
        <div className="panels-grid">
          <section className="panel">
            <h2>Pedidos em aberto ({pedidosAbertos.length})</h2>
            {pedidosAbertos.length === 0 ? (
              <p className="empty-state">Nenhum pedido aguardando entrega.</p>
            ) : (
              pedidosAbertos.map((p) => (
                <div key={p.id} className="pedido-card">
                  <div className="pedido-card-header">
                    <div>
                      <strong>{p.nome}</strong>
                      {p.cliente && <small className="pedido-cliente">Cliente: {p.cliente}</small>}
                    </div>
                    <button className="btn btn-success btn-sm" onClick={() => marcarEntregue(p.id)}>
                      Marcar como entregue
                    </button>
                  </div>
                  <div className="form-row">
                    <label>
                      Data do pedido
                      <input
                        type="date"
                        value={edicoes[p.id]?.dataPedido ?? ''}
                        onChange={(e) => atualizarEdicao(p.id, 'dataPedido', e.target.value)}
                      />
                    </label>
                    <label>
                      Meta de entrega
                      <input
                        type="date"
                        value={edicoes[p.id]?.metaEntrega ?? ''}
                        onChange={(e) => atualizarEdicao(p.id, 'metaEntrega', e.target.value)}
                      />
                    </label>
                  </div>
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    disabled={salvandoId === p.id}
                    onClick={() => salvarDatas(p.id)}
                  >
                    {salvandoId === p.id ? 'Salvando...' : 'Salvar datas'}
                  </button>
                </div>
              ))
            )}
          </section>

          <section className="panel">
            <h2>Histórico de entregas ({historicoEntregas.length})</h2>
            {historicoEntregas.length === 0 ? (
              <p className="empty-state">Nenhuma entrega registrada ainda.</p>
            ) : (
              <div className="table-scroll">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Projeto</th>
                      <th>Cliente</th>
                      <th>Pedido</th>
                      <th>Meta</th>
                      <th>Entregue em</th>
                    </tr>
                  </thead>
                  <tbody>
                    {historicoEntregas.map((p) => (
                      <tr key={p.id}>
                        <td>{p.nome}</td>
                        <td>{p.cliente ?? '-'}</td>
                        <td>{formatarData(p.dataPedido)}</td>
                        <td>{formatarData(p.metaEntrega)}</td>
                        <td>{new Date(p.dataEntrega).toLocaleDateString('pt-BR')}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
