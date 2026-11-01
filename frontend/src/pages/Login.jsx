import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';

// Login em etapas: senha → 2FA (configurar o app na primeira vez, ou digitar o código) → troca de senha (se exigida).
export default function Login() {
  const { concluirLogin } = useAuth();
  const navigate = useNavigate();
  const [etapa, setEtapa] = useState('CREDENCIAIS');
  const [username, setUsername] = useState('');
  const [senha, setSenha] = useState('');
  const [desafio, setDesafio] = useState('');
  const [nome, setNome] = useState('');
  const [config, setConfig] = useState(null);
  const [codigo, setCodigo] = useState('');
  const [usarRecuperacao, setUsarRecuperacao] = useState(false);
  const [codigosRecuperacao, setCodigosRecuperacao] = useState([]);
  const [respostaPendente, setRespostaPendente] = useState(null);
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function recomecar(mensagem) {
    setEtapa('CREDENCIAIS');
    setSenha('');
    setCodigo('');
    setConfig(null);
    setError(mensagem ?? '');
  }

  function tratarErro(err) {
    const status = err.response?.status;
    const msg = err.response?.data?.message;
    if (!err.response) return setError('Não foi possível conectar ao servidor.');
    // etapa vencida (5 minutos) ou senha errada: volta ao início
    if (status === 401 && etapa !== 'CREDENCIAIS') return recomecar(msg ?? 'A etapa de login expirou. Entre de novo.');
    setError(msg ?? 'Não foi possível concluir. Tente de novo.');
  }

  // Avança conforme a resposta do servidor
  function seguir(resp) {
    setError('');
    setCodigo('');
    setDesafio(resp.desafio ?? '');
    if (resp.nome) setNome(resp.nome);
    if (resp.codigosRecuperacao?.length) {
      // 2FA recém-ativado: mostra os códigos de recuperação antes de seguir
      setCodigosRecuperacao(resp.codigosRecuperacao);
      setRespostaPendente({ ...resp, codigosRecuperacao: null });
      setEtapa('RECUPERACAO');
      return;
    }
    if (resp.etapa === 'CONCLUIDO') {
      concluirLogin(resp);
      navigate('/');
      return;
    }
    setEtapa(resp.etapa);
  }

  // Na etapa de configuração, busca o QR code assim que ela aparece
  useEffect(() => {
    if (etapa !== 'CONFIGURAR_2FA' || config || !desafio) return;
    api
      .post('/auth/2fa/configurar', { desafio })
      .then(({ data }) => setConfig(data))
      .catch(tratarErro);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [etapa, desafio]);

  async function enviar(e, fn) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await fn();
    } catch (err) {
      tratarErro(err);
    } finally {
      setLoading(false);
    }
  }

  const entrar = (e) =>
    enviar(e, async () => {
      const { data } = await api.post('/auth/login', { username, senha });
      setSenha('');
      setConfig(null);
      setUsarRecuperacao(false);
      seguir(data);
    });

  const ativar = (e) =>
    enviar(e, async () => {
      const { data } = await api.post('/auth/2fa/ativar', { desafio, codigo });
      seguir(data);
    });

  const verificar = (e) =>
    enviar(e, async () => {
      const { data } = await api.post('/auth/2fa/verificar', { desafio, codigo });
      seguir(data);
    });

  const trocarSenha = (e) =>
    enviar(e, async () => {
      if (novaSenha !== confirmarSenha) throw { response: { data: { message: 'As duas senhas não são iguais.' } } };
      const { data } = await api.post('/auth/trocar-senha', { desafio, novaSenha });
      setNovaSenha('');
      setConfirmarSenha('');
      seguir(data);
    });

  function baixarCodigos() {
    const texto =
      `SmartStock - códigos de recuperação do 2FA (${username})\n` +
      'Cada código funciona uma única vez, se você não tiver o celular.\n\n' +
      codigosRecuperacao.join('\n') +
      '\n';
    const url = URL.createObjectURL(new Blob([texto], { type: 'text/plain' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `smartstock-recuperacao-${username}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  const campoCodigo = (
    <label>
      {usarRecuperacao ? 'Código de recuperação' : 'Código de 6 dígitos do app'}
      <input
        className="input-codigo"
        value={codigo}
        onChange={(e) => setCodigo(usarRecuperacao ? e.target.value : e.target.value.replace(/\D/g, '').slice(0, 6))}
        inputMode={usarRecuperacao ? 'text' : 'numeric'}
        autoComplete="one-time-code"
        placeholder={usarRecuperacao ? 'xxxx-xxxx' : '000000'}
        required
        autoFocus
      />
    </label>
  );

  return (
    <div className="login-screen">
      <div className={'login-card' + (etapa === 'CONFIGURAR_2FA' || etapa === 'RECUPERACAO' ? ' login-card-largo' : '')}>
        <div className="brand brand-center">
          <span className="brand-mark">SS</span>
          <div>
            <strong>SmartStock</strong>
            <small>Controle de Estoque de Pastilhas Industriais</small>
          </div>
        </div>

        {etapa === 'CREDENCIAIS' && (
          <form onSubmit={entrar} className="form">
            <label>
              Usuário
              <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required autoFocus />
            </label>
            <label>
              Senha
              <input
                type="password"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                autoComplete="current-password"
                required
              />
            </label>
            {error && <div className="alert alert-error">{error}</div>}
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Entrando...' : 'Entrar'}
            </button>
          </form>
        )}

        {etapa === 'CONFIGURAR_2FA' && (
          <form onSubmit={ativar} className="form">
            <h2 className="login-titulo">Proteja sua conta com o 2FA</h2>
            <p className="login-texto">
              {nome ? `${nome}, a` : 'A'} partir de agora o login pede, além da senha, um código do seu celular.
            </p>
            <ol className="login-passos">
              <li>
                Instale um app autenticador: <strong>Google Authenticator</strong>, <strong>Microsoft Authenticator</strong>{' '}
                ou <strong>Authy</strong>.
              </li>
              <li>No app, toque em “+” e escaneie o QR code abaixo.</li>
              <li>Digite o código de 6 dígitos que aparecer no app.</li>
            </ol>
            <div className="qr-2fa">
              {config ? <img src={config.qrCode} alt="QR code para configurar o app autenticador" /> : <span>Gerando QR code...</span>}
            </div>
            {config && (
              <details className="chave-2fa">
                <summary>Não consegue escanear? Digite a chave no app</summary>
                <p>
                  Conta: <strong>{config.conta}</strong> · Chave: <code>{config.chave}</code> · Tipo: baseado em tempo
                </p>
              </details>
            )}
            {campoCodigo}
            {error && <div className="alert alert-error">{error}</div>}
            <button className="btn btn-primary" type="submit" disabled={loading || !config}>
              {loading ? 'Confirmando...' : 'Ativar e continuar'}
            </button>
            <button type="button" className="btn-link" onClick={() => recomecar()}>
              Cancelar
            </button>
          </form>
        )}

        {etapa === 'RECUPERACAO' && (
          <div className="form">
            <h2 className="login-titulo">Guarde seus códigos de recuperação</h2>
            <p className="login-texto">
              Se você perder ou trocar de celular, use um destes códigos no lugar do código do app. Cada um funciona{' '}
              <strong>uma única vez</strong>. Eles não serão mostrados de novo.
            </p>
            <ul className="codigos-recuperacao">
              {codigosRecuperacao.map((c) => (
                <li key={c}>
                  <code>{c}</code>
                </li>
              ))}
            </ul>
            <div className="form-actions form-actions-esquerda">
              <button type="button" className="btn btn-ghost" onClick={baixarCodigos}>
                Baixar .txt
              </button>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => navigator.clipboard?.writeText(codigosRecuperacao.join('\n'))}
              >
                Copiar
              </button>
            </div>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => {
                setCodigosRecuperacao([]);
                seguir(respostaPendente);
              }}
            >
              Já guardei, continuar
            </button>
          </div>
        )}

        {etapa === 'CODIGO_2FA' && (
          <form onSubmit={verificar} className="form">
            <h2 className="login-titulo">Verificação em duas etapas</h2>
            <p className="login-texto">
              {usarRecuperacao
                ? 'Digite um dos códigos de recuperação que você guardou ao ativar o 2FA.'
                : 'Abra o app autenticador no celular e digite o código do SmartStock.'}
            </p>
            {campoCodigo}
            {error && <div className="alert alert-error">{error}</div>}
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Verificando...' : 'Verificar'}
            </button>
            <button
              type="button"
              className="btn-link"
              onClick={() => {
                setUsarRecuperacao((v) => !v);
                setCodigo('');
                setError('');
              }}
            >
              {usarRecuperacao ? 'Usar o código do app' : 'Estou sem o celular: usar código de recuperação'}
            </button>
            <button type="button" className="btn-link" onClick={() => recomecar()}>
              Voltar
            </button>
          </form>
        )}

        {etapa === 'TROCAR_SENHA' && (
          <form onSubmit={trocarSenha} className="form">
            <h2 className="login-titulo">Crie uma senha nova</h2>
            <p className="login-texto">
              A senha atual foi definida por outra pessoa ou é uma senha de exemplo. Escolha uma só sua: pelo menos 8
              caracteres, com letras e números.
            </p>
            <label>
              Nova senha
              <input
                type="password"
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                autoComplete="new-password"
                minLength={8}
                required
                autoFocus
              />
            </label>
            <label>
              Repita a nova senha
              <input
                type="password"
                value={confirmarSenha}
                onChange={(e) => setConfirmarSenha(e.target.value)}
                autoComplete="new-password"
                minLength={8}
                required
              />
            </label>
            {error && <div className="alert alert-error">{error}</div>}
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Salvando...' : 'Salvar senha e entrar'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
