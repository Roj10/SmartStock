const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function moeda(valor) {
  if (valor === null || valor === undefined || valor === '') return '-';
  return BRL.format(Number(valor));
}

export function dataBr(iso) {
  if (!iso) return '-';
  const [ano, mes, dia] = iso.slice(0, 10).split('-').map(Number);
  return new Date(ano, mes - 1, dia).toLocaleDateString('pt-BR');
}

export function urlImagem(imagemUrl) {
  return imagemUrl ? `http://localhost:8080${imagemUrl}` : null;
}
