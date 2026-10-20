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

// As fotos dos itens ficam no Supabase Storage e já vêm com o endereço completo (https://...).
// Endereços relativos (/arquivos/...) só aparecem no perfil "h2" do backend, que serve as fotos ele mesmo.
export function urlImagem(imagemUrl) {
  if (!imagemUrl) return null;
  return /^https?:\/\//.test(imagemUrl) ? imagemUrl : `http://localhost:8080${imagemUrl}`;
}
