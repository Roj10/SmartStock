import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Pastilhas from './pages/Pastilhas';
import Fornecedores from './pages/Fornecedores';
import Movimentacoes from './pages/Movimentacoes';

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
        <Route path="pastilhas" element={<Pastilhas />} />
        <Route path="fornecedores" element={<Fornecedores />} />
        <Route path="movimentacoes" element={<Movimentacoes />} />
      </Route>
    </Routes>
  );
}
