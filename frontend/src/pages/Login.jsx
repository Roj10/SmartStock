/*
 * Em palavras simples: Tela de login, em etapas, conversando com o backend:
 * 1) usuário e senha;
 * 2) código de 6 dígitos enviado para o e-mail da conta (verificação em duas etapas, ou 2FA).
 *    Na primeira vez, antes, a pessoa informa o e-mail; o código que chega nele confirma que o e-mail é dela;
 * 3) troca de senha, quando a conta usa senha de exemplo ou definida pelo administrador.
 * Cada etapa usa um "desafio" (passe temporário de 10 minutos); o acesso só é liberado no fim.
 */
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';

const ESPERA_REENVIO = 60; // segundos entre um envio de código e outro (o servidor também confere)

export default function Login() {
  const { concluirLogin } = useAuth();
  const navigate = useNavigate();
  const [etapa, setEtapa] = useState('CREDENCIAIS');
  const [username, setUsername] = useState('');
  const [senha, setSenha] = useState('');
  const [desafio, setDesafio] = useState('');
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [emailMascarado, setEmailMascarado] = useState('');
  const [modoTeste, setModoTeste] = useState(false);
  const [primeiraVez, setPrimeiraVez] = useState(false);
  const [codigo, setCodigo] = useState('');
  const [espera, setEspera] = useState(0);
  const [aviso, setAviso] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // contagem regressiva do botão "Enviar outro código"
  useEffect(() => {
    if (espera <= 0) return undefined;
    const t = setTimeout(() => setEspera((s) => s - 1), 1000);
    return () => clearTimeout(t);
  }, [espera]);

  function recomecar(mensagem) {
    setEtapa('CREDENCIAIS');
    setSenha('');
    setCodigo('');
    setAviso('');
    setError(mensagem ?? '');
  }

  function tratarErro(err) {
    const status = err.response?.status;
    const msg = err.response?.data?.message;
    if (!err.response) return setError('Não foi possível conectar ao servidor.');
    // etapa vencida (10 minutos): volta ao início
    if (status === 401 && etapa !== 'CREDENCIAIS') return recomecar(msg ?? 'A etapa de login expirou. Entre de novo.');
    setError(msg ?? 'Não foi possível concluir. Tente de novo.');
  }

  // Avança conforme a resposta do servidor
  function seguir(resp) {
    setError('');
    setCodigo('');
    setDesafio(resp.desafio ?? '');
    if (resp.nome) setNome(resp.nome);
    setModoTeste(!!resp.modoTeste);
    if (resp.etapa === 'CONCLUIDO') {
      concluirLogin(resp);
      navigate('/');
      return;
    }
    if (resp.etapa === 'CODIGO_EMAIL') {
      setEmailMascarado(resp.emailMascarado ?? '');
      setEspera(ESPERA_REENVIO);
    }
    setEtapa(resp.etapa);
  }

  async function enviar(e, fn) {
    e?.preventDefault();
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
      setAviso('');
      setPrimeiraVez(data.etapa === 'CADASTRAR_EMAIL');
      seguir(data);
    });

  const cadastrarEmail = (e) =>
    enviar(e, async () => {
      const { data } = await api.post('/auth/email/cadastrar', { desafio, email });
      setAviso('');
      seguir(data);
    });

  const verificar = (e) =>
    enviar(e, async () => {
      const { data } = await api.post('/auth/email/verificar', { desafio, codigo });
      seguir(data);
    });

  const reenviar = () =>
    enviar(null, async () => {
      const { data } = await api.post('/auth/email/reenviar', { desafio });
      seguir(data);
      setAviso('Enviamos um código novo. O anterior não vale mais.');
    });

  const trocarSenha = (e) =>
    enviar(e, async () => {
      if (novaSenha !== confirmarSenha) throw { response: { data: { message: 'As duas senhas não são iguais.' } } };
      const { data } = await api.post('/auth/trocar-senha', { desafio, novaSenha });
      setNovaSenha('');
      setConfirmarSenha('');
      seguir(data);
    });

  return (
    <div className="login-screen">
      <div className="login-card">
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

        {etapa === 'CADASTRAR_EMAIL' && (
          <form onSubmit={cadastrarEmail} className="form">
            <h2 className="login-titulo">Proteja sua conta</h2>
            <p className="login-texto">
              {nome ? `${nome}, a` : 'A'} partir de agora, além da senha, cada login pede um código de 6 dígitos enviado
              para o seu e-mail. Assim, mesmo que alguém descubra a sua senha, não consegue entrar.
            </p>
            <label>
              Seu e-mail
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                placeholder="voce@gmail.com"
                maxLength={254}
                required
                autoFocus
              />
            </label>
            {error && <div className="alert alert-error">{error}</div>}
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Enviando...' : 'Enviar código'}
            </button>
            <button type="button" className="btn-link" onClick={() => recomecar()}>
              Cancelar
            </button>
          </form>
        )}

        {etapa === 'CODIGO_EMAIL' && (
          <form onSubmit={verificar} className="form">
            <h2 className="login-titulo">Verificação em duas etapas</h2>
            <p className="login-texto">
              Enviamos um código de 6 dígitos para <strong>{emailMascarado}</strong>. Ele vale por 10 minutos. Se não
              aparecer, confira a caixa de spam.
            </p>
            {modoTeste && (
              <div className="alert alert-info">
                Modo de teste: o e-mail não está configurado no servidor, então o código aparece no terminal do backend.
              </div>
            )}
            <label>
              Código
              <input
                className="input-codigo"
                value={codigo}
                onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
                inputMode="numeric"
                autoComplete="one-time-code"
                placeholder="000000"
                required
                autoFocus
              />
            </label>
            {aviso && !error && <div className="alert alert-info">{aviso}</div>}
            {error && <div className="alert alert-error">{error}</div>}
            <button className="btn btn-primary" type="submit" disabled={loading || codigo.length !== 6}>
              {loading ? 'Verificando...' : 'Verificar'}
            </button>
            <button type="button" className="btn-link" onClick={reenviar} disabled={loading || espera > 0}>
              {espera > 0 ? `Enviar outro código (em ${espera}s)` : 'Enviar outro código'}
            </button>
            {primeiraVez && (
              <button
                type="button"
                className="btn-link"
                onClick={() => {
                  setError('');
                  setAviso('');
                  setEtapa('CADASTRAR_EMAIL');
                }}
              >
                Digitei o e-mail errado
              </button>
            )}
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
