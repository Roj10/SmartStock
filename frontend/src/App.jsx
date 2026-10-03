import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Pastilhas from './pages/Pastilhas';
import Fornecedores from './pages/Fornecedores';
import Movimentacoes from './pages/Movimentacoes';
import Usuarios from './pages/Usuarios';
import Progresso from './pages/Progresso';
import Calendario from './pages/Calendario';
import Financeiro from './pages/Financeiro';
import Mensagens from './pages/Mensagens';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route
          path="pastilhas"
          element={
            <ProtectedRoute permissao="PASTILHAS">
              <Pastilhas />
            </ProtectedRoute>
          }
        />
        <Route
          path="fornecedores"
          element={
            <ProtectedRoute permissao="FORNECEDORES">
              <Fornecedores />
            </ProtectedRoute>
          }
        />
        <Route
          path="movimentacoes"
          element={
            <ProtectedRoute permissao="MOVIMENTACOES">
              <Movimentacoes />
            </ProtectedRoute>
          }
        />
        <Route
          path="progresso"
          element={
            <ProtectedRoute permissao="PROJETOS">
              <Progresso />
            </ProtectedRoute>
          }
        />
        <Route
          path="calendario"
          element={
            <ProtectedRoute permissao="PROJETOS">
              <Calendario />
            </ProtectedRoute>
          }
        />
        <Route
          path="financeiro"
          element={
            <ProtectedRoute permissao="FINANCEIRO">
              <Financeiro />
            </ProtectedRoute>
          }
        />
        <Route path="mensagens" element={<Mensagens />} />
        <Route
          path="usuarios"
          element={
            <ProtectedRoute permissao="USUARIOS">
              <Usuarios />
            </ProtectedRoute>
          }
        />
      </Route>
    </Routes>
  );
}
