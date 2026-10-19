import { useRef, useState } from 'react';
import api from '../api/client';

// Mesmas regras do servidor (AnexoProjetoService): tipos aceitos, 20 MB por arquivo e até 20 arquivos por projeto.
export const EXTENSOES_ACEITAS = [
  'pdf', 'png', 'jpg', 'jpeg', 'webp', 'gif',
  'dwg', 'dxf', 'dwf', 'step', 'stp', 'iges', 'igs', 'stl', 'skp', '3dm',
  'ipt', 'iam', 'idw', 'sldprt', 'sldasm', 'slddrw',
  'doc', 'docx', 'xls', 'xlsx', 'odt', 'ods', 'txt', 'csv',
  'zip', 'rar', '7z',
];
export const TAMANHO_MAXIMO = 20 * 1024 * 1024;
export const MAXIMO_ARQUIVOS = 20;

const CAD = ['dwg', 'dxf', 'dwf', 'step', 'stp', 'iges', 'igs', 'stl', 'skp', '3dm', 'ipt', 'iam', 'idw', 'sldprt', 'sldasm', 'slddrw'];
const IMAGEM = ['png', 'jpg', 'jpeg', 'webp', 'gif'];
const PLANILHA_DOC = ['doc', 'docx', 'xls', 'xlsx', 'odt', 'ods', 'txt', 'csv'];

function extensao(nome) {
  const i = nome.lastIndexOf('.');
  return i < 0 ? '' : nome.slice(i + 1).toLowerCase();
}

/** Rótulo curto que aparece no ícone do arquivo. */
export function rotuloTipo(nome) {
  const ext = extensao(nome);
  if (ext === 'pdf') return 'PDF';
  if (IMAGEM.includes(ext)) return 'IMG';
  if (CAD.includes(ext)) return 'CAD';
  if (PLANILHA_DOC.includes(ext)) return 'DOC';
  return 'ZIP';
}

export function formatarTamanho(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`;
}

/**
 * Os arquivos são privados (não há link público): são baixados com o token de login.
 * PDF e imagens abrem numa nova aba; os demais (DWG, STEP, ZIP...) são salvos no computador.
 */
export async function abrirAnexo(projetoId, anexo) {
  const abreNoNavegador = anexo.tipo === 'application/pdf' || anexo.tipo.startsWith('image/');
  // a aba é aberta já no clique; se fosse aberta depois do download, o navegador a bloquearia como pop-up
  const aba = abreNoNavegador ? window.open('', '_blank') : null;
  try {
    const { data } = await api.get(`/projetos/${projetoId}/anexos/${anexo.id}`, { responseType: 'blob' });
    const url = URL.createObjectURL(data);
    if (aba) {
      aba.location.href = url;
    } else {
      const link = document.createElement('a');
      link.href = url;
      link.download = anexo.nomeOriginal;
      document.body.appendChild(link);
      link.click();
      link.remove();
    }
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch {
    aba?.close();
    alert(`Não foi possível abrir o arquivo "${anexo.nomeOriginal}".`);
  }
}

/** Confere os arquivos escolhidos; devolve os aceitos e a mensagem de erro dos recusados (se houver). */
export function filtrarArquivos(arquivos, jaSelecionados) {
  const aceitos = [];
  const erros = [];
  for (const arquivo of arquivos) {
    if (!EXTENSOES_ACEITAS.includes(extensao(arquivo.name))) {
      erros.push(`"${arquivo.name}" não é de um tipo aceito`);
    } else if (arquivo.size > TAMANHO_MAXIMO) {
      erros.push(`"${arquivo.name}" passa de 20 MB`);
    } else if (jaSelecionados + aceitos.length >= MAXIMO_ARQUIVOS) {
      erros.push(`o limite é de ${MAXIMO_ARQUIVOS} arquivos por projeto`);
      break;
    } else {
      aceitos.push(arquivo);
    }
  }
  return { aceitos, erro: erros.length ? `Não foi possível anexar: ${erros.join('; ')}.` : '' };
}

function IconeArquivo({ nome }) {
  const rotulo = rotuloTipo(nome);
  return <span className={`anexo-icone anexo-icone-${rotulo.toLowerCase()}`}>{rotulo}</span>;
}

/**
 * Campo do formulário do projeto: arquivos que já estão no projeto (ou que serão copiados, ao repetir)
 * e arquivos novos escolhidos agora. Aceita arrastar vários arquivos de uma vez.
 */
// Cada item de `existentes` traz o `projetoId` de onde o arquivo está (no caso de repetir, é o projeto de origem).
export function CampoAnexos({ existentes, onRemoverExistente, novos, onChangeNovos, onErro }) {
  const inputRef = useRef(null);
  const [arrastando, setArrastando] = useState(false);

  function receber(lista) {
    const { aceitos, erro } = filtrarArquivos(Array.from(lista ?? []), existentes.length + novos.length);
    onErro(erro);
    if (aceitos.length) onChangeNovos([...novos, ...aceitos]);
  }

  function aoArrastar(e) {
    e.preventDefault();
    setArrastando(true);
  }

  const total = existentes.length + novos.length;

  return (
    <div className="dynamic-field">
      <span className="permissoes-label">Arquivos do projeto ({total})</span>

      {total > 0 && (
        <ul className="anexos-lista">
          {existentes.map((a) => (
            <li key={`e${a.id}`} className="anexo-item">
              <IconeArquivo nome={a.nomeOriginal} />
              <button type="button" className="anexo-nome btn-link" onClick={() => abrirAnexo(a.projetoId, a)}>
                {a.nomeOriginal}
              </button>
              <small>{formatarTamanho(a.tamanho)}</small>
              <button type="button" className="btn-icon" title="Remover" onClick={() => onRemoverExistente(a)}>
                &times;
              </button>
            </li>
          ))}
          {novos.map((f, i) => (
            <li key={`n${i}-${f.name}`} className="anexo-item anexo-item-novo">
              <IconeArquivo nome={f.name} />
              <span className="anexo-nome">{f.name}</span>
              <small>{formatarTamanho(f.size)} · novo</small>
              <button
                type="button"
                className="btn-icon"
                title="Remover"
                onClick={() => onChangeNovos(novos.filter((_, j) => j !== i))}
              >
                &times;
              </button>
            </li>
          ))}
        </ul>
      )}

      <div
        className={'dropzone dropzone-compacta' + (arrastando ? ' dropzone-ativa' : '')}
        onDragEnter={aoArrastar}
        onDragOver={aoArrastar}
        onDragLeave={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget)) setArrastando(false);
        }}
        onDrop={(e) => {
          e.preventDefault();
          setArrastando(false);
          receber(e.dataTransfer.files);
        }}
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
          multiple
          accept={EXTENSOES_ACEITAS.map((x) => '.' + x).join(',')}
          hidden
          onChange={(e) => {
            receber(e.target.files);
            e.target.value = '';
          }}
        />
        <div className="dropzone-vazio">
          <span className="dropzone-icone">📎</span>
          <strong>{arrastando ? 'Solte os arquivos aqui' : 'Arraste PDFs ou arquivos do projeto para cá'}</strong>
          <small>ou clique para escolher · PDF, imagens, DWG, DXF, STEP, STL, planilhas, ZIP · até 20 MB cada</small>
        </div>
      </div>
    </div>
  );
}

/** Lista compacta mostrada no card do quadro de produção. */
export function AnexosCard({ projetoId, anexos }) {
  if (!anexos?.length) return null;
  return (
    <ul className="kanban-anexos" aria-label="Arquivos do projeto">
      {anexos.map((a) => (
        <li key={a.id}>
          <button
            type="button"
            className="anexo-chip"
            title={`${a.nomeOriginal} (${formatarTamanho(a.tamanho)}) — clique para ${
              a.tipo === 'application/pdf' || a.tipo.startsWith('image/') ? 'abrir' : 'baixar'
            }`}
            onClick={() => abrirAnexo(projetoId, a)}
          >
            <IconeArquivo nome={a.nomeOriginal} />
            <span className="anexo-chip-nome">{a.nomeOriginal}</span>
          </button>
        </li>
      ))}
    </ul>
  );
}
