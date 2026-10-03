import { useEffect, useRef, useState } from 'react';

const TIPOS_ACEITOS = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];

// Área para soltar uma imagem arrastada do computador (ou clicar para escolher o arquivo).
export default function ImagemDropzone({ arquivo, onChange, onErro }) {
  const inputRef = useRef(null);
  const [arrastando, setArrastando] = useState(false);
  const [previa, setPrevia] = useState(null);

  useEffect(() => {
    if (!arquivo) {
      setPrevia(null);
      return undefined;
    }
    const url = URL.createObjectURL(arquivo);
    setPrevia(url);
    return () => URL.revokeObjectURL(url);
  }, [arquivo]);

  function receber(arquivos) {
    const escolhido = arquivos?.[0];
    if (!escolhido) return;
    if (!TIPOS_ACEITOS.includes(escolhido.type)) {
      onErro?.('Formato não aceito. Use uma imagem PNG, JPG, WEBP ou GIF.');
      return;
    }
    onErro?.('');
    onChange(escolhido);
  }

  function aoSoltar(e) {
    e.preventDefault();
    setArrastando(false);
    receber(e.dataTransfer.files);
  }

  function aoArrastar(e) {
    // sem isso o navegador abriria a imagem na página ao soltar
    e.preventDefault();
    setArrastando(true);
  }

  return (
    <div
      className={'dropzone' + (arrastando ? ' dropzone-ativa' : '')}
      onDragEnter={aoArrastar}
      onDragOver={aoArrastar}
      onDragLeave={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget)) setArrastando(false);
      }}
      onDrop={aoSoltar}
      onClick={() => inputRef.current?.click()}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          inputRef.current?.click();
        }
      }}
    >
      <input
        ref={inputRef}
        type="file"
        accept={TIPOS_ACEITOS.join(',')}
        hidden
        onChange={(e) => {
          receber(e.target.files);
          e.target.value = '';
        }}
      />

      {previa ? (
        <div className="dropzone-previa">
          <img src={previa} alt="Pré-visualização" />
          <div className="dropzone-previa-info">
            <strong>{arquivo.name}</strong>
            <small>Clique ou arraste outra imagem para trocar</small>
            <button
              type="button"
              className="btn-link btn-link-danger"
              onClick={(e) => {
                e.stopPropagation();
                onChange(null);
              }}
            >
              Remover seleção
            </button>
          </div>
        </div>
      ) : (
        <div className="dropzone-vazio">
          <span className="dropzone-icone">⬆</span>
          <strong>{arrastando ? 'Solte a imagem aqui' : 'Arraste a imagem para cá'}</strong>
          <small>ou clique para escolher o arquivo (PNG, JPG, WEBP ou GIF)</small>
        </div>
      )}
    </div>
  );
}
