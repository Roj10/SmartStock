/*
 * Em palavras simples: "Memória" do login no navegador, compartilhada por todas as telas.
 * Guarda quem está logado, o perfil e as abas permitidas, e oferece: concluirLogin (ao fim das etapas
 * do login), logout (sair) e hasPermissao (para mostrar ou esconder abas e botões).
 * Os dados ficam no sessionStorage: valem só para esta aba do navegador e somem ao fechá-la.
 */
import { createContext, useContext, useState, useCallback } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = sessionStorage.getItem('smartstock_user');
    return stored ? JSON.parse(stored) : null;
  });

  // O login tem etapas (senha → 2FA → troca de senha, se exigida). Quando o servidor responde CONCLUIDO,
  // a resposta traz o token de acesso: só então a sessão é guardada.
  const concluirLogin = useCallback((data) => {
    sessionStorage.setItem('smartstock_token', data.token);
    const userData = {
      username: data.username,
      nome: data.nome,
      role: data.role,
      permissoes: data.permissoes ?? [],
    };
    sessionStorage.setItem('smartstock_user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  }, []);

  const logout = useCallback(async () => {
    // encerra a sessão também no servidor (o token deixa de valer, mesmo se alguém o tiver copiado)
    try {
      if (sessionStorage.getItem('smartstock_token')) await api.post('/auth/sair');
    } catch {
      // sem conexão: a sessão local é apagada mesmo assim
    }
    sessionStorage.removeItem('smartstock_token');
    sessionStorage.removeItem('smartstock_user');
    setUser(null);
  }, []);

  const isAdmin = user?.role === 'ADMIN';

  const hasPermissao = useCallback(
    (modulo) => isAdmin || !!user?.permissoes?.includes(modulo),
    [user, isAdmin]
  );

  return (
    <AuthContext.Provider value={{ user, concluirLogin, logout, isAuthenticated: !!user, isAdmin, hasPermissao }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return ctx;
}
