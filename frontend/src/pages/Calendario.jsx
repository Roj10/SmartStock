import { useEffect, useMemo, useRef, useState } from 'react';
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
  // pedidos destacados ao clicar num dia do calendário (e o dia clicado)
  const [destacados, setDestacados] = useState([]);
  const [diaSelecionado, setDiaSelecionado] = useState(null);
  const [pulso, setPulso] = useState(0);
  const cartoesRef = useRef(new Map());
  const linhasRef = useRef(new Map());
  const timerRef = useRef(null);
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

  useEffect(() => () => clearTimeout(timerRef.current), []);

  // Destaca os pedidos e rola a lista até o primeiro deles (em "Pedidos em aberto" ou, se já foi entregue, no histórico).
  function destacar(ids, chaveDia) {
    clearTimeout(timerRef.current);
    setDestacados(ids);
    setDiaSelecionado(chaveDia);
    setPulso((n) => n + 1); // alterna a animação para ela recomeçar a cada clique
    const primeiroAberto = ids.find((id) => cartoesRef.current.has(id));
    const alvo =
      cartoesRef.current.get(primeiroAberto) ?? linhasRef.current.get(ids.find((id) => linhasRef.current.has(id)));
    alvo?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    timerRef.current = setTimeout(() => {
      setDestacados([]);
      setDiaSelecionado(null);
    }, 4500);
  }

  function aoClicarDia(chave, eventos) {
    const abertos = eventos.filter((e) => e.tipo === 'meta').map((e) => e.projeto.id);
    const entregues = eventos.filter((e) => e.tipo === 'entrega').map((e) => e.projeto.id);
    destacar([...abertos, ...entregues], chave);
  }

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
    <div className="pagina-fixa">
      <header className="page-header">
        <h1>Calendário de entregas</h1>
        <p>Pedidos enviados aguardando entrega e histórico de entregas já realizadas.</p>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="calendario-layout">
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
                role={eventos.length ? 'button' : undefined}
                tabIndex={eventos.length ? 0 : undefined}
                title={eventos.length ? 'Clique para ver o pedido na lista ao lado' : undefined}
                onClick={eventos.length ? () => aoClicarDia(chave, eventos) : undefined}
                onKeyDown={
                  eventos.length
                    ? (e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          aoClicarDia(chave, eventos);
                        }
                      }
                    : undefined
                }
                className={
                  'calendario-cel' +
                  (eventos.length ? ' clicavel' : '') +
                  (diaSelecionado === chave ? ' selecionado' : '') +
                  (outroMes ? ' outro-mes' : '') +
                  (hoje ? ' hoje' : '') +
                  (temMeta && temEntrega ? ' ev-ambos' : temMeta ? ' ev-meta' : temEntrega ? ' ev-entrega' : '')
                }
              >
                <span className="calendario-num">{dia.getDate()}</span>
                <div className="calendario-eventos">
                  {eventos.map((ev, i) => (
                    <button
                      type="button"
                      key={i}
                      className={`calendario-evento ${ev.tipo === 'meta' ? 'evento-meta' : 'evento-entrega'}`}
                      title={`${ev.tipo === 'meta' ? 'Meta de entrega' : 'Entregue'}: ${ev.projeto.nome}${ev.projeto.cliente ? ` (${ev.projeto.cliente})` : ''}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        destacar([ev.projeto.id], chave);
                      }}
                    >
                      {ev.projeto.nome}
                    </button>
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
        <div className="calendario-lateral">
          <section className="panel painel-rolavel">
            <h2>Pedidos em aberto ({pedidosAbertos.length})</h2>
            {pedidosAbertos.length === 0 ? (
              <p className="empty-state">Nenhum pedido aguardando entrega.</p>
            ) : (
              <div className="painel-rolavel-corpo">
              {pedidosAbertos.map((p) => {
                const edicao = edicoes[p.id] ?? {};
                const alterado =
                  (edicao.dataPedido ?? '') !== (p.dataPedido ?? '') || (edicao.metaEntrega ?? '') !== (p.metaEntrega ?? '');
                const atrasado = p.metaEntrega && p.metaEntrega < paraChave(new Date());
                return (
                  <div
                    key={p.id}
                    ref={(el) => {
                      if (el) cartoesRef.current.set(p.id, el);
                      else cartoesRef.current.delete(p.id);
                    }}
                    className={'pedido-card' + (destacados.includes(p.id) ? ' destacado' + (pulso % 2 ? ' alt' : '') : '')}
                  >
                    <div className="pedido-card-header">
                      <div className="pedido-card-titulo">
                        <strong>{p.nome}</strong>
                        <span className="pedido-card-sub">
                          {p.cliente && <small className="pedido-cliente">Cliente: {p.cliente}</small>}
                          {atrasado && <span className="badge badge-danger">Atrasado</span>}
                        </span>
                      </div>
                      <button className="btn btn-success btn-sm" onClick={() => marcarEntregue(p.id)}>
                        Marcar como entregue
                      </button>
                    </div>

                    <div className="pedido-datas">
                      <label className="pedido-campo">
                        <span>Data do pedido</span>
                        <input
                          type="date"
                          value={edicao.dataPedido ?? ''}
                          onChange={(e) => atualizarEdicao(p.id, 'dataPedido', e.target.value)}
                        />
                      </label>
                      <label className="pedido-campo">
                        <span>Meta de entrega</span>
                        <input
                          type="date"
                          value={edicao.metaEntrega ?? ''}
                          onChange={(e) => atualizarEdicao(p.id, 'metaEntrega', e.target.value)}
                        />
                      </label>
                    </div>

                    <button
                      type="button"
                      className={'btn btn-sm pedido-salvar ' + (alterado ? 'btn-primary' : 'btn-ghost')}
                      disabled={!alterado || salvandoId === p.id}
                      onClick={() => salvarDatas(p.id)}
                    >
                      {salvandoId === p.id ? 'Salvando...' : alterado ? 'Salvar datas' : 'Datas salvas'}
                    </button>
                  </div>
                );
              })}
              </div>
            )}
          </section>

          <section className="panel painel-rolavel">
            <h2>Histórico de entregas ({historicoEntregas.length})</h2>
            {historicoEntregas.length === 0 ? (
              <p className="empty-state">Nenhuma entrega registrada ainda.</p>
            ) : (
              <div className="table-scroll rolavel">
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
                      <tr
                        key={p.id}
                        ref={(el) => {
                          if (el) linhasRef.current.set(p.id, el);
                          else linhasRef.current.delete(p.id);
                        }}
                        className={destacados.includes(p.id) ? 'row-highlight' : ''}
                      >
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
    </div>
  );
}
