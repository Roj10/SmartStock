import { useEffect, useState } from 'react';
import api from '../api/client';
import StatCard from '../components/StatCard';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api
      .get('/dashboard')
      .then((res) => active && setData(res.data))
      .catch(() => active && setError('Não foi possível carregar o painel.'));
    return () => {
      active = false;
    };
  }, []);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!data) return <div className="loading">Carregando painel...</div>;

  const semAcesso = !data.podeVerPastilhas && !data.podeVerFornecedores && !data.podeVerMovimentacoes;

  return (
    <div>
      <header className="page-header">
        <h1>Painel de estoque</h1>
        <p>Visão geral do estoque de pastilhas industriais da DDA Metalúrgica.</p>
      </header>

      {semAcesso && (
        <div className="alert alert-info">
          Sua conta ainda não tem acesso a nenhum módulo. Fale com um administrador para liberar o que você precisa.
        </div>
      )}

      <div className="stats-grid">
        {data.podeVerPastilhas && <StatCard label="Pastilhas cadastradas" value={data.totalPastilhas} />}
        {data.podeVerFornecedores && <StatCard label="Fornecedores/fabricantes" value={data.totalFornecedores} />}
        {data.podeVerPastilhas && (
          <StatCard
            label="Itens abaixo do mínimo"
            value={data.itensAbaixoDoMinimo}
            tone={data.itensAbaixoDoMinimo > 0 ? 'danger' : 'success'}
          />
        )}
      </div>

      {(data.podeVerPastilhas || data.podeVerMovimentacoes) && (
        <div className="panels-grid">
          {data.podeVerPastilhas && (
            <section className="panel">
              <h2>Alertas de estoque crítico</h2>
              {data.alertas.length === 0 ? (
                <p className="empty-state">Nenhum item abaixo do estoque mínimo. Tudo sob controle.</p>
              ) : (
                <div className="table-scroll">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Código</th>
                        <th>Descrição</th>
                        <th>Atual</th>
                        <th>Mínimo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.alertas.map((p) => (
                        <tr key={p.id}>
                          <td>{p.codigo}</td>
                          <td>{p.descricao}</td>
                          <td className="text-danger">{p.quantidadeAtual}</td>
                          <td>{p.estoqueMinimo}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {data.podeVerMovimentacoes && (
            <section className="panel">
              <h2>Últimas movimentações</h2>
              {data.ultimasMovimentacoes.length === 0 ? (
                <p className="empty-state">Nenhuma movimentação registrada ainda.</p>
              ) : (
                <div className="table-scroll">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Pastilha</th>
                        <th>Tipo</th>
                        <th>Qtd.</th>
                        <th>Data</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.ultimasMovimentacoes.map((m) => (
                        <tr key={m.id}>
                          <td>{m.pastilha?.codigo}</td>
                          <td>
                            <span className={`badge ${m.tipo === 'ENTRADA' ? 'badge-success' : 'badge-warning'}`}>
                              {m.tipo === 'ENTRADA' ? 'Entrada' : 'Saída'}
                            </span>
                          </td>
                          <td>{m.quantidade}</td>
                          <td>{new Date(m.dataHora).toLocaleString('pt-BR')}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}
        </div>
      )}
    </div>
  );
}
