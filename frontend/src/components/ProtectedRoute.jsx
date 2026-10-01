import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, permissao }) {
  const { isAuthenticated, hasPermissao } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (permissao && !hasPermissao(permissao)) {
    return (
      <div className="access-denied">
        <h1>Acesso restrito</h1>
        <p>Você não tem permissão para acessar esta página. Fale com um administrador se precisar de acesso.</p>
      </div>
    );
  }

  return children;
}
