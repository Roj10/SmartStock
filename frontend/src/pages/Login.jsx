import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('admin');
  const [senha, setSenha] = useState('admin123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(username, senha);
      navigate('/');
    } catch (err) {
      setError(
        err.response?.status === 401 || err.response?.status === 403
          ? 'Usuário ou senha inválidos.'
          : 'Não foi possível conectar ao servidor.'
      );
    } finally {
      setLoading(false);
    }
  }

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

        <form onSubmit={handleSubmit} className="form">
          <label>
            Usuário
            <input value={username} onChange={(e) => setUsername(e.target.value)} required autoFocus />
          </label>
          <label>
            Senha
            <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
          </label>

          {error && <div className="alert alert-error">{error}</div>}

          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        <p className="login-hint">
          Usuários de demonstração: <code>admin / admin123</code> ou <code>operador / operador123</code>
        </p>
      </div>
    </div>
  );
}
