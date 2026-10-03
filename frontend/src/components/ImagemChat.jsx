import { useEffect, useState } from 'react';
import api from '../api/client';

// As imagens do chat são privadas: não há link público, então a imagem é baixada com o token de login
// e exibida a partir de um endereço temporário do navegador.
export default function ImagemChat({ mensagemId, onAbrir, onCarregar }) {
  const [src, setSrc] = useState(null);
  const [falhou, setFalhou] = useState(false);

  useEffect(() => {
    let cancelado = false;
    let url = null;
    api
      .get(`/mensagens/${mensagemId}/imagem`, { responseType: 'blob' })
      .then((res) => {
        if (cancelado) return;
        url = URL.createObjectURL(res.data);
        setSrc(url);
      })
      .catch(() => !cancelado && setFalhou(true));
    return () => {
      cancelado = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [mensagemId]);

  if (falhou) return <div className="chat-imagem chat-imagem-erro">Imagem indisponível</div>;
  if (!src) return <div className="chat-imagem chat-imagem-carregando">Carregando imagem...</div>;

  return (
    <button type="button" className="chat-imagem-botao" onClick={() => onAbrir(src)} title="Clique para ampliar">
      <img className="chat-imagem" src={src} alt="Imagem enviada na conversa" onLoad={onCarregar} />
    </button>
  );
}
