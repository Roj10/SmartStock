import { useEffect } from 'react';

// Imagem ampliada sobre a tela; fecha com clique, com o "×" ou com Esc.
export default function VisualizadorImagem({ src, onFechar }) {
  useEffect(() => {
    function aoTeclar(e) {
      if (e.key === 'Escape') onFechar();
    }
    document.addEventListener('keydown', aoTeclar);
    return () => document.removeEventListener('keydown', aoTeclar);
  }, [onFechar]);

  return (
    <div className="visualizador" onClick={onFechar} role="dialog" aria-label="Imagem ampliada">
      <button type="button" className="visualizador-fechar" aria-label="Fechar" onClick={onFechar}>
        &times;
      </button>
      <img src={src} alt="Imagem ampliada" onClick={(e) => e.stopPropagation()} />
    </div>
  );
}
