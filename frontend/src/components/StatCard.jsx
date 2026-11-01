/*
 * Em palavras simples: Cartão de número do painel inicial (ex.: "Itens cadastrados: 42").
 */
export default function StatCard({ label, value, tone = 'default' }) {
  return (
    <div className={`stat-card tone-${tone}`}>
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
    </div>
  );
}
