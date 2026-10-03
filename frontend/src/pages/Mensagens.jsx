import { Fragment, useCallback, useEffect, useRef, useState } from 'react';
import api from '../api/client';
import EmojiPicker from '../components/EmojiPicker';
import ImagemChat from '../components/ImagemChat';
import VisualizadorImagem from '../components/VisualizadorImagem';
import '../chat.css';

const INTERVALO_ATUALIZACAO = 4000;
const TIPOS_IMAGEM = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];
const TAMANHO_MAXIMO = 5 * 1024 * 1024;

const SETORES = {
  PROJETOS: 'Produção',
  FINANCEIRO: 'Financeiro',
  PASTILHAS: 'Estoque',
  MOVIMENTACOES: 'Estoque',
  FORNECEDORES: 'Compras',
  USUARIOS: 'Administração',
};

// Cada conta representa um setor; o setor é deduzido das abas a que a conta tem acesso.
function setorDe(contato) {
  if (contato.role === 'ADMIN') return 'Administrador';
  const setores = [...new Set((contato.permissoes ?? []).map((p) => SETORES[p]).filter(Boolean))];
  return setores.length ? setores.join(' · ') : 'Sem setor definido';
}

function inicial(nome) {
  return nome?.trim()?.[0]?.toUpperCase() ?? '?';
}

// Cada conta ganha sempre a mesma cor de avatar, escolhida pelo nome.
const CORES_AVATAR = ['#1f4e79', '#1b8a5a', '#b4640e', '#7b4ea3', '#c2410c', '#0f766e', '#be185d'];

function corAvatar(nome = '') {
  let soma = 0;
  for (const letra of nome) soma += letra.charCodeAt(0);
  return CORES_AVATAR[soma % CORES_AVATAR.length];
}

function Avatar({ nome, pequeno = false }) {
  return (
    <span className={'chat-avatar' + (pequeno ? ' chat-avatar-pequeno' : '')} style={{ background: corAvatar(nome) }}>
      {inicial(nome)}
    </span>
  );
}

// Ícones em SVG (os emojis de clipe/sorriso mudam de aparência conforme o sistema).
function Icone({ nome }) {
  const props = {
    width: 22,
    height: 22,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.9,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
  };
  if (nome === 'sorriso') {
    return (
      <svg {...props}>
        <circle cx="12" cy="12" r="9.5" />
        <path d="M8 14.2c.9 1.3 2.3 2 4 2s3.1-.7 4-2" />
        <path d="M9 9.6h.01M15 9.6h.01" strokeWidth="2.6" />
      </svg>
    );
  }
  if (nome === 'clipe') {
    return (
      <svg {...props}>
        <path d="m20.4 11.1-8.5 8.5a5.5 5.5 0 0 1-7.8-7.8l8.9-8.9a3.7 3.7 0 0 1 5.2 5.2l-8.9 8.9a1.8 1.8 0 0 1-2.6-2.6l8.1-8.1" />
      </svg>
    );
  }
  if (nome === 'conversa') {
    return (
      <svg {...props} width="34" height="34" strokeWidth="1.6">
        <path d="M21 12a8.5 8.5 0 0 1-12.3 7.6L3 21l1.5-5.4A8.5 8.5 0 1 1 21 12Z" />
        <path d="M8.5 11h7M8.5 14h4" />
      </svg>
    );
  }
  return (
    <svg {...props} fill="currentColor" stroke="none">
      <path d="M3.4 20.4 21.5 12 3.4 3.6a.6.6 0 0 0-.8.7L4.6 10.5l8.9 1.5-8.9 1.5-2 6.2a.6.6 0 0 0 .8.7Z" />
    </svg>
  );
}

function hora(dataHora) {
  return new Date(dataHora).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function rotuloDia(dataHora) {
  const data = new Date(dataHora);
  const hoje = new Date();
  const ontem = new Date();
  ontem.setDate(hoje.getDate() - 1);
  if (data.toDateString() === hoje.toDateString()) return 'Hoje';
  if (data.toDateString() === ontem.toDateString()) return 'Ontem';
  return data.toLocaleDateString('pt-BR');
}

// Na lista de conversas: hora se for de hoje, senão a data.
function resumoData(dataHora) {
  const data = new Date(dataHora);
  return data.toDateString() === new Date().toDateString() ? hora(dataHora) : data.toLocaleDateString('pt-BR');
}

// Mensagem só com emojis (até 6) aparece em tamanho maior, como nos aplicativos de conversa.
const SO_EMOJIS = /^(?:\p{Extended_Pictographic}|\u200d|\ufe0f|\s){1,24}$/u;

function soEmojis(texto) {
  const t = texto?.trim();
  if (!t || !SO_EMOJIS.test(t)) return false;
  return [...new Intl.Segmenter().segment(t)].filter((s) => s.segment.trim()).length <= 6;
}

function previaUltima(mensagem) {
  if (!mensagem) return 'Nenhuma mensagem ainda';
  if (mensagem.temImagem) return `📷 ${mensagem.texto || 'Imagem'}`;
  return mensagem.texto;
}

function avisarMenu() {
  window.dispatchEvent(new Event('mensagens-atualizadas'));
}

export default function Mensagens() {
  const [contatos, setContatos] = useState([]);
  const [selecionadoId, setSelecionadoId] = useState(null);
  const [mensagens, setMensagens] = useState([]);
  const [texto, setTexto] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  // falha de conexão nas atualizações automáticas: some sozinha quando a conexão volta,
  // sem apagar os avisos de ações do usuário (ex.: formato de imagem não aceito)
  const [erroConexao, setErroConexao] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [busca, setBusca] = useState('');
  const [anexo, setAnexo] = useState(null);
  const [previaAnexo, setPreviaAnexo] = useState(null);
  const [emojisAbertos, setEmojisAbertos] = useState(false);
  const [arrastando, setArrastando] = useState(false);
  const [imagemAmpliada, setImagemAmpliada] = useState(null);

  const listaRef = useRef(null);
  const textareaRef = useRef(null);
  const arquivoRef = useRef(null);
  const ultimoIdRef = useRef(null);
  const selecionadoRef = useRef(null);
  selecionadoRef.current = selecionadoId;
  // sobe a cada envio: uma atualização que começou antes do envio não pode apagar a mensagem nova da tela
  const versaoEnvioRef = useRef(0);

  const carregarContatos = useCallback(() => {
    return api
      .get('/mensagens/contatos')
      .then((res) => {
        setContatos(res.data);
        setErroConexao('');
        return res.data;
      })
      .catch(() => setErroConexao('Não foi possível carregar as conversas.'));
  }, []);

  const carregarConversa = useCallback((id) => {
    const versao = versaoEnvioRef.current;
    return api
      .get(`/mensagens/conversa/${id}`)
      .then((res) => {
        // ignora a resposta se, enquanto ela vinha, o usuário abriu outra conversa ou enviou uma mensagem
        if (selecionadoRef.current !== id || versaoEnvioRef.current !== versao) return;
        setMensagens(res.data);
        avisarMenu();
      })
      .catch(() => setErroConexao('Não foi possível carregar a conversa.'));
  }, []);

  // carga inicial: no computador já abre a primeira conversa
  useEffect(() => {
    carregarContatos().then((lista) => {
      setLoading(false);
      if (lista?.length && window.matchMedia('(min-width: 821px)').matches) {
        const comNaoLida = lista.find((c) => c.naoLidas > 0);
        setSelecionadoId((comNaoLida ?? lista[0]).id);
      }
    });
  }, [carregarContatos]);

  useEffect(() => {
    ultimoIdRef.current = null;
    setMensagens([]);
    // ao abrir, as mensagens recebidas viram "lidas": atualiza também a bolinha da lista de contas
    if (selecionadoId != null) carregarConversa(selecionadoId).then(carregarContatos);
  }, [selecionadoId, carregarConversa, carregarContatos]);

  // atualização periódica, só com a aba visível; ao voltar para a aba atualiza na hora
  useEffect(() => {
    function atualizar() {
      if (document.hidden) return;
      carregarContatos();
      if (selecionadoRef.current != null) carregarConversa(selecionadoRef.current);
    }
    const timer = setInterval(atualizar, INTERVALO_ATUALIZACAO);
    document.addEventListener('visibilitychange', atualizar);
    window.addEventListener('focus', atualizar);
    return () => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', atualizar);
      window.removeEventListener('focus', atualizar);
    };
  }, [carregarContatos, carregarConversa]);

  useEffect(() => {
    if (!anexo) {
      setPreviaAnexo(null);
      return undefined;
    }
    const url = URL.createObjectURL(anexo);
    setPreviaAnexo(url);
    return () => URL.revokeObjectURL(url);
  }, [anexo]);

  useEffect(() => {
    setEmojisAbertos(false);
    setError('');
  }, [selecionadoId]);

  // a imagem ocupa espaço só depois de carregar: se a conversa estava no fim, continua no fim
  const manterNoFim = useCallback(() => {
    const lista = listaRef.current;
    if (lista && lista.scrollHeight - lista.scrollTop - lista.clientHeight < 700) {
      lista.scrollTop = lista.scrollHeight;
    }
  }, []);

  function escolherImagem(arquivo) {
    if (!arquivo) return;
    if (!TIPOS_IMAGEM.includes(arquivo.type)) {
      setError('Formato não aceito. Use uma imagem PNG, JPG, WEBP ou GIF.');
      return;
    }
    if (arquivo.size > TAMANHO_MAXIMO) {
      setError('A imagem é grande demais. O limite é de 5 MB.');
      return;
    }
    setError('');
    setAnexo(arquivo);
    textareaRef.current?.focus();
  }

  // Ctrl+V com uma imagem copiada (ex.: print de tela) anexa a imagem
  function aoColar(e) {
    const imagem = [...(e.clipboardData?.files ?? [])].find((f) => f.type.startsWith('image/'));
    if (imagem) {
      e.preventDefault();
      escolherImagem(imagem);
    }
  }

  function aoSoltarImagem(e) {
    e.preventDefault();
    setArrastando(false);
    escolherImagem([...e.dataTransfer.files].find((f) => f.type.startsWith('image/')) ?? e.dataTransfer.files[0]);
  }

  function inserirEmoji(emoji) {
    const campo = textareaRef.current;
    const inicio = campo?.selectionStart ?? texto.length;
    const fim = campo?.selectionEnd ?? texto.length;
    const novo = texto.slice(0, inicio) + emoji + texto.slice(fim);
    if (novo.length > 2000) return;
    setTexto(novo);
    // devolve o cursor para logo depois do emoji
    requestAnimationFrame(() => {
      campo?.focus();
      campo?.setSelectionRange(inicio + emoji.length, inicio + emoji.length);
    });
  }

  // o campo de texto cresce conforme a mensagem (até um limite) e volta ao tamanho normal ao enviar
  useEffect(() => {
    const campo = textareaRef.current;
    if (!campo) return;
    campo.style.height = 'auto';
    // vazio: uma linha só (o scrollHeight de um campo vazio pode contar o texto de exemplo)
    campo.style.height = texto ? `${Math.min(campo.scrollHeight, 140)}px` : '40px';
  }, [texto, selecionadoId]);

  // desce até a última mensagem quando chega algo novo
  useEffect(() => {
    const ultima = mensagens[mensagens.length - 1]?.id ?? null;
    if (ultima !== ultimoIdRef.current && listaRef.current) {
      listaRef.current.scrollTop = listaRef.current.scrollHeight;
    }
    ultimoIdRef.current = ultima;
  }, [mensagens]);

  async function enviar(e) {
    e?.preventDefault();
    const conteudo = texto.trim();
    if ((!conteudo && !anexo) || selecionadoId == null || enviando) return;
    setEnviando(true);
    versaoEnvioRef.current += 1;
    try {
      let data;
      if (anexo) {
        const dados = new FormData();
        dados.append('arquivo', anexo);
        if (conteudo) dados.append('texto', conteudo);
        ({ data } = await api.post(`/mensagens/conversa/${selecionadoId}/imagem`, dados));
      } else {
        ({ data } = await api.post(`/mensagens/conversa/${selecionadoId}`, { texto: conteudo }));
      }
      versaoEnvioRef.current += 1;
      setMensagens((ms) => [...ms, data]);
      setTexto('');
      setAnexo(null);
      setEmojisAbertos(false);
      setError('');
      carregarContatos();
    } catch (err) {
      setError(err.response?.data?.message ?? 'Não foi possível enviar a mensagem.');
    } finally {
      setEnviando(false);
      textareaRef.current?.focus();
    }
  }

  function aoTeclar(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      enviar();
    }
  }

  const contato = contatos.find((c) => c.id === selecionadoId) ?? null;
  const termo = busca.trim().toLowerCase();
  const contatosFiltrados = termo
    ? contatos.filter((c) => c.nome.toLowerCase().includes(termo) || setorDe(c).toLowerCase().includes(termo))
    : contatos;

  return (
    <div className="pagina-fixa">
      <header className="page-header">
        <h1>Mensagens</h1>
        <p>Converse com os outros setores: produção, estoque, financeiro e administração.</p>
      </header>

      {(error || erroConexao) && <div className="alert alert-error">{error || erroConexao}</div>}

      {loading ? (
        <section className="panel">
          <p className="empty-state">Carregando...</p>
        </section>
      ) : contatos.length === 0 ? (
        <section className="panel">
          <p className="empty-state">Ainda não há outras contas cadastradas para conversar.</p>
        </section>
      ) : (
        <div className={'chat' + (contato ? ' chat-aberto' : '')}>
          <aside className="chat-contatos">
            <div className="chat-contatos-topo">
              <input
                className="chat-busca"
                type="search"
                placeholder="Buscar setor ou pessoa"
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
              />
            </div>
            <div className="chat-contatos-lista">
              {contatosFiltrados.length === 0 && <p className="empty-state">Nenhuma conta encontrada.</p>}
              {contatosFiltrados.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  className={
                    'chat-contato' + (c.id === selecionadoId ? ' ativo' : '') + (c.naoLidas > 0 ? ' nova' : '')
                  }
                  onClick={() => setSelecionadoId(c.id)}
                >
                  <Avatar nome={c.nome} />
                  <span className="chat-contato-info">
                    <span className="chat-contato-linha">
                      <strong>{c.nome}</strong>
                      {c.ultimaMensagem && (
                        <small className="chat-contato-hora">{resumoData(c.ultimaMensagem.dataHora)}</small>
                      )}
                    </span>
                    <small className="chat-contato-setor">{setorDe(c)}</small>
                    <span className="chat-contato-linha">
                      <small className="chat-contato-previa">
                        {previaUltima(c.ultimaMensagem)}
                      </small>
                      {c.naoLidas > 0 && <span className="chat-badge">{c.naoLidas}</span>}
                    </span>
                  </span>
                </button>
              ))}
            </div>
          </aside>

          <section
            className={'chat-conversa' + (arrastando ? ' chat-arrastando' : '')}
            onDragOver={(e) => {
              if (contato && [...(e.dataTransfer?.types ?? [])].includes('Files')) {
                e.preventDefault();
                setArrastando(true);
              }
            }}
            onDragLeave={(e) => {
              if (!e.currentTarget.contains(e.relatedTarget)) setArrastando(false);
            }}
            onDrop={(e) => {
              if (contato) aoSoltarImagem(e);
            }}
          >
            {arrastando && <div className="chat-solte">Solte a imagem aqui para enviar</div>}
            {!contato ? (
              <div className="chat-vazio">
                <span className="chat-vazio-icone">
                  <Icone nome="conversa" />
                </span>
                <strong>Suas conversas</strong>
                <span>Escolha um setor ao lado para começar a conversar.</span>
              </div>
            ) : (
              <>
                <header className="chat-conversa-header">
                  <button type="button" className="btn-link chat-voltar" onClick={() => setSelecionadoId(null)}>
                    ← Voltar
                  </button>
                  <Avatar nome={contato.nome} pequeno />
                  <div>
                    <strong>{contato.nome}</strong>
                    <small>{setorDe(contato)}</small>
                  </div>
                </header>

                <div className="chat-mensagens" ref={listaRef}>
                  {mensagens.length === 0 && (
                    <p className="empty-state">Nenhuma mensagem ainda. Escreva a primeira abaixo.</p>
                  )}
                  {mensagens.map((m, i) => {
                    const minha = m.remetenteId !== contato.id;
                    const novoDia = i === 0 || rotuloDia(mensagens[i - 1].dataHora) !== rotuloDia(m.dataHora);
                    const mesmoAutor = !novoDia && mensagens[i - 1].remetenteId === m.remetenteId;
                    return (
                      <Fragment key={m.id}>
                        {novoDia && <div className="chat-dia">{rotuloDia(m.dataHora)}</div>}
                        <div className={'chat-linha ' + (minha ? 'minha' : 'dele') + (mesmoAutor ? ' mesmo-autor' : '')}>
                        <div
                          className={
                            'chat-balao ' +
                            (minha ? 'minha' : 'dele') +
                            (!m.temImagem && soEmojis(m.texto) ? ' so-emoji' : '')
                          }
                        >
                          {m.temImagem && (
                            <ImagemChat mensagemId={m.id} onAbrir={setImagemAmpliada} onCarregar={manterNoFim} />
                          )}
                          {m.texto && <p>{m.texto}</p>}
                          <small>
                            {hora(m.dataHora)}
                            {minha && (
                              <span className={'chat-tique' + (m.lida ? ' lida' : '')} title={m.lida ? 'Lida' : 'Enviada'}>
                                {m.lida ? '✓✓' : '✓'}
                              </span>
                            )}
                          </small>
                        </div>
                        </div>
                      </Fragment>
                    );
                  })}
                </div>

                <form className="chat-form" onSubmit={enviar}>
                  {anexo && (
                    <div className="chat-anexo">
                      {previaAnexo && <img src={previaAnexo} alt="Imagem a enviar" />}
                      <div>
                        <strong>{anexo.name || 'Imagem colada'}</strong>
                        <small>{(anexo.size / 1024).toFixed(0)} KB · será enviada com a mensagem</small>
                      </div>
                      <button type="button" className="btn-icon" aria-label="Remover imagem" onClick={() => setAnexo(null)}>
                        &times;
                      </button>
                    </div>
                  )}
                  <div className="chat-form-linha">
                    <div className="chat-ferramentas">
                      <button
                        type="button"
                        className={'chat-ferramenta' + (emojisAbertos ? ' ativa' : '')}
                        title="Emojis"
                        aria-label="Emojis"
                        data-emoji-toggle
                        onClick={() => setEmojisAbertos((v) => !v)}
                      >
                        <Icone nome="sorriso" />
                      </button>
                      <button
                        type="button"
                        className="chat-ferramenta"
                        title="Enviar imagem (também dá para arrastar ou colar)"
                        aria-label="Enviar imagem"
                        onClick={() => arquivoRef.current?.click()}
                      >
                        <Icone nome="clipe" />
                      </button>
                      <input
                        ref={arquivoRef}
                        type="file"
                        accept={TIPOS_IMAGEM.join(',')}
                        hidden
                        onChange={(e) => {
                          escolherImagem(e.target.files?.[0]);
                          e.target.value = '';
                        }}
                      />
                    </div>
                    <textarea
                      ref={textareaRef}
                      value={texto}
                      onChange={(e) => setTexto(e.target.value)}
                      onKeyDown={aoTeclar}
                      onPaste={aoColar}
                      placeholder={window.innerWidth < 480 ? 'Mensagem' : 'Escreva uma mensagem'}
                      rows={1}
                      maxLength={2000}
                    />
                    <button
                      type="submit"
                      className="chat-enviar"
                      title="Enviar"
                      aria-label="Enviar"
                      disabled={enviando || (!texto.trim() && !anexo)}
                    >
                      <Icone nome="enviar" />
                    </button>
                  </div>
                  <small className="chat-dica">Enter envia · Shift+Enter quebra a linha</small>
                  {emojisAbertos && <EmojiPicker onEscolher={inserirEmoji} onFechar={() => setEmojisAbertos(false)} />}
                </form>
              </>
            )}
          </section>
        </div>
      )}

      {imagemAmpliada && <VisualizadorImagem src={imagemAmpliada} onFechar={() => setImagemAmpliada(null)} />}
    </div>
  );
}
